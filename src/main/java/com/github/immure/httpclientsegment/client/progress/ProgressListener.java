package com.github.immure.httpclientsegment.client.progress;

public interface ProgressListener {
	
	public void updateProgress(long bytesDownloaded);

}
