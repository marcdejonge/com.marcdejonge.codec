package com.marcdejonge.codec.json;

public class JSONParseException extends Exception {
	private static final long serialVersionUID = -7836090288074585431L;

	private final int lineNumber;
	private final int charNumber;

	public JSONParseException(String message, int lineNumber, int charNumber) {
		super(message + " @ line " + lineNumber + " character " + charNumber);
		this.lineNumber = lineNumber;
		this.charNumber = charNumber;
	}

	public JSONParseException(String message, int lineNumber, int charNumber, Throwable cause) {
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