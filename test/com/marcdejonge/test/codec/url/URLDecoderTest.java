package com.marcdejonge.test.codec.url;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.url.URLDecoder;

public class URLDecoderTest {
	@Test
	public void testObjectDecoding() throws IOException {
		testCorrect("", new MixedMap());

		// String testing, first some simple tests
		testCorrect("text=", new MixedMap().$("text", ""));
		testCorrect("text=simple", new MixedMap().$("text", "simple"));
		testCorrect("text=%20simple%20", new MixedMap().$("text", " simple "));
		testCorrect("text=A%20longer%20sentence...", new MixedMap().$("text", "A longer sentence..."));
		testCorrect("text=%73%69%6d%70%6C%65", new MixedMap().$("text", "simple"));
		testCorrect("text=%6a%6A", new MixedMap().$("text", "jj"));

		// Testing unicode support
		testCorrect("text=%e9%80%81%20%e9%86%a8%20%ed%9b%96", new MixedMap().$("text", "送 醨 훖"));
		testCorrect("text=送 醨 훖", new MixedMap().$("text", ""));

		// Test escaping characters
		testCorrect("text=%09%20%08%20%0a%20%0d%20%5c%20%22", new MixedMap().$("text", "\t \b \n \r \\ \""));

		// Test a complex object
		testCorrect("array[]=1&long=1234567890&inner.inner.x=x&text=text",
		            new MixedMap().$("array", new MixedList().$("1"))
		                             .$("long", "1234567890")
		                             .$("inner", new MixedMap().$("inner", new MixedMap().$("x", "x")))
		                             .$("text", "text"));

		// Test null values in objects
		testCorrect("null=null", new MixedMap().$("null", "null"));
	}

	private void testCorrect(String input, MixedMap expected) {
		MixedMap parsed = URLDecoder.parse(input);
		Assert.assertEquals(expected, parsed);
	}
}