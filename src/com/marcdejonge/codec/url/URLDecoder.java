package com.marcdejonge.codec.url;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.MixedMap;

public class URLDecoder extends URLCodec {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static MixedMap parse(InputStream input) throws IOException {
		return parse(new InputStreamReader(input));
	}

	public static MixedMap parse(String string) {
		try {
			return parse(new StringReader(string));
		} catch (IOException e) {
			throw new AssertionError("I/O Error should never be possible when reading a String", e);
		}
	}

	public static MixedMap parse(Reader reader) throws IOException {
		return new URLDecoder(reader).parseValue();
	}

	private final ByteBuffer bytes = ByteBuffer.allocate(64);
	private final CharBuffer chars = CharBuffer.allocate(64);
	private final CharsetDecoder decoder;
	private final Reader reader;

	public URLDecoder(Reader reader) {
		this(reader, UTF8);
	}

	public URLDecoder(Reader reader, Charset charset) {
		this.reader = reader;
		decoder = charset.newDecoder();
	}

	public MixedMap parseValue() throws IOException {
		MixedMap result = new MixedMap();
		StringBuilder key = new StringBuilder();
		StringBuilder value = new StringBuilder();

		int c = 0;
		while (c >= 0) {
			c = readTo(key, true);

			if (c != '&') {
				// There is still a value to be found
				c = readTo(value, false);
			}

			if (key.length() > 0) {
				save(result, key.toString(), value.toString());
			}
		}

		return result;
	}

	private int readTo(StringBuilder sb, boolean forKey) throws IOException {
		bytes.clear();
		chars.clear();
		sb.setLength(0);

		int c;
		while ((c = reader.read()) >= 0) {
			if (forKey && c == '=' || c == '&') {
				break; // End of key
			} else if (c == '%') {
				// Try and read 2 more bytes
				int b = decodeHexChars(reader.read(), reader.read());
				if (b >= 0) { // Invalid escade sequences will be ignored
					bytes.put((byte) b);
				}
			} else if (c == '+') {
				bytes.put((byte) ' ');
			} else if (forKey && (c == '[' || c == ']')) {
				bytes.put((byte) c);
			} else if (isUrlSafeChar(c)) {
				bytes.put((byte) c);
			}

			if (!bytes.hasRemaining()) {
				bytes.flip();
				chars.clear();
				decoder.decode(bytes, chars, false);
				chars.flip();
				sb.append(chars);

				bytes.compact();
			}
		}

		if (bytes.hasRemaining()) {
			bytes.flip();
			chars.clear();
			decoder.decode(bytes, chars, true);
			chars.flip();
			sb.append(chars);
		}

		return c;
	}

	private static void save(MixedMap result, String key, Object value) {
		int dotIx = key.indexOf('.');
		if (dotIx < 0) {
			if (key.endsWith("[]")) {
				// Drop the brackets for the real name
				key = key.substring(0, key.length() - 2);

				// Save in an MixedArray
				Object object = result.get(key);
				if (object == null) {
					result.$(key, new MixedList().$(value));
				} else if (object instanceof MixedList) {
					((MixedList) object).add(value);
				} else {
					// We don't save it if the parent was previously added as a different object
				}
			} else {
				// This is a simple saved result
				result.$(key, value);
			}
		} else {
			String parentKey = key.substring(0, dotIx);
			String childKey = key.substring(dotIx + 1);

			// Save in a MixedObject
			Object object = result.get(key);
			if (object == null) {
				MixedMap mixedObject = new MixedMap();
				save(mixedObject, childKey, value);
				result.$(parentKey, mixedObject);
			} else if (object instanceof MixedMap) {
				save((MixedMap) object, childKey, value);
			} else {
				// We don't save it if the parent was previously added as a different object
			}
		}
	}
}
