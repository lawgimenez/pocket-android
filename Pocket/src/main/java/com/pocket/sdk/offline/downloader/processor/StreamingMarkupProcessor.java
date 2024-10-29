package com.pocket.sdk.offline.downloader.processor;

import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.downloader.Cancel;
import com.pocket.util.java.PktFileUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import okio.Source;

/**
 * Performs the reading, processing and saving of an {@link Asset} for {@link com.pocket.sdk.offline.downloader.WebDownloader}.
 * <p>
 * Reads through markup (.html or .css) and finds additional assets to download.
 * It also modifies the markup, changing each asset's path to a relative path that points to its eventual location locally on disk.
 * Finally it saves this modified html/stylesheet file to disk.
 * <p>
 * Previous implementations:
 * v1: https://github.com/Pocket/Android/tree/7.12.0.0/Pocket/src/main/java/com/pocket/sdk/offline/processor
 * v2: https://github.com/Pocket/Android/blob/sync-migration/Pocket/src/main/java/com/pocket/sdk/offline/downloader/MarkupProcessor.java
 * required having the entire file as a string in memory and then using regex to find assets.
 * For each page this could be several kb or mb depending on the size of the page. That can be a LOT of memory for mobile devices.
 * <p>
 * This implementation streams/buffers the data and attempts to use the least amount of memory as possible in the process.
 * It reads from a buffered source, processes it byte by byte and writes it to a buffered output on disk.
 * For most cases it will only use some default buffer size such as 8k, but if it encounters extremely long urls or weird
 * edge cases it could potentially use more memory temporarily.
 * <p>
 * The previous implementations relied on regex to find assets. Since we don't have access to the entire page when buffering,
 * we can't rely on that approach.  Also ideally we'd be able to use a proper html parsing library that can handle all of the
 * wild html found out there on the web and would let us just search for certain attributes, tags, etc. Unfortunately, at the time
 * of writing this class, there weren't any libraries that handled parsing html in a streaming way except some that were either way too
 * big of a library to use on mobile, or too old or undocumented to rely on.
 * <p>
 * So the approach here was to carry over the exact logic found in the regex of the previous implementations but hand write
 * matcher classes that could do this in a streaming way.
 * <p>
 * See {@link #process(Reader, Matcher...)} for the implementation design.
 */
public class StreamingMarkupProcessor {
	
	private static final boolean DEBUG_CHAR_BY_CHAR = false;
	private static final boolean DEBUG_CAPTURES = false;
	
	/** A buffer that holds characters that might be part of a capture */
	private final Buffer buffer = new Buffer();
	/** Matchers that are in a capture mode, the location in the buffer of the captures first character */
	private final Map<Matcher, Long> starts = new HashMap<>();
	/** Matchers that are in a post capture mode, the location in the buffer of the last character */
	private final Map<Matcher, Long> ends = new HashMap<>();
	/** Matchers currently in capture mode. */
	private final Set<Matcher> capturing = new HashSet<>();
	/** A temporary file used for charset detection */
	private final File tmp1;
	/** A temporary file used when saving the final file */
	private final File tmp2;
	/** The real location the file should end up at when successful */
	private final File output;
	/** The maximum file size allowed, if greater than this it will give up processing and fail */
	private final long maxSize;
	/** Handles capturing/modifying asset paths when found */
	private final LiteralHandler handler;
	/** If debugging captures, a buffer that holds the entire content up to the current location. This obviously defeats the purpose of this whole class, it is only for help debugging matchers. */
	private final Buffer debugCapture = DEBUG_CAPTURES ? new Buffer() : null;
	
	/** Protects against reuse of this instance. */
	private boolean used;
	/** See {@link #cleanup} */
	private PendingCleanup cleanup;
	
	public StreamingMarkupProcessor(String sourceUrl, Cancel cancel, File processingDirectory, File output, long maxSize, LiteralHandler handler) {
		this.output = output;
		this.tmp1 = new File(processingDirectory, ByteString.encodeUtf8(sourceUrl).sha256().hex()+"_1.tmp"); // TODO avoid multiples attempting to download this file at once, could hold a file lock, or add a random string into this file name as well?
		this.tmp2 = new File(processingDirectory, ByteString.encodeUtf8(sourceUrl).sha256().hex()+"_2.tmp"); // TODO avoid multiples attempting to download this file at once
		this.maxSize = maxSize;
		this.handler = handler;
	}
	
