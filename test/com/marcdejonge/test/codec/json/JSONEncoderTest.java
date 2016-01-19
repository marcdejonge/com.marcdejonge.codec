package com.marcdejonge.test.codec.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.json.JSONEncoder;

import org.junit.Assert;
import org.junit.Test;

public class JSONEncoderTest {
	@Test
	public void testObjectDecoding() throws IOException {
		testCorrect("{}", new MixedMap());

		// Number testing, with automatic typing
		// First some int's
		testCorrect("{\"number\":34}", new MixedMap().$("number", 34));
		testCorrect("{\"number\":2154988}", new MixedMap().$("number", 2154988));
		testCorrect("{\"number\":-34}", new MixedMap().$("number", -34));
		testCorrect("{\"number\":-2154988}", new MixedMap().$("number", -2154988));
		// Some longs
		testCorrect("{\"number\":124896378952654}", new MixedMap().$("number", 124896378952654L));
		testCorrect("{\"number\":-124896378952654}", new MixedMap().$("number", -124896378952654L));
		// Really large integers become BigIntegers
		testCorrect("{\"number\":12345678901234567890123456790}",
		            new MixedMap().$("number", new BigInteger("12345678901234567890123456790")));
		testCorrect("{\"number\":-12345678901234567890123456790}",
		            new MixedMap().$("number", new BigInteger("-12345678901234567890123456790")));
		// Some doubles, with fractions and/or exponents
		testCorrect("{\"number\":34.0}", new MixedMap().$("number", 34.0));
		testCorrect("{\"number\":34000.0}", new MixedMap().$("number", 34e3));
		testCorrect("{\"number\":34500.0}", new MixedMap().$("number", 34.5e3));
		testCorrect("{\"number\":0.0345}", new MixedMap().$("number", 34.5e-3));
		testCorrect("{\"number\":-34.0}", new MixedMap().$("number", -34.0));
		testCorrect("{\"number\":-34000.0}", new MixedMap().$("number", -34e3));
		testCorrect("{\"number\":-34500.0}", new MixedMap().$("number", -34.5e3));
		testCorrect("{\"number\":-0.0345}", new MixedMap().$("number", -34.5e-3));
		// The really big numbers become BigDecimals
		testCorrect("{\"number\":3.4E+3001}", new MixedMap().$("number", new BigDecimal("34e3000")));
		testCorrect("{\"number\":4.65498E-54889}", new MixedMap().$("number", new BigDecimal("465498e-54894")));
		testCorrect("{\"number\":-3.4E+3001}", new MixedMap().$("number", new BigDecimal("-34e3000")));
		testCorrect("{\"number\":-4.65498E-54889}", new MixedMap().$("number", new BigDecimal("-465498e-54894")));

		// String testing, first some simple tests
		testCorrect("{\"text\":\"\"}", new MixedMap().$("text", ""));
		testCorrect("{\"text\":\"simple\"}", new MixedMap().$("text", "simple"));
		testCorrect("{\"text\":\" simple \"}", new MixedMap().$("text", " simple "));
		testCorrect("{\"text\":\"A longer sentence...\"}", new MixedMap().$("text", "A longer sentence..."));

		// Testing unicode support
		testCorrect("{\"text\":\"送 醨 훖\"}", new MixedMap().$("text", "送 醨 훖"));

		// Test escaping characters
		testCorrect("{\"text\":\"\\t \\b \\n \\r \\\\ \\\"\"}", new MixedMap().$("text", "\t \b \n \r \\ \""));

		// Test the extra random whitespace (which should be ignored)
		testCorrect("{\"number\":49846546573379,\"text\":\" \\tbla\"}",
		            new MixedMap().$("number", 49846546573379L).$("text", " \tbla"));

		// Test the arrays
		testCorrect("[]", new MixedList());
		testCorrect("[0,1,2,3,4,5,6]", new MixedList().$(0, 1, 2, 3, 4, 5, 6));
		testCorrect("[-1,{},true,false,null,{\"x\":[]}]",
		            new MixedList().$(-1)
		                           .$(new MixedMap())
		                           .$(true)
		                           .$(false)
		                           .$(null)
		                           .$(new MixedMap().$("x", new MixedList())));

		// Test a complex object
		testCorrect("{\"array\":[],\"long\":1234567890,\"inner\":{\"inner\":{}},\"text\":\"text\"}",
		            new MixedMap().$("array", new MixedList())
		                          .$("long", 1234567890L)
		                          .$("inner", new MixedMap().$("inner", new MixedMap()))
		                          .$("text", "text"));

		// Test null values in objects
		testCorrect("{\"null\":null}", new MixedMap().$("null", null));
	}

	private void testCorrect(String expectedJson, Object input) throws IOException {
		Assert.assertEquals(expectedJson, JSONEncoder.toString(input));
	}
}