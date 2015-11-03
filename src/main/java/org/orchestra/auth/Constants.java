package org.orchestra.auth;

public class Constants {
	public static final String PARAMETER_SIGNATURE = "signature";
	public static final String PARAMETER_APIKEY = "apikey";
	public static final String PARAMETER_NONCE = "nonce";
	public static final String PARAMETER_TIMESTAMP = "timestamp";
	public static final String SIGNED_HEADERS = "SignedHeaders";
	public static final String NEW_LINE = "\n";
	public static final String SIGNATURE_ALGORITHM = "HmacSHA256";
	public static String DATE_FORMAT = "yyyyMMdd";
	public static final String TIMESTAMP_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
	public static final String SECONDS_TIME_FORMAT = "'N'yyyyMMddHHmmss";
	public static final String MINUTES_TIME_FORMAT = "'N'yyyyMMddHHmm";
}
