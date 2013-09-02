package com.github.immure.httpclientsegment;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestListener;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.github.immure.httpclientsegment.client.HttpClientSegmented;
import com.github.immure.httpclientsegment.client.Segment;
import com.github.immure.httpclientsegment.client.SegmentedDownloadException;
import com.github.immure.httpclientsegment.client.progress.LoggingProgressListener;
import com.github.immure.httpclientsegment.client.progress.ProgressListener;

public class HttpClientSegmentedTest {
	@Test
	public void testBasic() throws IOException, SegmentedDownloadException,
			URISyntaxException {


		HttpClientSegmented client = new HttpClientSegmented();
		File output = File.createTempFile("httpclientsegment", "junit");
		System.out.println("Using output:" + output.getPath());
		FileOutputStream fos = new FileOutputStream(output);
		client.copyResourceToStream(new URI(
				"http://localhost:8080/download/63f2c766-7bf4-4c7b-ab3d-73b88e4054b2"), 10, fos);
		String md5 = getMd5(output);
		fos.close();
		FileUtils.forceDelete(output);
		assertEquals("9b5183f05b62ca114c467945467050be", md5);
	}
	
	@Test
	public void testWithListeners() throws IOException, SegmentedDownloadException,
			URISyntaxException {


		HttpClientSegmented client = new HttpClientSegmented();
		File output = File.createTempFile("httpclientsegment", "junit");
		System.out.println("Using output:" + output.getPath());
		FileOutputStream fos = new FileOutputStream(output);
		List<Segment> segments = client.getSegments(new URI("http://localhost:8080/download/63f2c766-7bf4-4c7b-ab3d-73b88e4054b2"), 10);
		List<TestProgressListener> testListeners = new ArrayList<HttpClientSegmentedTest.TestProgressListener>();
		for (Segment s : segments) {
			s.addListener(new LoggingProgressListener(s));
			TestProgressListener progressListener = new TestProgressListener();
			s.addListener(progressListener);
			testListeners.add(progressListener);
		}
		client.copySegmentsToStream(segments, fos);
		String md5 = getMd5(output);
		fos.close();
		FileUtils.forceDelete(output);
		for (TestProgressListener progressListener : testListeners) {
			assertTrue(progressListener.isUpdated());
		}
		assertEquals("9b5183f05b62ca114c467945467050be", md5);
		
	}
	
	private static class TestProgressListener implements ProgressListener {
		
		boolean updated;
		@Override
		public void updateProgress(long bytesDownloaded) {
			updated = true;
		}
		
		public boolean isUpdated() {
			return updated;
		}
	}

	private static String getMd5(File f) {
		try {
			FileInputStream fis = new FileInputStream(f);
			try {
				String md5 = DigestUtils.md5Hex(fis);
				return md5;
			} finally {
				fis.close();
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
