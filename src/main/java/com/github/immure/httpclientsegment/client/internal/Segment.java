package com.github.immure.httpclientsegment.client.internal;

import java.net.URI;
import java.net.URL;

import com.github.immure.httpclientsegment.util.HttpClientChunkUtil;

public class Segment {
	
	private long firstByte;
	private long lastByte;
	private URI url;
	private int segmentNumber;
	
	private HttpClientChunkUtil util = HttpClientChunkUtil.getInstance();



	public Segment(long firstByte, long lastByte, URI url, int segmentNumber) {
		super();
		this.firstByte = firstByte;
		this.lastByte = lastByte;
		this.url = url;
		this.segmentNumber = segmentNumber;
	}

	public int getSegmentNumber() {
		return segmentNumber;
	}

	public void setSegmentNumber(int segmentNumber) {
		this.segmentNumber = segmentNumber;
	}

	public URI getUrl() {
		return url;
	}

	public void setUrl(URI url) {
		this.url = url;
	}

	public long getFirstByte() {
		return firstByte;
	}

	public void setFirstByte(long firstByte) {
		this.firstByte = firstByte;
	}

	public long getLastByte() {
		return lastByte;
	}

	public void setLastByte(long lastByte) {
		this.lastByte = lastByte;
	}
	
	public long getTotalSize() {
		return getLastByte() - getFirstByte();
	}

	@Override
	public String toString() {
		return "Segment [totalSize=" + getTotalSize() + ", firstByte=" + firstByte + ", lastByte=" + lastByte
				+ ", url=" + url + ", segmentNumber=" + segmentNumber
				+ "]";
	}

	
}