	/**
	 * Takes a data source that is expected to be html markup, processes it and gets it written to {@link #output}.
	 * @param encoding If known, the encoding/charset. If null it will attempt to detect it.
	 * @param src The data stream. It is up to the caller to handle closing this stream.
	 * @return {@link HtmlSuccess} if the page was successfully processed and saved or {@link Result#FAILURE} if it was larger than {@link #maxSize} and was not downloaded
	 * @throws IOException If something goes wrong during the process
	 */
	public Result processHtml(String encoding, Source src) throws IOException {
		// Verify the provided encoding is valid
		if (encoding != null) {
			try {
				Charset.forName(encoding);
			} catch (Throwable t) {
				encoding = null;
			}
		}
		if (encoding == null) {
			// We have to determine the charset first
			// So we'll buffer it to a temporary file and detect the charset while we do that
			BufferedSource source = src instanceof BufferedSource ? (BufferedSource) src : Okio.buffer(src);
			BufferedSink sink = null;
			long total = 0;
			try {
				PktFileUtils.createFile(tmp1, false);
				sink = Okio.buffer(Okio.sink(tmp1));
				UniversalDetector charsetDetector = new UniversalDetector(null);
				byte[] detectorBuffer = new byte[1024 * 4];
				long read;
				while ((read = source.read(detectorBuffer)) != -1) {
					total += read;
					if (total > maxSize) break;
					charsetDetector.handleData(detectorBuffer, 0, (int) read);
					sink.write(detectorBuffer);
					if (charsetDetector.isDone()) {
						encoding = charsetDetector.getDetectedCharset();
						// Can just finish buffering directly
						// This code mimics the internals of BufferedSink.writeAll() but allows us to cancel if too large
						Buffer buffer = sink.buffer();
						do {
							read = source.read(buffer, 8192L);
							sink.emitCompleteSegments();
							total += read;
						} while (read != -1 && total < maxSize);
						break;
					}
				}
				sink.flush();
			} finally {
				IOUtils.closeQuietly(sink);
			}
			if (total > maxSize) {
				FileUtils.deleteQuietly(tmp1);
				return null;
			}
			src = Okio.source(tmp1);
			
			// Ensure a valid encoding, fall back to utf-8 if needed.
			if (encoding != null) {
				try {
					Charset.forName(encoding);
				} catch (Throwable t) {
					encoding = null;
				}
			}
			if (encoding == null) encoding = "UTF-8";
		}
		
		// Now that we know the encoding, we can process the file
		Closeable reader = null;
		try {
			Matcher[] matchers = new Matcher[]{
					new MatchesImagesInHtml(),
					new MatchesCssImportStatements(),
					new MatchesImagesInCss(),
					new MatchesStylesheetLinks(),
					new MatchesCleanup()
			};
			final Result result;
			if (StringUtils.equalsIgnoreCase(encoding, "UTF-8")) {
				// If utf-8 we can use Okio
				BufferedSource in = src instanceof BufferedSource ? (BufferedSource) src : Okio.buffer(src);
				reader = in;
				result = process(okioReader(in), matchers);
			} else {
				// Otherwise we'll use a Reader
				BufferedReader in = new BufferedReader(new FileReader(tmp1));
				reader = in;
				result = process(in::read, matchers);
			}
			if (result instanceof Success) {
				return new HtmlSuccess(((Success) result).size, encoding);
			} else {
				return Result.FAILURE;
			}
		} finally {
			IOUtils.closeQuietly(reader);
			FileUtils.deleteQuietly(tmp1);
		}
	}
	
	public Result processStylesheet(Source data) throws IOException {
		// For stylesheets, we assume utf-8 and can buffer directly
		return process(okioReader(Okio.buffer(data)), new MatchesCssImportStatements(), new MatchesImagesInCss());
	}
	
	private static Reader okioReader(BufferedSource src) {
		return () -> {
			try {
				int next = src.readUtf8CodePoint();
				return next != 0 ? next : -1;
			} catch (EOFException e) {
				return -1;
			}
		};
	}
	
	interface Reader {
		int nextCodePoint() throws IOException;
	}
	
