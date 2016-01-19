package com.marcdejonge.codec.url;

import java.io.IOException;
import java.nio.charset.Charset;

public class URLCodec {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static boolean isUrlSafeChar(int c) {
		return c >= '0' && c <= '9'
		       || c >= 'a' && c <= 'z'
		       || c >= 'A' && c <= 'Z'
		       || c == '-'
		       || c == '_'
		       || c == '.'
		       || c == '~';
	}

	private static final char[] hex = "0123456789abcdef".toCharArray();

	public static void encodeEscapedCharacter(Appendable out, char c) throws IOException {
		out.append('%');
		out.append(hex[c >> 4 & 0xf]);
		out.append(hex[c & 0xf]);
	}

	public static int decodeHexChar(int c) {
		if (c >= '0' && c <= '9') {
			return c - '0';
		} else if (c >= 'a' && c <= 'f') {
			return c - 'a' + 10;
		} else if (c >= 'A' && c <= 'F') {
			return c - 'A' + 10;
		} else {
			return -1;
		}
	}

	public static int decodeHexChars(int c1, int c2) {
		return decodeHexChar(c1) << 4 | decodeHexChar(c2);
	}

	URLCodec() {
	}
}
