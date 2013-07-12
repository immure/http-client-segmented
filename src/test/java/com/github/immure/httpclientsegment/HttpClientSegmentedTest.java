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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.github.immure.httpclientsegment.client.HttpClientSegmented;
import com.github.immure.httpclientsegment.client.SegmentedDownloadException;

public class HttpClientSegmentedTest {
	@Test
	public void test() throws IOException, SegmentedDownloadException,
			URISyntaxException {


		HttpClientSegmented client = new HttpClientSegmented();
		File output = File.createTempFile("httpclientsegment", "junit");
		System.out.println("Using output:" + output.getPath());
		FileOutputStream fos = new FileOutputStream(output);
		client.copyResourceToStream(new URI(
				"http://lounge.local/~immure/100mb.test"), 10, fos);
		assertEquals("9b5183f05b62ca114c467945467050be", getMd5(output));
		fos.close();
		FileUtils.forceDelete(output);
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
