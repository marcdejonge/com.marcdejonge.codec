package com.marcdejonge.codec;

public class ParseException extends Exception {
	private static final long serialVersionUID = -7836090288074585431L;

	private final int lineNumber;
	private final int charNumber;

	public ParseException(Throwable cause) {
		super(cause);
		lineNumber = -1;
		charNumber = -1;
	}

	public ParseException(String message) {
		this(message, -1, -1);
	}

	public ParseException(String message, int lineNumber, int charNumber) {
		super(message + " @ line " + lineNumber + " character " + charNumber);
		this.lineNumber = lineNumber;
		this.charNumber = charNumber;
	}

	public ParseException(String message, int lineNumber, int charNumber, Throwable cause) {
		super(message + " @ line " + lineNumber + " character " + charNumber, cause);
		this.lineNumber = lineNumber;
		this.charNumber = charNumber;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getCharNumber() {
		return charNumber;
	}
}