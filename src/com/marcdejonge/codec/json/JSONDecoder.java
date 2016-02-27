package com.marcdejonge.codec.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.marcdejonge.codec.ParseException;
import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.MixedMap;

public class JSONDecoder {
	public static Object parse(InputStream input) throws ParseException {
		return parse(new InputStreamReader(input));
	}

	public static Object parse(String string) throws ParseException {
		return parse(new StringReader(string));
	}

	public static Object parse(Reader reader) throws ParseException {
		return new JSONDecoder(reader).parseValue();
	}

	private final Reader reader;
	private int lineNumber, charNumber;
	private char c;
	private boolean endOfFile;

	private final StringBuilder buffer = new StringBuilder(512);

	public JSONDecoder(Reader reader) throws ParseException {
		this.reader = reader;

		lineNumber = 1;
		charNumber = 0;
		c = 0;
		endOfFile = false;
	}

	public Object parseValue() throws ParseException {
		skipWhitespace();

		switch (c) {
		case '"':
			return parseString();
		case '{':
			return parseObject();
		case '[':
			return parseArray();
		case '-':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			return parseNumber();
		case 't':
			return parseTrue();
		case 'f':
			return parseFalse();
		case 'n':
			return parseNull();
		default:
			throw new ParseException("Unexpected character '" + c + "' found", lineNumber, charNumber);
		}
	}

	public Number parseNumber() throws ParseException {
		skipWhitespace();

		buffer.setLength(0);
		if (c == '-') {
			buffer.append('-');
			next(false);
		}
		int integerLength = 0, fractionLength = 0, exponentialLength = 0;

		while (c >= '0' && c <= '9') {
			buffer.append(c);
			integerLength++;
			next(true);
		}

		// Parse the fraction part, if found
		if (c == '.') {
			buffer.append('.');
			next(false);
			while (c >= '0' && c <= '9') {
				buffer.append(c);
				fractionLength++;
				next(true);
			}

			if (fractionLength == 0) {
				throw new ParseException("Fraction part started, but no digits found", lineNumber, charNumber);
			}
		}

		// Parse the exponential part, if found
		if (c == 'e' || c == 'E') {
			buffer.append('e');
			next(false);
			if (c == '-') {
				buffer.append('-');
				next(false);
			} else if (c == '+') {
				next(false);
			}

			while (c >= '0' && c <= '9') {
				buffer.append(c);
				exponentialLength++;
				next(true);
			}

			if (exponentialLength == 0) {
				throw new ParseException("Exponential part started, but no digits found", lineNumber, charNumber);
			}
		}

		if (fractionLength == 0 && exponentialLength == 0) {
			// Whole number
			if (integerLength <= 9) {
				return Integer.parseInt(buffer.toString());
			} else if (integerLength <= 18) {
				return Long.parseLong(buffer.toString());
			} else {
				return new BigInteger(buffer.toString());
			}
		} else {
			// Decimal numbers, try and parse as double
			BigDecimal result = new BigDecimal(buffer.toString());
			if (Math.abs(result.scale()) < 1024) {
				return result.doubleValue();
			} else {
				return result;
			}
		}
	}

	public String parseString() throws ParseException {
		skipWhitespace();
		consume('"', "start of string");

		buffer.setLength(0);
		while (true) {
			if (c < 32 || c == 127) {
				throw new ParseException("Control character in string found", lineNumber, charNumber);
			}

			switch (c) {
			case '"':
				next(true);
				return buffer.toString();
			case '\\':
				next(false);
				switch (c) {
				case 'b':
					buffer.append('\b');
					break;
				case 'f':
					buffer.append('\f');
					break;
				case 'n':
					buffer.append('\n');
					break;
				case 'r':
					buffer.append('\r');
					break;
				case 't':
					buffer.append('\t');
					break;
				case 'u':
					buffer.append(parseUnicodePoint());
					break;
				default:
					buffer.append(c);
					break;
				}
				break;
			default:
				buffer.append(c);
				break;
			}

			next(false);
		}
	}

