package com.marcdejonge.codec;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.BaseStream;

import com.marcdejonge.codec.json.JSONDecoder;
import com.marcdejonge.codec.json.JSONEncoder;

/**
 * <p>
 * A {@link MixedList} is a {@link List} of mixed typed objects. This adds a lot of helper methods to the
 * {@link ArrayList} it extends, specifically typed getters and methods to transform objects into a {@link MixedList}
 * or to objects that can accept this type ({@link MixedList#as(Class)}).
 * </p>
 * <p>
 * Also, building a new inline {@link MixedList} is easy using the {@link MixedList#$(Object)} method to do something
 * like:
 * </p>
 *
 * <pre>
 * new MixedList().$(1).$("something").$('3').$(4, 5).$(new String[] { "a", "b" });
 * </pre>
 *
 * @author Marc de Jonge (marcdejonge@gmail.com)
 */
public class MixedList extends ArrayList<Object> {
	private static final long serialVersionUID = 6277744694982281980L;

	/**
	 * @param object
	 *            The iterable object on which this {@link MixedList} should be based.
	 * @return A {@link MixedList} that contains all the elements from the collection. If the object is
	 *         already a {@link MixedList}, the object itself will be casted and returned. Otherwise a new
	 *         {@link MixedList} is created with all the elements copied into it, if possible (e.g. with any iterable or
	 *         stream objects).
	 * @throws UnexpectedTypeException
	 *             When the object can not be iterated over.
	 */
	public static final MixedList from(Object object) throws UnexpectedTypeException {
		if (object instanceof MixedList) {
			return (MixedList) object;
		} else if (object instanceof Collection) {
			return new MixedList((Collection<?>) object);
		} else if (object instanceof Iterable) {
			return new MixedList((Iterable<?>) object);
		} else if (object instanceof BaseStream) {
			return new MixedList((BaseStream<?, ?>) object);
		} else if (object instanceof Map) {
			return new MixedList(((Map<?, ?>) object).entrySet());
		} else {
			throw new UnexpectedTypeException("an iterable object", object);
		}
	}

	/**
	 * @param string
	 *            The JSON input
	 * @return A {@link MixedList} that contains the parsed JSON content
	 * @throws ParseException
	 *             If the JSON does not describe a valid array
	 */
	public static final MixedList fromJSON(String string) throws ParseException {
		return fromJSON(new StringReader(string));
	}

	/**
	 * @param reader
	 *            The JSON input
	 * @return A {@link MixedList} that contains the parsed JSON content
	 * @throws ParseException
	 *             If the JSON does not describe a valid array
	 */
	public static final MixedList fromJSON(Reader reader) throws ParseException {
		return new JSONDecoder(reader).parseArray();
	}

	/**
	 * Creates a new empty {@link MixedList}.
	 */
	public MixedList() {
	}

	/**
	 * Creates a new {@link MixedList} that contains all the elements currently in the {@link Collection}
	 *
	 * @param collection
	 *            The {@link Collection} from which all elements will be read
	 */
	public MixedList(Collection<?> collection) {
		super(collection);
	}

	/**
	 * Creates a new {@link MixedList} that contains all the elements currently in the {@link Iterable}
	 *
	 * @param iterable
	 *            The {@link Iterable} from which all elements will be read
	 */
	public MixedList(Iterable<?> iterable) {
		for (Object value : iterable) {
			add(value);
		}
	}

	/**
	 * Creates a new {@link MixedList} that contains all the elements currently in the {@link BaseStream}
	 *
	 * @param stream
	 *            The {@link BaseStream} from which all elements will be read
	 */
	public MixedList(BaseStream<?, ?> stream) {
		for (Iterator<?> it = stream.iterator(); it.hasNext();) {
			add(it.next());
		}
	}

	/**
	 * This fluent-API method supports adding a object to the end of this list and returning this {@link MixedList}
	 * itself.
	 *
	 * @param value
	 *            The object that you want added to the end of this list
	 * @return this object
	 */
	public MixedList $(Object value) {
		add(value);
		return this;
	}