	/**
	 * Processes the stream, finding assets, updating their paths, and writes the modified text to the expected file location.
	 *
	 * <h2>Implementation design</h2>
	 * Here we are dealing with 4 major players:
	 * <ol>
	 *     <li>stream. The byte stream of the content coming from the network.</li>
	 *     <li>sink. The local file we are writing the content to.</li>
	 *     <li>matchers. The pattern matching that will find asset paths.</li>
	 *     <li>buffer. A temporary holding place of bytes when a matcher thinks it might have found a path.</li>
	 * </ol>
	 * The input stream is wrapped in a buffered stream. This allows us to read it one character at a time.
	 * Internally it will read a chunk of data from the network and hold it in a buffer. When we have finished
	 * reading that buffer it will load the next chunk.
	 * <p>
	 * Then we pass that next character to each matcher. The matcher is a state machine looks for a certain pattern
	 * to indicate if the character is part of an asset path that should be captured. It returns a mode based on each character.
	 * <p>
	 * If no matchers are in capturing mode, we write the character to the sink. The sink is also buffered so internally
	 * each character is added to a buffer and when that buffer is full, it pushes those bytes to disk.
	 * <p>
	 * If any matchers are in capturing mode, we instead write the character to our own buffer. Since we'll need to know
	 * the full path and possibly modify it, we can't write it to the sink yet.
	 * <p>
	 * If a matcher reports it has completed a capture, we take the buffer, convert it to a string and pass it to the handler
	 * to capture the asset and modify the string.  We then take that modified string and write it to the sink instead of the original string.
	 * The buffer is cleared and we continue onward to the next character.
	 * <p>
	 * If the matchers decide the capture ended up being incomplete we flush the buffer to the sink as is.
	 * <p>
	 * In this way, the total memory this implementation uses is the size of the 3 buffers (input, processing and sink).
	 * The processing buffer could in theory end up expanding if we hit a huge capture.
	 *
	 * @param stream The data stream. It is up to the caller to close this.
	 * @param matchers The patterns to look for.
	 * @return {@link Success} if processed, {@link Result#FAILURE} if the file was larger than {@link #maxSize}
	 * @throws IOException If anything goes wrong during this process
	 */
	private Result process(Reader stream, Matcher... matchers) throws IOException {
		if (used) throw new RuntimeException("processor may only be used once");
		used = true;
		
		BufferedSink sink = null;
		long size = 0;
		char c;
		try {
			// Buffer to a tmp file, we'll move it to the correct location if we successfully finish. This avoids a half written file in the assets folder if something goes wrong.
			PktFileUtils.createFile(tmp2, false);
			sink = Okio.buffer(Okio.sink(tmp2));
			
			// Read the stream one character at a time
			int next;
			while ((next = stream.nextCodePoint()) != -1) {
				// NOTE this is a very hot path since we are invoking this for every single character
				
				if (DEBUG_CHAR_BY_CHAR) System.out.println(Character.toChars(next)[0] + " " + next);
				
				if ((size += length(next)) > maxSize) break;
				
				boolean written = false; // Tracks if we wrote this char out yet
				for (int i = 0, len = matchers.length; i < len; i++) {
					Matcher matcher = matchers[i];
					Mode mode = matcher.read(next);
					if (DEBUG_CHAR_BY_CHAR) System.out.println("    " + matcher + " " + mode);
					switch (mode) {
						case NO_MATCH:
						case MATCHING:
							capturing.remove(matcher);
							break;
						case CAPTURING:
							if (!starts.containsKey(matcher)) starts.put(matcher, buffer.size());
							ends.remove(matcher);
							capturing.add(matcher);
							break;
						case POST_CAPTURE_MATCHING:
							ends.put(matcher, buffer.size());
							break;
						case MATCHED:
							long start = starts.get(matcher);
							long end = ends.containsKey(matcher) ? ends.get(matcher) : buffer.size();
							
							if (matcher instanceof MatchesCleanup) {
								// Special case. We could make an interface if needed, but this is the one use case.
								// This matcher finds absolute urls that weren't claimed by any other matcher
								// So mark this capture as pending, and if no one else claims it, we'll capture it.
								cleanup = new PendingCleanup(start, end);
								
							} else {
								// Write the pending char into the buffer so it gets written out during the capture call
								buffer.writeUtf8CodePoint(next);
								written = true;
								size += capture(start, end, matcher.type(), sink, matchers, handler);
							}
							break;
					}
				}
				
				// If nothing is capturing, we can process any pending clean
				if (cleanup != null && capturing.isEmpty()) {
					size += capture(cleanup.start, cleanup.end, 0, sink, matchers, StreamingMarkupProcessor::cleanup);
				}
				
				if (!written) {
					if (!capturing.isEmpty() || cleanup != null) {
						buffer.writeUtf8CodePoint(next);
						// TODO put a max length on capturing to avoid expanding the buffer for broken markup?
						
					} else {
						if (buffer.size() > 0) sink.writeAll(buffer);
						sink.writeUtf8CodePoint(next);
					}
					if (DEBUG_CAPTURES) debugCapture.writeUtf8CodePoint(next);
				}
			}
			
			sink.writeAll(buffer);
			sink.flush();
		} finally {
			IOUtils.closeQuietly(sink);
			IOUtils.closeQuietly(buffer);
			IOUtils.closeQuietly(debugCapture);
		}
		if (size <= maxSize) {
			FileUtils.deleteQuietly(output);
			FileUtils.moveFile(tmp2, output);
			return new Success(size);
		} else {
			FileUtils.deleteQuietly(tmp2);
			return Result.FAILURE;
		}
	}
	
