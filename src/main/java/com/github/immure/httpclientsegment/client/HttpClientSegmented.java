package com.github.immure.httpclientsegment.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.github.immure.httpclientsegment.client.internal.SegmentDownloadThreadPool;
import com.github.immure.httpclientsegment.client.internal.SegmentDownloadWorker;
import com.github.immure.httpclientsegment.util.HttpClientChunkUtil;

public class HttpClientSegmented {

	private HttpClientChunkUtil util = HttpClientChunkUtil.getInstance();
	private final static Log log = LogFactory.getLog(HttpClientSegmented.class);

	public HttpClientSegmented() {
		// TODO Auto-generated constructor stub
	}
	
	public List<Segment> getSegments(URI url, int numSegments) throws SegmentedDownloadException {
		if (url == null) {
			throw new IllegalArgumentException("url cannot be null");
		}
		if (numSegments <= 0) {
			throw new IllegalArgumentException("segments cannot be negative: "
					+ numSegments);
		}
		
		List<Segment> segments = new ArrayList<Segment>();

		long contentLength = getContentSize(url);
		
		if (log.isDebugEnabled())
			log.debug("Content Size: " + contentLength + " (" + util.readableFileSize(contentLength) + ")");
		
		long segmentSize = contentLength / numSegments;
		
		if (log.isDebugEnabled())
			log.debug("Using " + segments + " segments of size " + segmentSize + " (" + util.readableFileSize(segmentSize) + ")");
		
		long currentSize = 0;
		for (int i = 1; i <= numSegments; i++) {
				long firstByte = currentSize;
				long lastByte = firstByte + segmentSize - 1;
				currentSize = lastByte;
				if (i == numSegments) {
					// Last segment, round lastByte down to content size (prevent
					// rounding errors)
					lastByte = contentLength;
				}
				Segment s = new Segment(firstByte, lastByte, url, i);
				segments.add(s);
		}
		return segments;
	}

	public void copyResourceToStream(URI url, int numSegments, OutputStream outputStream) throws SegmentedDownloadException {
		List<Segment> segments = getSegments(url, numSegments);
		copySegmentsToStream(segments, outputStream);
	}
	
	public void copySegmentsToStream(List<Segment> segments, OutputStream outputStream) throws SegmentedDownloadException {

		// Create worker pool
		SegmentDownloadThreadPool threadPool = new SegmentDownloadThreadPool(segments.size());

		
		// Create temporary files + assign workers
		List<File> temporaryFiles = new ArrayList<File>(segments.size()); 
		List<FileOutputStream> fileOutputStreams = new ArrayList<FileOutputStream>(segments.size());

		int i = 0;
		for (Segment s : segments) {
			try {
				File f = File.createTempFile(s.getUrl().getHost(), "-segment-" + i++);
				log.debug("Creating: " + f.getName());
				f.createNewFile();
				temporaryFiles.add(f);
				FileOutputStream fos = new FileOutputStream(f);
				fileOutputStreams.add(fos);
				SegmentDownloadWorker worker = new SegmentDownloadWorker(s, fos);
				threadPool.addWorker(worker);
			} catch (IOException e) {
				throw new SegmentedDownloadException(e);
			}
		}
		

		threadPool.startDownload();
		while (!threadPool.isComplete()) {
		}
		
		String errorMessage = null;
		
		for (SegmentDownloadWorker worker : threadPool.getWorkers()) {
			if (worker.getHttpCode() != HttpStatus.SC_OK && worker.getHttpCode() != HttpStatus.SC_PARTIAL_CONTENT) {
				switch (worker.getHttpCode()) {
				case HttpStatus.SC_NOT_FOUND:
					errorMessage = "HTTP 404: File not found";
					break;
				default:
					errorMessage = "HTTP " + worker.getHttpCode();
				}
			}
		}
		
		for (FileOutputStream fos : fileOutputStreams) {
			try {
				fos.close();
			} catch (IOException e) {
				log.warn("Was unable to close output stream for file! Probably won't delete.");
			}
		}
		log.debug("Download complete, merging temporary files");
		
		try {
			if (errorMessage == null) {
				for (File temporaryFile : temporaryFiles) {
					FileInputStream fis = new FileInputStream(temporaryFile);
					IOUtils.copy(fis, outputStream);
					fis.close();
				}
			}
		} catch (IOException e) {
			log.warn("Failed to write to output stream, deleting temporary files");
			throw new SegmentedDownloadException(e);
		} finally {
			for (File temporaryFile : temporaryFiles) {
				if (temporaryFile.exists()) {
					log.debug("Deleting: " + temporaryFile.getName());
					try {
						FileUtils.forceDelete(temporaryFile);
					} catch (IOException e) {
						log.warn("Failed to delete (" + e.getMessage() + "): " + temporaryFile.getPath());
					}
					boolean success =temporaryFile.delete();
					if (!success) {
					}
				}
			}
		}
		if (errorMessage != null) {
			throw new SegmentedDownloadException(errorMessage);
		}
	}
	
	public static long getContentSize(URI uri)
			throws SegmentedDownloadException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(uri);
		log.debug("Getting content size for: " + uri);
		for (Header header : httpGet.getAllHeaders()) {
			log.debug("--> Header: " + header);
		}

		log.debug("--> Header size is: " + httpGet.getAllHeaders().length);
		HttpResponse httpResponse = null;
		try {
			httpResponse = httpClient.execute(httpGet);
			long contentLength = Long.parseLong(httpResponse.getFirstHeader(
					"Content-Length").getValue());
			for (Header header : httpResponse.getAllHeaders()) {
				log.debug("<-- Header: " + header.getName() + ": " + header.getValue());
			}
			httpGet.releaseConnection();
			return contentLength;
		} catch (ClientProtocolException e) {
			throw new SegmentedDownloadException(e);
		} catch (IOException e) {
			throw new SegmentedDownloadException(e);
		}

	}
}
