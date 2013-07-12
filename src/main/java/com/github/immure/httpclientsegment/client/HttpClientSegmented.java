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
import java.util.List;
import java.util.Queue;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
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

import com.github.immure.httpclientsegment.client.internal.Segment;
import com.github.immure.httpclientsegment.client.internal.SegmentDownloadThreadPool;
import com.github.immure.httpclientsegment.client.internal.SegmentDownloadWorker;
import com.github.immure.httpclientsegment.util.HttpClientChunkUtil;

public class HttpClientSegmented {

	private HttpClientChunkUtil util = HttpClientChunkUtil.getInstance();
	private final static Log log = LogFactory.getLog(HttpClientSegmented.class);

	public HttpClientSegmented() {
		// TODO Auto-generated constructor stub
	}

	public void copyResourceToStream(URI url, int segments, OutputStream outputStream) throws SegmentedDownloadException {
		if (url == null) {
			throw new IllegalArgumentException("url cannot be null");
		}
		if (segments <= 0) {
			throw new IllegalArgumentException("segments cannot be negative: "
					+ segments);
		}

		long contentLength = getContentSize(url);
		
		if (log.isDebugEnabled())
			log.debug("Content Size: " + contentLength + " (" + util.readableFileSize(contentLength) + ")");
		
		long segmentSize = contentLength / segments;
		
		if (log.isDebugEnabled())
			log.debug("Using segment size of " + segments + " (" + util.readableFileSize(segmentSize) + ")");
		
		
		SegmentDownloadThreadPool threadPool = new SegmentDownloadThreadPool(segments);

		// Create segments

		long currentSize = 0;
		
		List<File> temporaryFiles = new ArrayList<File>(segments); 
		List<FileOutputStream> fileOutputStreams = new ArrayList<FileOutputStream>(segments);

		for (int i = 1; i <= segments; i++) {
			try {
				File f = File.createTempFile(url.getHost(), "-segment-" + i);
				log.debug("Creating: " + f.getName());
				f.createNewFile();
				temporaryFiles.add(f);
				FileOutputStream fos = new FileOutputStream(f);
				fileOutputStreams.add(fos);
				long firstByte = currentSize;
				long lastByte = firstByte + segmentSize;
				currentSize = lastByte + 1;
				if (i == segments) {
					// Last segment, round lastByte up to content size (prevent
					// rounding errors)
					lastByte = contentLength;
				}
				Segment s = new Segment(firstByte, lastByte, url, i);
				SegmentDownloadWorker worker = new SegmentDownloadWorker(s, fos);
				threadPool.addWorker(worker);
			} catch (IOException e) {
				throw new SegmentedDownloadException(e);
			}
		}

		
		threadPool.startDownload();
		while (!threadPool.isComplete()) {
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
			for (File temporaryFile : temporaryFiles) {
				FileInputStream fis = new FileInputStream(temporaryFile);
				IOUtils.copy(fis, outputStream);
				fis.close();
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
		
	}

	private final static int NUM_CHUNKS = 10;

//	public static void main(String[] args) {
//
//		long contentSize = getContentSize();
//		log.debug(readableFileSize(contentSize));
//
//		long chunkSize = contentSize / NUM_CHUNKS;
//		long totalSize = 0;
//
//		FileOutputStream fos = null;
//		File f = null;
//
//		try {
//
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//
//		for (int i = 1; i <= NUM_CHUNKS; i++) {
//			Chunk c;
//			if (i == NUM_CHUNKS) {
//				// Last chunk, round up to full file size
//				c = new Chunk(totalSize, contentSize);
//			} else {
//				c = new Chunk(totalSize, totalSize + chunkSize);
//			}
//			totalSize = totalSize + chunkSize + 1;
//			log.debug(c);
//			InputStream is = getChunk(c.getMin(), c.getMax());
//			try {
//				IOUtils.copy(is, fos);
//				is.close();
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//
//		}
//		try {
//			fos.close();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		log.info("MD5: " + getMd5(f));
//		f.delete();
//
//	}

//	public static String getMd5(File f) {
//		try {
//			return DigestUtils.md5Hex(new FileInputStream(f));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}

	public static long getContentSize(URI uri)
			throws SegmentedDownloadException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(uri);
		for (Header header : httpGet.getAllHeaders()) {
			log.debug("--> Header: " + header);
		}

		log.debug("--> Header size is: " + httpGet.getAllHeaders().length);
		HttpResponse httpResponse = null;
		try {
			httpResponse = httpClient.execute(httpGet);
			long contentLength = Long.parseLong(httpResponse.getFirstHeader(
					"Content-Length").getValue());
			httpGet.releaseConnection();
			return contentLength;
		} catch (ClientProtocolException e) {
			throw new SegmentedDownloadException(e);
		} catch (IOException e) {
			throw new SegmentedDownloadException(e);
		}

	}
}