	private char parseUnicodePoint() throws ParseException {
		int unicode = 0;

		for (int ix = 0; ix < 4; ix++) {
			unicode <<= 4;

			next(false);

			switch (c) {
			case '0':
				unicode += 0;
				break;
			case '1':
				unicode += 1;
				break;
			case '2':
				unicode += 2;
				break;
			case '3':
				unicode += 3;
				break;
			case '4':
				unicode += 4;
				break;
			case '5':
				unicode += 5;
				break;
			case '6':
				unicode += 6;
				break;
			case '7':
				unicode += 7;
				break;
			case '8':
				unicode += 8;
				break;
			case '9':
				unicode += 9;
				break;
			case 'a':
			case 'A':
				unicode += 10;
				break;
			case 'b':
			case 'B':
				unicode += 11;
				break;
			case 'c':
			case 'C':
				unicode += 12;
				break;
			case 'd':
			case 'D':
				unicode += 13;
				break;
			case 'e':
			case 'E':
				unicode += 14;
				break;
			case 'f':
			case 'F':
				unicode += 15;
				break;
			default:
				throw new ParseException("Invalid character for unicode character \'"
				                             + c
				                             + "\'",
				                             lineNumber,
				                             charNumber);
			}
		}

		return (char) unicode;
	}

	public MixedList parseArray() throws ParseException {
		skipWhitespace();
		consume('[', "start of array");

		MixedList array = new MixedList();
		boolean first = true;
		while (true) {
			skipWhitespace();

			if (c == ']') {
				next(true);
				return array;
			} else {
				if (first) {
					first = false;
				} else {
					consume(',', "a comma");
					skipWhitespace();
				}

				array.add(parseValue());
			}
		}
	}

	public MixedMap parseObject() throws ParseException {
		skipWhitespace();
		consume('{', "start of object");

		MixedMap object = new MixedMap();
		boolean first = true;
		while (true) {
			skipWhitespace();

			if (c == '}') {
				next(true);
				return object;
			} else {
				if (first) {
					first = false;
				} else {
					consume(',', "a comma");
					skipWhitespace();
				}

				int startLine = lineNumber;
				int startChar = charNumber;

				String name = parseString();
				skipWhitespace();
				consume(':', "colon");
				skipWhitespace();
				Object value = parseValue();

				if (object.put(name, value) != null) {
					throw new ParseException("Duplicate key \"" + name + "\" in object", startLine, startChar);
				}
			}
		}
	}

	public Boolean parseTrue() throws ParseException {
		expectedNext("true".toCharArray());
		return true;
	}

	public Boolean parseFalse() throws ParseException {
		expectedNext("false".toCharArray());
		return false;
	}

	public Object parseNull() throws ParseException {
		expectedNext("null".toCharArray());
		return null;
	}

	private void checkEndOfFile() throws ParseException {
		if (endOfFile) {
			throw new ParseException("Premature end of file found", lineNumber, charNumber);
		}
	}

	private void expectedNext(char... expectedChars) throws ParseException {
		skipWhitespace();
		for (int ix = 0; ix < expectedChars.length; ix++) {
			char expectedChar = expectedChars[ix];
			if (c != expectedChar) {
				throw new ParseException("Unexpected character '"
				                             + c
				                             + "', expected a '"
				                             + expectedChar
				                             + "'",
				                             lineNumber,
				                             charNumber);
			}
			next(ix == expectedChars.length - 1);
		}
	}

	private void consume(char expectedChar, String description) throws ParseException {
		checkEndOfFile();
		if (c != expectedChar) {
			throw new ParseException("Unexpected character '"
			                             + c
			                             + "', expected a "
			                             + description,
			                             lineNumber,
			                             charNumber);
		}
		next(false);
	}

	private void next(boolean allowEof) throws ParseException {
		try {
			int value = reader.read();
			charNumber++;

			if (value < 0) {
				endOfFile = true;
				c = 0;

				if (!allowEof) {
					checkEndOfFile();
				}

				return;
			}

			c = (char) value;
			if (c == '\n') {
				lineNumber++;
				charNumber = 0;
			}
		} catch (IOException ex) {
			throw new ParseException("I/O Error while parsing json", lineNumber, charNumber, ex);
		}
	}

	private void skipWhitespace() throws ParseException {
		while (c == 0 || Character.isWhitespace(c)) {
			next(false);
		}
	}
}
