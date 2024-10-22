package com.pocket.sdk.offline.cache;

import com.pocket.util.java.Logs;
import com.pocket.util.java.StringUtils2;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * An Asset represents a file on the internet and its location locally when downloaded and stored offline.
 * <p>
 * Assets have a url (online) and a local path (offline). Some assets have a base url when they are part
 * of html or css markup. For example, an image on web page will have its url (the link to the image),
 * and a base url (the page it was on).
 * <p>
 * An asset will be one of the following types:
 * <ul>
 * <li>{@link #MARKUP} A web page. HTML content.</li>
 * <li>{@link #IMAGE} An image.</li>
 * <li>{@link #STYLESHEET} A CSS document.</li>
 * </ul>
 * {@link Assets} and features that use that component use this class to determine where on disk to store
 */
public class Asset {
	
	/** Note: We don't actually keep markup files as assets... TODO clean this reference up. */
	public static final int MARKUP = 0;
	public static final int IMAGE = 1;
	public static final int STYLESHEET = 2;
	
	private static final boolean DEBUG = false;
	private static final int MAX_SUBDIRECTORY_DEPTH = 50;
	private static final int MAX_DIRECTORY_LENGTH = 100;
	private static final Pattern PATTERN_CLEAN_PATH = Pattern.compile("[\\#\\=\\?\\&\\%\\;\\:\\*\\\"\\<\\>\\|]");
	
	/** The url of this asset on the web. */
	public final URL url;
	/** The location on disk where this asset would be if saved to disk. */
	public final File local;
	/** The type of file, one of {@link #IMAGE}, {@link #MARKUP}, {@link #STYLESHEET}. */
	public final int type;
	/**
	 * A filename to use for image's resized filenames. This is only provided (instead of just calculating it from {@link #local} because of a bug in the old Asset class.
	 * It would determine a filename to use for images before truncating each path part to the maximum length.
	 * So this filename may not actually match {@link #local}. But since this is how it worked before, this kept for backwards compatibility with assets that might already be on disk.
	 * <p>
	 * As a more concrete example: Take this image url:
	 * https://pocket-image-cache.com/direct?resize=w2000&url=https%3A%2F%2Fcdn.theatlantic.com%2Fassets%2Fmedia%2Fimg%2Fmt%2F2018%2F06%2FGettyImages_521677454%2Flead_720_405.jpg%3Fmod%3D1533691457
	 * <p>
	 * Which gets cleaned up to this path:
	 * /pocket-image-cache.com/directresizew2000urlhttps3A2F2Fcdn.theatlantic.com2Fassets2Fmedia2Fimg2Fmt2F20182F062FGettyImages_5216774542Flead_720_405.jpg3Fmod3D1533691457
	 * <p>
	 * And after truncating the path lengths the local file becomes:
	 * /pocket-image-cache.com/directresizew2000urlhttps3A2F2Fcdn.theatlantic.com2Fassets2Fmedia2Fimg2Fmt2F20182F062FGettyImages_52
	 * <p>
	 * The previous Asset class had a getFilename() method which would return:
	 * directresizew2000urlhttps3A2F2Fcdn.theatlantic.com2Fassets2Fmedia2Fimg2Fmt2F20182F062FGettyImages_5216774542Flead_720_405
	 * <p>
	 * And the image downloading code used this for determining the filename of resized images.
	 * If we wanted to refactor this later, we'd have to figure out a code approach for handling assets already on disk that might have the old paths, so it felt out of scope to try to fix.
	 * For now just documenting this quirk.
	 */
	public final String filename;
	
	private Asset(URL url, File local, String filename, int type) {
		this.type = type;
		this.url = url;
		this.local = local;
		this.filename = filename;
	}
	
	@Override
	public String toString() {
		return url.toString();
	}
	
	/** Equality is based on {@link #local} */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Asset asset = (Asset) o;
		return local.equals(asset.local);
	}
	
	/** Equality is based on {@link #local} */
	@Override
	public int hashCode() {
		return local.hashCode();
	}
	
	/** Convenience for {@link #create(URL, int, boolean, AssetDirectory)} as a {@link #IMAGE}. */
	public static Asset createImage(String url, AssetDirectory assetDirectory) {
		try {
			return create(new URL(url), Asset.IMAGE, false, assetDirectory);
		} catch (MalformedURLException e) {
			if (DEBUG) Logs.printStackTrace(e);
			return null;
		}
	}
	
	/**
	 * Attempt to create an asset from the provided url.
	 * If it is an unsupported schema, type or fails due to being an invalid url or other issues it will return null.
	 * @param url See {@link #url}
	 * @param type See {@link #type}
	 * @param forceType (Currently only applies to types of {@link #STYLESHEET}, if true, it will append .css to the filename if not already present.
	 * @param assetDirectory Info about where assets are stored on disk.
	 * @return The asset or null
	 */
	public static Asset create(URL url, int type, boolean forceType, AssetDirectory assetDirectory) {
		try {
			if (!StringUtils2.equalsIgnoreCaseOneOf(url.getProtocol(), "http", "https")) throw new InvalidAssetException("Assets may have a http or https protocol");
			if (StringUtils.containsIgnoreCase(url.toString(), "data:")) throw new InvalidAssetException("Data urls are not allowed");
			
			/*
			 * Convert a url to a valid local directory and filename that we can store the file in that resembles the url
			 *  As an example, convert a url like "getpocket.com/something/file.css?query=value"
			 * 	     			   to a path like "getpocket.com/something/filequeryvalue.css"
			 *
			 * Dev Note: The complexity of this may not be worth trying to make a visually readable url. Could consider making a sha256 hash of domain and paths as the file structure instead.
			 * That will would remove some of the chances of weird paths or paths that are too long. It is possible this would have its own problems, but something to consider if we ever redo this.
			 */
			// Clean up the path string
			String path = url.toString();
			path = cleanUpQueries(path);
			if (type == STYLESHEET && forceType) path = adjustExtension(path, ".css"); // For stylesheets force a .css extensino
			path = PATTERN_CLEAN_PATH.matcher(path).replaceAll("");
			// Break up path parts to figure out folders
			ArrayList<String> pathParts = new ArrayList<>();
			String[] paths = path.split(File.separator);
			if (paths.length > MAX_SUBDIRECTORY_DEPTH) throw new InvalidAssetException("Subdirectory depth too deep: " + path);
			Collections.addAll(pathParts, paths);
			// remove http(s)://
			pathParts.remove(0);
			pathParts.remove(0); // BUG some assets throwing an out of bounds error here. (This m
			// Pull out the compat filename
			String last = pathParts.get(pathParts.size()-1);
			int dot = last.lastIndexOf(".");
			String filename = dot > 0 ? last.substring(0, dot) : last;
			// Get Domain
			String domain = pathParts.remove(0);
			// Assemble the local path, trimming sections that are too long
			StringBuilder localPath = new StringBuilder(path.length())
					.append(File.separator)
					.append(domain);
			for (String appendBlock : pathParts){
				localPath
						.append(File.separator)
						.append(limitFilenameLength(appendBlock));
			}
			String local = localPath.toString();
			
			return new Asset(url, new File(assetDirectory.getAssetsPath(), local), filename, type);
			
		} catch (Throwable t) {
			// Looking at the old code, it seemed like all exceptions were ignored because it is quite common for errors to happen in the large variety of stuff we process
			// Decided to keep this as throwing exceptions and catching them for now so we can at least be clear about the types of errors we might run into when we read the code.
			if (DEBUG) Logs.printStackTrace(t);
			return null;
		}
	}
	
	private static String limitFilenameLength(String value) {
		return value.length() > MAX_DIRECTORY_LENGTH ? value.substring(0, MAX_DIRECTORY_LENGTH) : value;
	}
	
	/**
	 * If there is a query, move it between the filename and extension so the extension is still at the end.
	 * NOTE: In looking at this code, if the url has a fragment (#something) that will also be moved, but it is only moved if there is a query. Not moving a fragment alone is problem an oversight, but leaving it as is for now so it more closely matches the existing conversion
	 */
	private static String cleanUpQueries(String path){
		if(path.contains("?")) {
			
			String file;
			String filename;
			String paths = "";
			String query = "";
			
			// Pull out file name
			int fileIndex = path.lastIndexOf("/");
			if(fileIndex < 0){
				file = path;
			} else {
				paths = path.substring(0, fileIndex);
				file = path.substring(fileIndex+1, path.length());
			}
			
			// Look for a query
			int queryIndex = file.indexOf("?");
			if(queryIndex >= 0){
				query = file.substring(queryIndex+1, file.length());
				filename = file.substring(0, queryIndex);
				
				// Move it in front of extension of file name
				int extensionIndex = filename.lastIndexOf(".");
				String extension = extensionIndex >= 0 ? filename.substring(extensionIndex) : "";
				String fileNoExtension = extensionIndex >= 0 ? filename.substring(0, extensionIndex) : filename;
				
				path = paths.concat(File.separator).concat(fileNoExtension).concat(query).concat(extension);
			}
		}
		
		return path;
	}
	
	/**
	 * Changes the file extension.
	 */
	private static String adjustExtension(String path, String ext){
		// Strip out current extension
		int extensionIndex = path.lastIndexOf(".");
		if(extensionIndex >= 0)
			path = path.substring(0, extensionIndex);
		
		// Add the CSS extension
		return path.concat(ext);
	}
}
