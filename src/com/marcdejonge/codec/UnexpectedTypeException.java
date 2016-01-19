package com.marcdejonge.codec;

/**
 * A {@link UnexpectedTypeException} is an exception that can be throws by the typed getter methods from the
 * {@link MixedList} and {@link MixedMap} when the type of the object is different than what the developer expected
 * as input. This can usually be interpreted as a parsing error (e.g. when translating JSON input into a java object).
 *
 * @author Marc de Jonge (marcdejonge@gmail.com)
 */
public class UnexpectedTypeException extends Exception {
	private static final long serialVersionUID = 4456179500435312084L;

	/**
	 * Constructs a new UnexpectedTypeException with the specified detail message. The cause is not initialized, and may
	 * subsequently
	 * be initialized by a call to {@link #initCause(Throwable)}.
	 *
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
	 *            method.
	 */
	public UnexpectedTypeException(String message) {
		super(message);
	}

	/**
	 * Constructs a new UnexpectedTypeException with the specified detail message and cause.
	 *
	 * Note that the detail message associated with cause is not automatically incorporated in this exception's detail
	 * message.
	 *
	 *
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
	 *            method.
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is
	 *            permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public UnexpectedTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new UnexpectedTypeException with a generated detail message from the expected and seen parameters.
	 *
	 * @param expected
	 *            The description of the type that was expected.
	 * @param seen
	 *            The description of the type that has been seen.
	 */
	public UnexpectedTypeException(String expected, String seen) {
		super("Unexpected type, expected [" + expected + "], but got a [" + seen + "]");
	}

	/**
	 * Constructs a new UnexpectedTypeException with a generated detail message from the expected and seen parameters.
	 *
	 * @param expected
	 *            The description of the type that was expected.
	 * @param seen
	 *            The object that has been seen
	 */
	public UnexpectedTypeException(String expected, Object seen) {
		this(expected, seen == null ? "null object" : seen.getClass().getSimpleName());
	}
}
