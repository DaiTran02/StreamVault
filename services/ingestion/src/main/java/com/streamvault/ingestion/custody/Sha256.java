package com.streamvault.ingestion.custody;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class Sha256 {

	private Sha256() {
	}

	public static final String GENESIS = "0".repeat(64);

	public static String hex(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 unavailable", ex);
		}
	}

	public static String hex(String value) {
		return hex(value.getBytes(StandardCharsets.UTF_8));
	}

	public static String chainHash(
			String previousChainHash,
			UUID videoId,
			UUID userId,
			String action,
			String contentSha256,
			long epochMilli) {
		String user = userId == null ? "" : userId.toString();
		String canonical = previousChainHash + "|" + videoId + "|" + user + "|" + action + "|" + contentSha256 + "|"
				+ epochMilli;
		return hex(canonical);
	}
}
