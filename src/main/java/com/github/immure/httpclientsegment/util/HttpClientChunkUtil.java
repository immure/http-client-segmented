package com.github.immure.httpclientsegment.util;

import java.text.DecimalFormat;

public class HttpClientChunkUtil {
	
	private static HttpClientChunkUtil instance;
	
	public static HttpClientChunkUtil getInstance() {
		if (instance == null)
			instance = new HttpClientChunkUtil();
		return instance;
	}

	public String readableFileSize(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size
				/ Math.pow(1024, digitGroups))
				+ " " + units[digitGroups];
	}

}
