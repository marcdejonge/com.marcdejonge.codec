package com.marcdejonge.codec;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.BaseStream;

import com.marcdejonge.codec.json.JSONDecoder;
import com.marcdejonge.codec.json.JSONParseException;

/**
 * <p>
 * A {@link MixedMap} is a {@link Map} of string keys to mixed typed objects. This adds a lot of helper methods to the
 * {@link LinkedHashMap} it extends, specifically typed getters and methods to transform objects into a {@link MixedMap}
 * or to objects that can accept this type ({@link MixedMap#as(Class)}).
 * </p>
 * <p>
 * Also, building a new inline {@link MixedMap} is easy using the {@link MixedList#$(String, Object)} method to do
 * something like:
 * </p>
 *
 * <pre>
 * new MixedMap().$("number", 1).$("something", "test").$("three", '3');
 * </pre>
 *
 * @author Marc de Jonge (marcdejonge@gmail.com)
 */
public class MixedMap extends LinkedHashMap<String, Object> {
	private static final long serialVersionUID = -1879873557979864838L;

	/**
	 * @param object
	 *            The map on which this {@link MixedMap} should be based.
	 * @return A {@link MixedMap} that contains all the elements from the source map. If the object is already a
	 *         {@link MixedMap}, the object itself will be casted and returned. Otherwise a new {@link MixedMap} is
	 *         created with all the entries copied into it, if possible.
	 * @throws UnexpectedTypeException
	 *             When the object can not be iterated over.
	 */
	public static final MixedMap from(Object value) throws UnexpectedTypeException {
		if (value instanceof MixedMap) {
			return (MixedMap) value;
		} else if (value instanceof Map) {
			return new MixedMap((Map<?, ?>) value);
		} else if (value instanceof Iterable || value instanceof BaseStream) {
			return new MixedMap(MixedList.from(value));
		} else {
			return new MixedMap(value);
		}
	}

	/**
	 * @param string
	 *            The JSON input
	 * @return A {@link MixedList} that contains the parsed JSON content
	 * @throws JSONParseException
	 *             If the JSON does not describe a valid array
	 */
	public static final MixedMap fromJSON(String string) throws JSONParseException {
		return fromJSON(new StringReader(string));
	}

	/**
	 * @param reader
	 *            The JSON input
	 * @return A {@link MixedList} that contains the parsed JSON content
	 * @throws JSONParseException
	 *             If the JSON does not describe a valid array
	 */
	public static final MixedMap fromJSON(Reader reader) throws JSONParseException {
		return new JSONDecoder(reader).parseObject();
	}

	/**
	 * Creates a new empty MixedMap.
	 */
	public MixedMap() {
	}