	/**
	 * This fluent-API method supports adding two objects to the end of this list and returning this {@link MixedList}
	 * itself.
	 *
	 * @param value1
	 *            The first object that you want added to the end of this list
	 * @param value2
	 *            The second object that you want added to the end of this list
	 * @return this object
	 */
	public MixedList $(Object value1, Object value2) {
		add(value1);
		add(value2);
		return this;
	}

	/**
	 * This fluent-API method supports adding three objects to the end of this list and returning this
	 * {@link MixedList} itself.
	 *
	 * @param value1
	 *            The first object that you want added to the end of this list
	 * @param value2
	 *            The second object that you want added to the end of this list
	 * @param value3
	 *            The third object that you want added to the end of this list
	 * @return this object
	 */
	public MixedList $(Object value1, Object value2, Object value3) {
		add(value1);
		add(value2);
		add(value3);
		return this;
	}

	/**
	 * This fluent-API method supports adding several objects to the end of this list and returning this
	 * {@link MixedList} itself.
	 *
	 * @param value1
	 *            The first object that you want added to the end of this list
	 * @param value2
	 *            The second object that you want added to the end of this list
	 * @param value3
	 *            The third object that you want added to the end of this list
	 * @param values
	 *            The rest of the objects that you want added to the end of this list
	 * @return this object
	 */
	public MixedList $(Object value1, Object value2, Object value3, Object... values) {
		add(value1);
		add(value2);
		add(value3);
		for (Object value : values) {
			add(value);
		}
		return this;
	}

	/**
	 * This method tries to translate this {@link MixedList} into a given type by finding a constructor or static
	 * parsing method that accepts this {@link MixedList} as its argument.
	 *
	 * @param clazz
	 *            The type of object that you want this list to be translated into
	 * @return A new instance of type T
	 * @throws UnexpectedTypeException
	 *             When no suitable method has been found to parse or when the parsing in the method itself has failed.
	 */
	@SuppressWarnings("unchecked")
	public <T> T as(Class<T> clazz) throws UnexpectedTypeException {
		try {
			// First try to find a public constructor that accepts this MixedList as its only argument
			for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
				if (Modifier.isPublic(constructor.getModifiers())
				    && constructor.getParameterCount() == 1
				    && constructor.getParameterTypes()[0].isAssignableFrom(MixedList.class)) {
					return (T) constructor.newInstance(this);
				}
			}

			// If no such constructor has been found, try finding a public static parsing method that accepts this
			// MixedList as its only argument
			for (Method method : clazz.getDeclaredMethods()) {
				if (Modifier.isStatic(method.getModifiers())
				    && Modifier.isPublic(method.getModifiers())
				    && method.getParameterCount() == 1
				    && method.getParameters()[0].getType().isAssignableFrom(MixedList.class)
				    && method.getReturnType() == clazz) {
					return (T) method.invoke(null, this);
				}
			}
		} catch (InstantiationException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof UnexpectedTypeException) {
				throw (UnexpectedTypeException) cause;
			}

