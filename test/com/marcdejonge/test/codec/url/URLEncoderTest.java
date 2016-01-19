package com.marcdejonge.test.codec.url;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.UnexpectedTypeException;
import com.marcdejonge.codec.url.URLEncoder;

public class URLEncoderTest {
	@Test
	public void testObjectDecoding() throws IOException, UnexpectedTypeException {
		testCorrect("", new MixedMap());

		// Number testing, with automatic typing
		// First some int's
		testCorrect("number=34", new MixedMap().$("number", 34));
		testCorrect("number=2154988", new MixedMap().$("number", 2154988));
		testCorrect("number=-34", new MixedMap().$("number", -34));
		testCorrect("number=-2154988", new MixedMap().$("number", -2154988));
		// Some longs
		testCorrect("number=124896378952654", new MixedMap().$("number", 124896378952654L));
		testCorrect("number=-124896378952654", new MixedMap().$("number", -124896378952654L));
		// Really large integers become BigIntegers
		testCorrect("number=12345678901234567890123456790",
		            new MixedMap().$("number", new BigInteger("12345678901234567890123456790")));
		testCorrect("number=-12345678901234567890123456790",
		            new MixedMap().$("number", new BigInteger("-12345678901234567890123456790")));
		// Some doubles, with fractions and/or exponents
		testCorrect("number=34.0", new MixedMap().$("number", 34.0));
		testCorrect("number=34000.0", new MixedMap().$("number", 34e3));
		testCorrect("number=34500.0", new MixedMap().$("number", 34.5e3));
		testCorrect("number=0.0345", new MixedMap().$("number", 34.5e-3));
		testCorrect("number=-34.0", new MixedMap().$("number", -34.0));
		testCorrect("number=-34000.0", new MixedMap().$("number", -34e3));
		testCorrect("number=-34500.0", new MixedMap().$("number", -34.5e3));
		testCorrect("number=-0.0345", new MixedMap().$("number", -34.5e-3));
		// The really big numbers become BigDecimals
		testCorrect("number=3.4E%2b3001", new MixedMap().$("number", new BigDecimal("34e3000")));
		testCorrect("number=4.65498E-54889", new MixedMap().$("number", new BigDecimal("465498e-54894")));
		testCorrect("number=-3.4E%2b3001", new MixedMap().$("number", new BigDecimal("-34e3000")));
		testCorrect("number=-4.65498E-54889", new MixedMap().$("number", new BigDecimal("-465498e-54894")));

		// String testing, first some simple tests
		testCorrect("text=", new MixedMap().$("text", ""));
		testCorrect("text=simple", new MixedMap().$("text", "simple"));
		testCorrect("text=%20simple%20", new MixedMap().$("text", " simple "));
		testCorrect("text=A%20longer%20sentence...", new MixedMap().$("text", "A longer sentence..."));

		// Testing unicode support
		testCorrect("text=%e9%80%81%20%e9%86%a8%20%ed%9b%96", new MixedMap().$("text", "送 醨 훖"));

		// Test escaping characters
		testCorrect("text=%09%20%08%20%0a%20%0d%20%5c%20%22", new MixedMap().$("text", "\t \b \n \r \\ \""));

		// Test the arrays
		testCorrect("array[]=0&array[]=1&array[]=2&array[]=3&array[]=4&array[]=5&array[]=6",
		            new MixedMap().$("array", new MixedList().$(0).$(1).$(2).$(3).$(4).$(5).$(6)));
		testCorrect("array[]=-1&array[]=true&array[]=false&array[]=null",
		            new MixedMap().$("array", new MixedList().$(-1)
		                                                         .$(true)
		                                                         .$(false)
		                                                         .$(null)));

		// Test a complex object
		testCorrect("array[]=1&long=1234567890&inner.inner.x=x&text=text",
		            new MixedMap().$("array", new MixedList().$(1))
		                             .$("long", 1234567890L)
		                             .$("inner", new MixedMap().$("inner", new MixedMap().$("x", "x")))
		                             .$("text", "text"));

		// Test null values in objects
		testCorrect("null=null", new MixedMap().$("null", null));
	}

	private void testCorrect(String expectedCode, MixedMap input) throws IOException, UnexpectedTypeException {
		Assert.assertEquals(expectedCode, URLEncoder.toString(input));
	}
}