	/**
	 * Capture a path from the buffer and dump the buffer and the capture's replacement to the sink
	 *
	 * @param start The index in the buffer of the first character
	 * @param end The index in the buffer of the last character
	 * @param type The {@link Asset#type}
	 * @param sink Where to write
	 * @param matchers Matchers to reset
	 * @param handler The capture handler
	 * @return The change in file byte size caused by replacing the capture text
	 * @throws IOException
	 */
	private long capture(long start, long end, int type, BufferedSink sink, Matcher[] matchers, LiteralHandler handler) throws IOException {
		long diff = 0;
		cleanup = null; // Cancels any clean up
		
		// Capture and replace
		if (start != end) {
			// Move all of the characters before the capture to the sink
			if (start > 0) sink.write(buffer, start);
			// Extract the capture characters
			String literal = buffer.readUtf8(end-start);
			String replacement = handler.capture(literal, type);
			diff = (replacement.length()-literal.length());
			// Write the capture to the sink
			sink.writeUtf8(replacement);
		}
		// Write any characters that were after the capture
		sink.writeAll(buffer);
		
		// Reset the state machine
		for (Matcher m : matchers) {  // reset all matchers to avoid overlap
			m.reset();
		}
		capturing.clear();
		ends.clear();
		starts.clear();
		return diff;
	}
	
	/** @return the number of bytes this character is (in utf-8) */
	private static int length(int codePoint) {
		// This code is based on Buffer.writeUtf8CodePoint()
		if (codePoint < 0x80) return 1;
		if (codePoint < 0x800) return 2;
		if (codePoint < 0x10000) {
			if (codePoint >= 0xd800 && codePoint <= 0xdfff) return 1;
			return 3;
		}
		if (codePoint <= 0x10ffff) return 4;
		throw new IllegalArgumentException("Unexpected code point: " + Integer.toHexString(codePoint));
	}
	
	interface LiteralHandler {
		/**
		 * @param literal A path that was found, could be a absolute url or a relative path.
		 * @param type The expected {@link Asset} type of the url
		 * @return A replacement path or the same path to leave unaltered.
		 */
		String capture(String literal, int type);
	}
	
	enum Mode {
		/** This character didn't match. If it was previously capturing it ended up deciding it wasn't valid */
		NO_MATCH,
		/** This character matched or continued the pattern BEFORE a capture. Not a capture yet though. */
		MATCHING,
		/** This character started or continued a potential capture. The first time this is returned is the first character, the last time this is returned is the last character. */
		CAPTURING,
		/** This character matched or continued the pattern AFTER a capture. This character is not part of the capture path, but part of the matcher still verifying whether the capture is valid.  */
		POST_CAPTURE_MATCHING,
		/** This character completed the verification of the capture. This character is not part of the capture path. This indicates the path is valid and can be officially captured. */
		MATCHED
	}
	
	interface Matcher {
		/**
		 * @param codepoint The next character's code point
		 * @return What mode your matcher is in based on this codepoint
		 */
		Mode read(int codepoint);
		/** The type of asset this matcher finds. One of the {@link Asset} types */
		int type();
		/** Reset back to the default state and start the beginning of your pattern next time. */
		void reset();
	}
	
	public abstract static class Result {
		private static final Result FAILURE = new Result() {};
	}
	static class Success extends Result {
		final long size;
		
		private Success(long size) { this.size = size; }
	}
	public static class HtmlSuccess extends Result {
		public final long size;
		public final String encoding;
		
		private HtmlSuccess(long size, String encoding) {
			this.size = size;
			this.encoding = encoding;
		}
	}
	
	private static class PendingCleanup {
		final long start;
		final long end;
		private PendingCleanup(long start, long end) {
			this.start = start;
			this.end = end;
		}
	}
	private static final Pattern CLEAN_UP_YOUTUBE = Pattern.compile("(youtube)\\.", Pattern.CASE_INSENSITIVE);
	private static final Pattern CLEAN_UP_VIMEO = Pattern.compile("^http://player\\.vimeo\\.com.*$", Pattern.CASE_INSENSITIVE);
	/**
	 * This path matched {@link MatchesCleanup} and no other patterns.
	 * This is used to clean up and remove any absolute urls that the other patterns didn't match.
	 * In offline web files we want to avoid making network calls for non-local assets.
	 * @return The replacement text
	 */
	private static String cleanup(String url, int type) { // NOTE: type isn't used, but it allows a method reference, which avoids allocation each invocation of this method.
		if (CLEAN_UP_VIMEO.matcher(url).find()) {
			return "file:///android_asset/video.html#" + url;
		} else if (CLEAN_UP_YOUTUBE.matcher(url).find()) {
			return url;
		} else {
			// Remove
			return "";
		}
	}
	
}