	/**
	 * Creates a new MixedMap, with all the key/value-pairs from the source copied into this.
	 *
	 * @param source
	 *            The source map from which to copy
	 */
	public MixedMap(Map<?, ?> source) {
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			put(entry.getKey().toString(), entry.getValue());
		}
	}

	/**
	 * Creates a new {@link MixedMap} from the given {@link MixedList}. The 0-based index are used as keys, with the
	 * corresponding values copied.
	 *
	 * @param list
	 *            The source list from which to copy
	 */
	public MixedMap(MixedList list) {
		for (int ix = 0; ix < list.size(); ix++) {
			put(String.valueOf(ix), list.get(ix));
		}
	}

	/**
	 * Creates a new {@link MixedMap} from the given JavaBean object. This method will search for all the getter methods
	 * and
	 * store the results into this new map.
	 *
	 * @param source
	 *            The source object from which to copy
	 * @throws UnexpectedTypeException
	 *             When the source object is null, or no valid getters could be found.
	 */
	public MixedMap(Object source) throws UnexpectedTypeException {
		if (source == null) {
			throw new UnexpectedTypeException("a JavaBean object", "null");
		}

		Class<? extends Object> clazz = source.getClass();

		for (Method method : clazz.getMethods()) {
			if (method.getReturnType() != Void.TYPE
			    && method.getParameterTypes().length == 0
			    && Modifier.isPublic(method.getModifiers())) {
				String name = method.getName();
				if (name.equals("getClass")) {
					continue;
				} else if (name.length() >= 4 && name.startsWith("get") && Character.isUpperCase(name.charAt(3))) {
					name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
					try {
						Object value = method.invoke(source);
						if (value instanceof Number || value instanceof String) {
							put(name, value);
						} else if (value instanceof Collection) {
							put(name, MixedList.from(value));
						} else {
							put(name, MixedMap.from(value));
						}
					} catch (IllegalAccessException
					         | IllegalArgumentException
					         | InvocationTargetException e) {
						// Ignored
					}
				} else if (name.length() >= 3 && name.startsWith("is") && method.getReturnType() == Boolean.TYPE) {
					try {
						Object value = method.invoke(source);
						put(name, value);
					} catch (IllegalAccessException
					         | IllegalArgumentException
					         | InvocationTargetException e) {
						// Ignored
					}
				}
			}
		}

		if (isEmpty()) {
			throw new UnexpectedTypeException("a JavaBean object", clazz.getName());
		}
	}

	/**
	 * Adds a new key/value pair into this {@link MixedMap}, possibly overriding the current value. It returns this
	 * object, such that it can be used as a fluent-API.
	 *
	 * @param key
	 *            The key under which to store the value
	 * @param value
	 *            The value that is to be stored
	 * @return This object
	 */
	public MixedMap $(String key, Object value) {
		put(key, value);
		return this;
	}

	/**
	 * This method tries to translate this {@link MixedMap} into a given type by finding a constructor or static
	 * parsing method that accepts this {@link MixedMap} as its argument.
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
			// First try to find a public constructor that accepts this MixedMap as its only argument
			for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
				if (Modifier.isPublic(constructor.getModifiers())
				    && constructor.getParameterCount() == 1
				    && constructor.getParameterTypes()[0].isAssignableFrom(MixedMap.class)) {
					return (T) constructor.newInstance(this);
				}
			}

			// If no such constructor has been found, try finding a public static parsing method that accepts this
			// MixedMap as its only argument
			for (Method method : clazz.getDeclaredMethods()) {
				if (Modifier.isStatic(method.getModifiers())
				    && Modifier.isPublic(method.getModifiers())
				    && method.getParameterCount() == 1
				    && method.getParameters()[0].getType().isAssignableFrom(MixedMap.class)
				    && method.getReturnType() == clazz) {
					return (T) method.invoke(null, this);
				}
			}
		} catch (InstantiationException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof UnexpectedTypeException) {
				throw (UnexpectedTypeException) cause;
			}

			throw new UnexpectedTypeException("Failed to parse this MixedMap as a " + clazz.getSimpleName(), ex);
		} catch (SecurityException
		         | IllegalAccessException
		         | IllegalArgumentException
		         | InvocationTargetException e) {
			throw new UnexpectedTypeException("Class "
			                                  + clazz.getName()
			                                  + " does not have a public constructor or static method that accepts this MixedMap",
			                                  e);
		}

		// No viable method has been found, so throw an exception
		throw new UnexpectedTypeException("Class "
		                                  + clazz.getName()
		                                  + " does not have a public constructor or static method that accepts this MixedMap");
	}

	/**
	 * @param key
	 *            The key at which to search for a value
	 * @param dflt
	 *            The default value that will be returned when no value is available under that key
	 * @return the object stored at the given key, or the default value when no such value was found, or the value was
	 *         null.
	 */
	public Object getOrDefault(String key, Object dflt) {
		Object value = get(key);
		if (value != null) {
			return value;
		} else {
			return dflt;
		}
	}

	/**
	 * @param key
	 *            The key at which to search for a value
	 * @return the boolean stored at the given key, possibly translating a number, where 0 = false and any other value
	 *         is true.
	 * @throws UnexpectedTypeException
	 *             when the object at the given key is not a boolean or {@link Number}.
	 */
	public boolean getBoolean(String key) throws UnexpectedTypeException {
		Object value = get(key);
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		} else if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		} else {
			throw new UnexpectedTypeException("a boolean", value);
		}
	}

	/**
	 * @param key
	 *            The key at which to search for a value
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the boolean stored at the given key, possibly translating a number, where 0 = false and any other value
	 *         is true, or the default value when the object at the given index is not a boolean or {@link Number}.
	 */
	public boolean getBoolean(String key, boolean dlft) {
		Object value = get(key);
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		} else if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		} else {
			return dlft;
		}
	}

	/**
	 * @param key
	 *            The key at which to search for a value
	 * @return the {@link Number} stored at the given key, possible parsing the string when needed.
	 * @throws UnexpectedTypeException
	 *             when the object at the given key is not a {@link Number} or .
	 */
	public Number getNumber(String key) throws UnexpectedTypeException {
		Object value = get(key);
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

	public Number getNumber(String key, Number dflt) {
		Object value = get(key);
		if (value instanceof Number) {
			return (Number) value;
		} else {
			return dflt;
		}
	}

	public int getInt(String key) throws UnexpectedTypeException {
		return getNumber(key).intValue();
	}

	public int getInt(String key, int dlft) {
		return getNumber(key, dlft).intValue();
	}

	public long getLong(String key) throws UnexpectedTypeException {
		return getNumber(key).longValue();
	}

	public long getLong(String key, long dflt) {
		return getNumber(key, dflt).longValue();
	}

	public double getFloat(String key) throws UnexpectedTypeException {
		return getNumber(key).floatValue();
	}

	public double getFloat(String key, double dflt) {
		return getNumber(key, dflt).floatValue();
	}

	public double getDouble(String key) throws UnexpectedTypeException {
		return getNumber(key).doubleValue();
	}

	public double getDouble(String key, double dflt) {
		return getNumber(key, dflt).doubleValue();
	}

	public BigInteger getBigInteger(String key) throws UnexpectedTypeException {
		BigInteger result = getBigInteger(key, null);
		if (result == null) {
			throw new UnexpectedTypeException("a BigInteger", get(key));
		}
		return result;
	}

	public BigInteger getBigInteger(String key, BigInteger dflt) {
		Object value = get(key);
		if (value instanceof BigInteger) {
			return (BigInteger) value;
		} else if (value instanceof Number) {
			return BigInteger.valueOf(((Number) value).longValue());
		} else {
			return null;
		}
	}

	public BigDecimal getBigDecimal(String key) throws UnexpectedTypeException {
		BigDecimal result = getBigDecimal(key, null);
		if (result == null) {
			throw new UnexpectedTypeException("a BigInteger", get(key));
		}
		return result;
	}

	public BigDecimal getBigDecimal(String key, BigInteger dflt) {
		Object value = get(key);
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		} else if (value instanceof Number) {
			return BigDecimal.valueOf(((Number) value).doubleValue());
		} else {
			return null;
		}
	}

	public String getString(String key) throws UnexpectedTypeException {
		Object value = get(key);
		if (value == null) {
			throw new UnexpectedTypeException("a string", value);
		} else {
			return value.toString();
		}
	}

	public String getString(String key, String dflt) throws UnexpectedTypeException {
		Object value = get(key);
		if (value == null) {
			return dflt;
		} else {
			return value.toString();
		}
	}

	public MixedList getList(String key) throws UnexpectedTypeException {
		Object value = get(key);
		if (value instanceof Collection) {
			return MixedList.from(value);
		} else {
			throw new UnexpectedTypeException("a collection", value);
		}
	}

	public MixedList getList(String key, MixedList dflt) {
		Object value = get(key);
		if (value instanceof MixedList) {
			return (MixedList) value;
		} else if (value instanceof Collection) {
			return new MixedList((Collection<?>) value);
		} else {
			return dflt;
		}
	}

	public MixedMap getMap(String key) throws UnexpectedTypeException {
		return MixedMap.from(get(key));
	}

	public MixedMap getMap(String key, MixedMap dflt) {
		try {
			return MixedMap.from(get(key));
		} catch (UnexpectedTypeException e) {
			return dflt;
		}
	}

	/**
	 * @param key
	 *            The key at which to look
	 * @param clazz
	 *            The class of the type that you are looking for
	 * @param <T>
	 *            The type of object that is expected at this key
	 * @return the object of type T at the given index
	 * @throws UnexpectedTypeException
	 *             when the object at the given key is missing or can not be cast into type T.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getAs(String key, Class<T> clazz) throws UnexpectedTypeException {
		Object value = get(key);
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
	 * @param key
	 *            The key at which to look
	 * @param clazz
	 *            The class of the type that you are looking for
	 * @param <T>
	 *            The type of object that is expected at this key
	 * @param dflt
	 *            The default value when nothing can be found
	 * @return the object of type T at the given key, or the default value if the object is missing or is not of the
	 *         valid type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getAs(String key, Class<T> clazz, T dflt) {
		Object value = get(key);
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendTo(sb, 2);
		return sb.toString();
	}

	void appendTo(StringBuilder a, int indent) {
		a.append("{ ");
		for (Iterator<Map.Entry<String, Object>> it = entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();

			a.append(entry.getKey()).append(": ");
			Object obj = entry.getValue();
			if (obj == null) {
				a.append("null");
			} else if (obj instanceof MixedList) {
				((MixedList) obj).appendTo(a, indent + entry.getKey().length() + 4);
			} else if (obj instanceof MixedMap) {
				((MixedMap) obj).appendTo(a, indent + entry.getKey().length() + 4);
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
		a.append("}");
	}
}
