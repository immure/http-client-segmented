package com.github.immure.httpclientsegment.client.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SegmentDownloadThreadPool {
	
	private final static Log log = LogFactory.getLog(SegmentDownloadWorker.class);

	private final ExecutorService executorService;
	
	private final List<SegmentDownloadWorker> workers;

	private int numSegments;
	
	private boolean started = false;

	public SegmentDownloadThreadPool(int numSegments) {
		this.numSegments = numSegments;
		executorService = Executors.newFixedThreadPool(numSegments);
		workers = new ArrayList<SegmentDownloadWorker>(numSegments);
	}
	
	public void addWorker(SegmentDownloadWorker worker) {
		if (workers.size() > numSegments) {
			throw new IllegalStateException("Attempted to add worker to a full threadpool");
		}
		workers.add(worker);
	}
	
	public void startDownload() {
		if (started) {
			throw new IllegalStateException("Download has already started");
		}
		started = true;
		for (SegmentDownloadWorker worker : workers) {
			executorService.execute(worker);
		}
		executorService.shutdown();
	}
	
	public boolean isComplete() {
		return executorService.isTerminated();
	}
	
	

}
