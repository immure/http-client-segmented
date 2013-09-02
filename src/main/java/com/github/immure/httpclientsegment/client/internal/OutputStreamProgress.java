package com.github.immure.httpclientsegment.client.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import com.github.immure.httpclientsegment.client.progress.ProgressListener;

/**
 * Original code from: http://stackoverflow.com/questions/7057342/how-to-get-a-progress-bar-for-a-file-upload-with-apache-httpclient-4
 * @author http://stackoverflow.com/users/435605/kilaka
 *
 */
public class OutputStreamProgress extends OutputStream { 
    private final OutputStream outstream;
    private volatile long bytesWritten=0;
    private List<ProgressListener> progressListeners;

    public OutputStreamProgress(OutputStream outstream) {
    	this(outstream,Collections.<ProgressListener>emptyList());
    }
    
    public OutputStreamProgress(OutputStream outstream, List<ProgressListener> progressListeners) {
        this.outstream = outstream;
        this.progressListeners = progressListeners;
    }

    @Override
    public void write(int b) throws IOException {
        outstream.write(b);
        bytesWritten++;
        updateListeners(bytesWritten);
    }

    @Override
    public void write(byte[] b) throws IOException {
        outstream.write(b);
        bytesWritten += b.length;
        updateListeners(bytesWritten);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outstream.write(b, off, len);
        bytesWritten += len;
        updateListeners(bytesWritten);
    }

    @Override
    public void flush() throws IOException {
        outstream.flush();
    }

    @Override
    public void close() throws IOException {
        outstream.close();
    }

    public long getWrittenLength() {
        return bytesWritten;
    }
    
    protected void updateListeners(long bytesWritten) {
    	for (ProgressListener listener : progressListeners) {
    		listener.updateProgress(bytesWritten);
    	}
    }
}