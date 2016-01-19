package com.marcdejonge.test.codec;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.UnexpectedTypeException;

import org.junit.Test;

public class MixedListTests {
	@Test
	public void testFrom() throws UnexpectedTypeException {
		MixedList empty = new MixedList();
		assertSame(empty, MixedList.from(empty));
		assertEquals(empty, MixedList.from(new HashSet<>()));
		assertEquals(empty, MixedList.from(new HashMap<>()));

		assertEquals(new MixedList().$(1), MixedList.from(Arrays.asList(1)));
		assertEquals(new MixedList().$("1"), MixedList.from(Arrays.asList("1")));

		assertEquals(new MixedList().$(1, 2), MixedList.from(IntStream.range(1, 3)));
		assertEquals(new MixedList().$(1L, 2L, 3L), MixedList.from(LongStream.range(1, 4)));

		assertEquals("A=B", MixedList.from(Collections.singletonMap("A", "B")).getString(0, "miss"));

		try {
			fail("Expected to throw an exception, but got a " + MixedList.from("test"));
		} catch (UnexpectedTypeException ex) {
			assertEquals("Unexpected type, expected [an iterable object], but got a [String]", ex.getMessage());
		}
	}

	@Test
	public void testGetOrNull() {
		MixedList list = new MixedList().$(1, null, 3);
		assertNull(list.getOrNull(-1));
		assertEquals(1, list.getOrNull(0));
		assertNull(list.getOrNull(1));
		assertEquals(3, list.getOrNull(2));
		assertNull(list.getOrNull(3));

		assertNull(list.getOrNull(Integer.MIN_VALUE));
		assertNull(list.getOrNull(Integer.MAX_VALUE));
	}

	@Test
	public void testGetOrDflt() {
		MixedList list = new MixedList().$(1, null, 3);
		assertNull(list.getOrDefault(-1, null));
		assertEquals(1, list.getOrDefault(0, 9999));
		assertEquals(9999, list.getOrDefault(1, 9999));
		assertEquals(3, list.getOrDefault(2, -9999));
		assertEquals(-9999, list.getOrDefault(3, -9999));

		assertNull(list.getOrDefault(Integer.MIN_VALUE, null));
		assertNull(list.getOrDefault(Integer.MAX_VALUE, null));
	}

	@Test
	public void testBoolean() throws UnexpectedTypeException {
		MixedList list = new MixedList().$(true, false, 0, 1, BigInteger.valueOf(99), "not a valid value");

		try {
			fail("Expected an exception, but got a " + list.getBoolean(-1));
		} catch (UnexpectedTypeException ex) {
			assertEquals("Unexpected type, expected [a boolean], but got a [null object]", ex.getMessage());
		}

		assertTrue(list.getBoolean(0));
		assertFalse(list.getBoolean(1));
		assertFalse(list.getBoolean(2));
		assertTrue(list.getBoolean(3));
		assertTrue(list.getBoolean(4));

		try {
			fail("Expected an exception, but got a " + list.getBoolean(5));
		} catch (UnexpectedTypeException ex) {
			assertEquals("Unexpected type, expected [a boolean], but got a [String]", ex.getMessage());
		}

		assertTrue(list.getBoolean(0, false));
		assertFalse(list.getBoolean(1, true));
		assertFalse(list.getBoolean(2, true));
		assertTrue(list.getBoolean(3, false));
		assertTrue(list.getBoolean(4, false));

		assertTrue(list.getBoolean(5, true));
		assertFalse(list.getBoolean(5, false));
	}

	@Test
	public void testInt() throws UnexpectedTypeException {
		MixedList list = new MixedList().$(0, 1, -99, 128L, Long.MAX_VALUE, BigInteger.valueOf(64), "xxx", true);

		assertEquals(0, list.getInt(0));
		assertEquals(1, list.getInt(1));
		assertEquals(-99, list.getInt(2));
		assertEquals(128, list.getInt(3));
		assertEquals(-1, list.getInt(4));
		assertEquals(64, list.getInt(5));

		try {
			fail("Expected an exception, but got a " + list.getInt(6));
		} catch (UnexpectedTypeException ex) {
			assertEquals("Unexpected type, expected [a number], but got a [String]", ex.getMessage());
		}
		try {
			fail("Expected an exception, but got a " + list.getInt(7));
		} catch (UnexpectedTypeException ex) {
			assertEquals("Unexpected type, expected [a number], but got a [Boolean]", ex.getMessage());
		}

		assertEquals(0, list.getInt(0, 9));
		assertEquals(1, list.getInt(1, 9));
		assertEquals(-99, list.getInt(2, 9));
		assertEquals(128, list.getInt(3, 9));
		assertEquals(-1, list.getInt(4, 9));
		assertEquals(64, list.getInt(5, 9));
		assertEquals(9, list.getInt(6, 9));
		assertEquals(9, list.getInt(7, 9));
	}
}
