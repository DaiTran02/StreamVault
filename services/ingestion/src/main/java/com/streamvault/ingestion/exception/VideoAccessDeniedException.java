package com.streamvault.ingestion.exception;

public class VideoAccessDeniedException extends RuntimeException {

	public VideoAccessDeniedException() {
		super("not authorized to access this video");
	}
}
