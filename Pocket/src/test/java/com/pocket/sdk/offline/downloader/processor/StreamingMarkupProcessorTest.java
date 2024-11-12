package com.pocket.sdk.offline.downloader.processor;

import com.pocket.util.java.BytesUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okio.Okio;

/**
 * Tests of the {@link StreamingMarkupProcessor}.
 * For a full test we should probably do a complete parse of the page specified by https://github.com/Pocket/parser-fixtures/tree/master/src
 * but this has some basic cases for starters.
 */
public class StreamingMarkupProcessorTest {
	
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();
	
	@Test
	public void processHtml() throws IOException {
		// given
		File processingDir = tmp.newFolder();
		File out = tmp.newFile();
		List<String> expected = new ArrayList<>(Arrays.asList( // wrapped to make mutable
				"yay.css",
				"c22.css",
				"../1/c23.css",
				"https://parser-fixtures.readitlater.com/1/c24.css",
				"../1/c25.css",
				"6.png",
				"something.jpg",
				"something2.jpg",
				"something3.jpg",
				"https://getpocket.com",
				"/path/img4.png",
				"https://getpocket.com/absolute/img5.gif"
		));
		List<String> notExpected = Arrays.asList(
				"https://getpocket.com/clean/me"
		);
		StreamingMarkupProcessor processor = new StreamingMarkupProcessor("http://getpocket.com", null, processingDir, out, BytesUtil.mbToBytes(10), (literal, type) -> {
			Assert.assertTrue(expected.remove(literal));
			System.out.println(literal);
			return "["+literal+"]";
		});
		
		// when
		InputStream in = StreamingMarkupProcessor.class.getResourceAsStream("StreamingMarkupProcessorTest/example.html");
		final StreamingMarkupProcessor.Result processed = processor.processHtml(null, Okio.source(in));
		String charset = processed instanceof StreamingMarkupProcessor.HtmlSuccess ? ((StreamingMarkupProcessor.HtmlSuccess) processed).encoding : null;
		String result = FileUtils.readFileToString(out);
		System.out.println(result);
		
		// then
		Assert.assertEquals("UTF-8", charset);
		Assert.assertTrue(expected.isEmpty());
		for (String n : notExpected) {
			Assert.assertEquals(-1, result.indexOf(n));
		}
	}
	
	@Test
	public void processStylesheet() throws IOException {
		// given
		File processingDir = tmp.newFolder();
		File out = tmp.newFile();
		List<String> expected = new ArrayList<>(Arrays.asList( // wrapped to make mutable
				"c22.css",
				"../1/c23.css",
				"https://parser-fixtures.readitlater.com/1/c24.css",
				"../1/c25.css",
				"6.png"
		));
		StreamingMarkupProcessor processor = new StreamingMarkupProcessor("http://getpocket.com", null, processingDir, out, BytesUtil.mbToBytes(10), (literal, type) -> {
			Assert.assertTrue(expected.remove(literal));
			System.out.println(literal);
			return "["+literal+"]";
		});
		
		// when
		InputStream in = StreamingMarkupProcessor.class.getResourceAsStream("StreamingMarkupProcessorTest/example.css");
		boolean valid = processor.processStylesheet(Okio.source(in)) instanceof StreamingMarkupProcessor.Success;
		String result = FileUtils.readFileToString(out);
		System.out.println(result);
		
		// then
		Assert.assertTrue(valid);
		Assert.assertTrue(expected.isEmpty());
	}
	
	
}