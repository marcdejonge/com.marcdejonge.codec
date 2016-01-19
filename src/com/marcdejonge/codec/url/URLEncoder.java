package com.marcdejonge.codec.url;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Map.Entry;

import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.UnexpectedTypeException;

public class URLEncoder extends URLCodec {
	public static String toString(MixedMap input) throws UnexpectedTypeException {
		try {
			StringBuffer sb = new StringBuffer();
			encode(input, sb);
			return sb.toString();
		} catch (IOException e) {
			// Should never be able to happen
			throw new AssertionError(e);
		}
	}

	public static void encode(MixedMap input, Appendable output) throws IOException, UnexpectedTypeException {
		new URLEncoder(output).write(input);
	}

	private final ByteBuffer bytes = ByteBuffer.allocate(64);
	private final CharsetEncoder charEncoder;
	private final Appendable out;

	public URLEncoder(Appendable out) {
		this(out, UTF8);
	}

	public URLEncoder(Appendable out, Charset charset) {
		this.out = out;
		charEncoder = charset.newEncoder();
	}

	public void write(MixedMap object) throws IOException, UnexpectedTypeException {
		write("", object);
	}

	public void write(String prepend, MixedMap object) throws IOException, UnexpectedTypeException {
		boolean first = true;
		for (Entry<String, Object> entry : object.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Iterable) {
				for (Object item : (Iterable<?>) value) {
					if (first) {
						first = false;
					} else {
						out.append('&');
					}
					write(prepend);
					write(entry.getKey());
					out.append("[]=");
					write(String.valueOf(item));
				}
			} else if (value instanceof Number || value instanceof CharSequence || value == null) {
				if (first) {
					first = false;
				} else {
					out.append('&');
				}
				write(prepend);
				write(entry.getKey());
				out.append('=');
				write(String.valueOf(value));
			} else {
				if (first) {
					first = false;
				} else {
					out.append('&');
				}
				write(prepend + entry.getKey() + ".", MixedMap.from(value));
			}
		}
	}

	private void write(CharSequence string) throws IOException {
		CharBuffer chars = CharBuffer.wrap(string);

		while (chars.hasRemaining()) {
			bytes.clear();
			charEncoder.encode(chars, bytes, true);
			bytes.flip();

			while (bytes.hasRemaining()) {
				char c = (char) bytes.get();
				if (isUrlSafeChar(c)) {
					out.append(c);
				} else {
					encodeEscapedCharacter(out, c);
				}
			}
		}
	}
}
