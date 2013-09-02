package com.github.immure.httpclientsegment.client.progress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.immure.httpclientsegment.client.Segment;

public class LoggingProgressListener implements ProgressListener {
	
	private final static Log log = LogFactory.getLog(LoggingProgressListener.class);
	
	private int progress = 0;

	private Segment s;

	public LoggingProgressListener(Segment s) {
		this.s = s;
	}
	
	@Override
	public void updateProgress(long bytesWritten) {
		long totalBytes = s.getTotalSize();
		float percentComplete = (bytesWritten*100f / totalBytes);
		
		if (log.isTraceEnabled()) {
			log.trace("bytes written: " + bytesWritten + " total bytes: " + totalBytes + " percent: " + percentComplete);
		}
		int newProgress = Math.round(percentComplete);
		if (newProgress > progress) {
			progress = newProgress;
			if ((progress % 10) == 0) {
				log.debug("Segment " + s.getSegmentNumber() + " " + progress  + "% complete");
			} else if (log.isTraceEnabled()) {
				log.trace("Segment " + s.getSegmentNumber() + " " + progress  + "% complete");
			}
		}
	}

}