			throw new UnexpectedTypeException("Failed to parse this MixedList as a " + clazz.getSimpleName(), ex);
		} catch (SecurityException
		         | IllegalAccessException
		         | IllegalArgumentException
		         | InvocationTargetException e) {
			throw new UnexpectedTypeException("Class "
			                                  + clazz.getName()
			                                  + " does not have a public constructor or static method that accepts this MixedList",
			                                  e);
		}

		// No viable method has been found, so throw an exception
		throw new UnexpectedTypeException("Class "
		                                  + clazz.getName()
		                                  + " does not have a public constructor or static method that accepts this MixedList");
	}

	/**
	 * @param index
	 *            The index at which to look
	 * @return the object at the given index, or <code>null</code> when the index is out-of-bounds.
	 */
	public Object getOrNull(int index) {
		if (index < 0 || index >= size()) {
			return null;
		} else {
			return super.get(index);
		}
	}

	/**
	 * @param index
	 *            The index at which to look
	 * @param dflt
	 *            The default value that will be returned when the value is not available
	 * @return the object at the given index, or the default value when the index is out-of-bounds or the value is set
	 *         to null.
	 */
	public Object getOrDefault(int index, Object dflt) {
		if (index < 0 || index >= size() || get(index) == null) {
			return dflt;
		} else {
			return get(index);
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the boolean at the given index, possibly translating a number, where 0 = false and any other value is
	 *         true.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is not a boolean or {@link Number} or the index is out-of-bounds.
	 */
	public boolean getBoolean(int ix) throws UnexpectedTypeException {
		Object value = getOrNull(ix);
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		} else if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		} else {
			throw new UnexpectedTypeException("a boolean", value);
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the boolean at the given index, possibly translating a number, where 0 = false and any other value is
	 *         true, or the default value when the object at the given index is not a boolean or {@link Number} or the
	 *         index is out-of-bounds.
	 */
	public boolean getBoolean(int ix, boolean dlft) {
		Object value = getOrNull(ix);
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		} else if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		} else {
			return dlft;
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the {@link Number} at the given index
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public Number getNumber(int ix) throws UnexpectedTypeException {
		Object value = getOrNull(ix);
		if (value instanceof Number) {
			return (Number) value;
		} else if (value instanceof CharSequence) {
			try {
				return new BigDecimal(value.toString());
			} catch (NumberFormatException ex) {
				throw new UnexpectedTypeException("a number", value);
			}
		} else {
			throw new UnexpectedTypeException("a number", value);
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the {@link Number} at the given index, or the default value when the object at the given index is not a
	 *         {@link Number} or the index is out-of-bounds.
	 */
	public Number getNumber(int ix, Number dflt) {
		Object value = getOrNull(ix);
		if (value instanceof Number) {
			return (Number) value;
		} else if (value instanceof CharSequence) {
			try {
				return new BigDecimal(value.toString());
			} catch (NumberFormatException ex) {
				return dflt;
			}
		} else {
			return dflt;
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the integer at the given index, possibly rounding or truncating when the number when is too big.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public int getInt(int ix) throws UnexpectedTypeException {
		return getNumber(ix).intValue();
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the integer at the given index, possibly rounding or truncating when the number is too big, or the
	 *         default value when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public int getInt(int ix, int dlft) {
		return getNumber(ix, dlft).intValue();
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the long at the given index, possibly rounding or truncating when the number when is too big.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public long getLong(int ix) throws UnexpectedTypeException {
		return getNumber(ix).longValue();
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the long at the given index, possibly rounding or truncating when the number is too big, or the default
	 *         value when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public long getLong(int ix, long dflt) {
		return getNumber(ix, dflt).longValue();
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the float at the given index, possibly rounding or truncating when the number when is too big.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public double getFloat(int ix) throws UnexpectedTypeException {
		return getNumber(ix).floatValue();
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the float at the given index, possibly rounding or truncating when the number is too big, or the default
	 *         value when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public double getFloat(int ix, double dflt) {
		return getNumber(ix, dflt).floatValue();
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the double at the given index, possibly rounding or truncating when the number when is too big.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public double getDouble(int ix) throws UnexpectedTypeException {
		return getNumber(ix).doubleValue();
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the double at the given index, possibly rounding or truncating when the number is too big, or the default
	 *         value when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public double getDouble(int ix, double dflt) {
		return getNumber(ix, dflt).doubleValue();
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the {@link BigInteger} at the given index, possibly rounding when the number when is too big.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public BigInteger getBigInteger(int ix) throws UnexpectedTypeException {
		BigInteger result = getBigInteger(ix, null);
		if (result == null) {
			throw new UnexpectedTypeException("a BigInteger", getOrNull(ix));
		}
		return result;
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the {@link BigInteger} at the given index, possibly rounding when the number is too big, or the default
	 *         value when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public BigInteger getBigInteger(int ix, BigInteger dflt) {
		Object value = getOrNull(ix);
		if (value instanceof BigInteger) {
			return (BigInteger) value;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toBigInteger();
		} else if (value instanceof Number) {
			return BigInteger.valueOf(((Number) value).longValue());
		} else {
			return null;
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the {@link BigDecimal} at the given index.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is not a {@link Number} or the index is out-of-bounds.
	 */
	public BigDecimal getBigDecimal(int ix) throws UnexpectedTypeException {
		BigDecimal result = getBigDecimal(ix, null);
		if (result == null) {
			throw new UnexpectedTypeException("a BigInteger", getOrNull(ix));
		}
		return result;
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the {@link BigDecimal} at the given index, or the default value when the object at the given index is not
	 *         a {@link Number} or the index is out-of-bounds.
	 */
	public BigDecimal getBigDecimal(int ix, BigInteger dflt) {
		Object value = getOrNull(ix);
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		} else if (value instanceof Number) {
			return BigDecimal.valueOf(((Number) value).doubleValue());
		} else {
			return null;
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the {@link String} representation of the object at the given index, using the {@link Object#toString()}
	 *         method of needed.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is missing.
	 */
	public String getString(int ix) throws UnexpectedTypeException {
		Object value = getOrNull(ix);
		if (value == null) {
			throw new UnexpectedTypeException("a string", value);
		} else {
			return value.toString();
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the {@link String} representation of the object at the given index, using the {@link Object#toString()}
	 *         method of needed, or the default value if the object is missing.
	 */
	public String getString(int ix, String dflt) {
		Object value = getOrNull(ix);
		if (value == null) {
			return dflt;
		} else {
			return value.toString();
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the {@link MixedList} at the given index, using the {@link MixedList#from(Collection)} to translate if
	 *         needed.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is missing or is not a valid {@link Collection}.
	 */
	public MixedList getList(int ix) throws UnexpectedTypeException {
		return MixedList.from(getOrNull(ix));
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the {@link MixedList} at the given index, using the {@link MixedList#from(Collection)} to translate if
	 *         needed, or the default value if the object is missing.
	 */
	public MixedList getList(int ix, MixedList dflt) {
		try {
			return MixedList.from(getOrNull(ix));
		} catch (UnexpectedTypeException ex) {
			return dflt;
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @return the {@link MixedMap} at the given index, using the {@link MixedMap#from(Object)} to translate if
	 *         needed.
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is missing or is not a valid {@link MixedMap}.
	 */
	public MixedMap getMap(int ix) throws UnexpectedTypeException {
		return MixedMap.from(getOrNull(ix));
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the {@link MixedMap} at the given index, using the {@link MixedMap#from(Object)} to translate if
	 *         needed, or the default value if the object is missing or is not a valid {@link MixedMap}.
	 */
	public MixedMap getMap(int ix, MixedMap dflt) {
		try {
			return MixedMap.from(getOrNull(ix));
		} catch (UnexpectedTypeException e) {
			return dflt;
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param clazz
	 *            The class of the type that you are looking for
	 * @param <T>
	 *            The type of object that is expected at this index
	 * @return the object of type T at the given index
	 * @throws UnexpectedTypeException
	 *             when the object at the given index is missing or can not be cast into type T.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getAs(int ix, Class<T> clazz) throws UnexpectedTypeException {
		Object value = getOrNull(ix);
		if (value == null) {
			throw new UnexpectedTypeException("an object of type " + clazz.getSimpleName(), value);
		} else if (clazz.isAssignableFrom(value.getClass())) {
			return (T) value;
		} else if (value instanceof MixedMap) {
			return ((MixedMap) value).as(clazz);
		} else {
			throw new UnexpectedTypeException("an object of type " + clazz.getSimpleName(), value);
		}
	}

	/**
	 * @param ix
	 *            The index at which to look
	 * @param clazz
	 *            The class of the type that you are looking for
	 * @param <T>
	 *            The type of object that is expected at this index
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the object of type T at the given index, or the default value if the object is missing or is not of the
	 *         valid type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getAs(int ix, Class<T> clazz, T dflt) {
		Object value = getOrNull(ix);
		if (value == null) {
			return dflt;
		} else if (clazz.isAssignableFrom(value.getClass())) {
			return (T) value;
		} else if (value instanceof MixedMap) {
			try {
				return ((MixedMap) value).as(clazz);
			} catch (UnexpectedTypeException e) {
				return dflt;
			}
		} else {
			return dflt;
		}
	}

	/**
	 * A {@link ListIterator} is an interal implementation to walk through this list for type values. The implementation
	 * of this iterator should implement the {@link #getValue(int)} method to create the real translation.
	 *
	 * @author Marc de Jonge (marcdejonge@gmail.com)
	 *
	 * @param <T>
	 *            The type of objects that we should return.
	 */
	private abstract class ListIterator<T> implements Iterator<T> {
		private int nextIx = 0;
		private T nextResult = null;

		@Override
		public boolean hasNext() {
			while (nextIx < size() && nextResult == null) {
				nextResult = getValue(nextIx++);
			}
			return nextResult != null;
		}

		protected abstract T getValue(int ix);

		@Override
		public T next() {
			if (hasNext()) {
				return nextResult;
			} else {
				throw new IllegalStateException("No more result");
			}
		}
	}

	/**
	 * @return An {@link Iterable} object that can be used to iterate over this {@link MixedList} getting only
	 *         {@link MixedMap} types.
	 */
	public Iterable<MixedMap> objects() {
		return () -> new ListIterator<MixedMap>() {
			@Override
			protected MixedMap getValue(int ix) {
				return getMap(ix, null);
			}
		};
	}

	/**
	 * @return An {@link Iterable} object that can be used to iterate over this {@link MixedList} getting only
	 *         {@link Number} types.
	 */
	public Iterable<Number> numbers() {
		return () -> new ListIterator<Number>() {
			@Override
			protected Number getValue(int ix) {
				return getNumber(ix, null);
			}
		};
	}

	/**
	 * @return An {@link Iterable} object that can be used to iterate over this {@link MixedList} getting only
	 *         {@link String} types.
	 */
	public Iterable<String> strings() {
		return () -> new ListIterator<String>() {
			@Override
			protected String getValue(int ix) {
				return getString(ix, null);
			}
		};
	}

	/**
	 * @return A JSON representation of this list.
	 */
	public String toJSON() {
		return JSONEncoder.toString(this);
	}

	/**
	 * Writes this {@link MixedList} to the output in JSON format.
	 *
	 * @param out
	 *            The output where the JSON will be written to.
	 * @throws IOException
	 *             When an I/O error occurred while writing to the output.
	 */
	public void toJSON(Appendable out) throws IOException {
		JSONEncoder.encode(this, out);
	}

	/**
	 * This gives a detailed string representation of the {@link MixedList}. This is mainly for debug purposes and does
	 * not have a fixed format.
	 *
	 * @see java.util.AbstractCollection#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendTo(sb, 2);
		return sb.toString();
	}

	void appendTo(StringBuilder a, int indent) {
		a.append("[ ");
		for (Iterator<Object> it = iterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj == null) {
				a.append("null");
			} else if (obj instanceof MixedList) {
				((MixedList) obj).appendTo(a, indent + 4);
			} else if (obj instanceof MixedMap) {
				((MixedMap) obj).appendTo(a, indent + 4);
			} else {
				a.append("(")
				 .append(obj.getClass().getSimpleName())
				 .append(") ")
				 .append(obj.toString());
			}

			if (it.hasNext()) {
				a.append(",\n");
				for (int ix = 0; ix < indent; ix++) {
					a.append(' ');
				}
			}
		}

		a.append("\n");
		for (int ix = 0; ix < indent - 2; ix++) {
			a.append(' ');
		}
		a.append("]");
	}
}
