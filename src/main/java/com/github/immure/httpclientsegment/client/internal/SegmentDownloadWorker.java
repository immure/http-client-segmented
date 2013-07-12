package com.github.immure.httpclientsegment.client.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.github.immure.httpclientsegment.client.SegmentedDownloadException;

public class SegmentDownloadWorker implements Runnable {

	private final Log log;
	private final Segment segment;
	private OutputStream outputStream;

	public SegmentDownloadWorker(Segment segment, OutputStream outputStream) {
		if (segment == null) {
			throw new IllegalArgumentException("segment cannot be null");
		}
		if (outputStream == null) {
			throw new IllegalArgumentException("outputstream cannot be null");
		}
		this.segment = segment;
		this.outputStream = outputStream;
		log = LogFactory.getLog(SegmentDownloadWorker.class + "-"
				+ segment.getSegmentNumber());
	}

	public SegmentDownloadWorker(URI url, int firstByte, int lastByte,
			int segmentNumber, OutputStream outputStream) {
		this(new Segment(firstByte, lastByte, url, segmentNumber), outputStream);
	}

	private void stream() throws SegmentedDownloadException {

		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(segment.getUrl());
		httpGet.addHeader("Range", "bytes=" + segment.getFirstByte() + "-"
				+ segment.getLastByte());
		for (Header header : httpGet.getAllHeaders()) {
			log.debug("--> Header: " + header);
		}

		HttpResponse httpResponse = null;
		try {
			httpResponse = httpClient.execute(httpGet);
		} catch (ClientProtocolException e) {
			throw new SegmentedDownloadException(e);
		} catch (IOException e) {
			throw new SegmentedDownloadException(e);
		}

		HttpEntity httpEntity = httpResponse.getEntity();
		try {
			InputStream httpInputStream = httpEntity.getContent();
			log.debug("--> StatusLine: " + httpResponse.getStatusLine());
			IOUtils.copy(httpInputStream, outputStream);
			httpInputStream.close();
		} catch (IllegalStateException e) {
			throw new SegmentedDownloadException(e);
		} catch (IOException e) {
			throw new SegmentedDownloadException(e);
		}

	}

	@Override
	public void run() {
		try {
		stream();
		} catch (SegmentedDownloadException e) {
			throw new RuntimeException(e);
		}
		
	}

}
