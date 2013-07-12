package com.github.immure.httpclientsegment.client;

public class SegmentedDownloadException extends Exception {

	public SegmentedDownloadException() {
	}

	public SegmentedDownloadException(String message) {
		super(message);
	}

	public SegmentedDownloadException(Throwable cause) {
		super(cause);
	}

	public SegmentedDownloadException(String message, Throwable cause) {
		super(message, cause);
	}

}
