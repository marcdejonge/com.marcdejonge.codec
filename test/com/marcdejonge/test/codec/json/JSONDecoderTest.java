package com.marcdejonge.test.codec.json;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.marcdejonge.codec.ParseException;
import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.json.JSONDecoder;

import org.junit.Assert;
import org.junit.Test;

public class JSONDecoderTest {
	@Test
	public void testObjectDecoding() throws ParseException {
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
		testCorrect("{\"number\":34e3}", new MixedMap().$("number", 34e3));
		testCorrect("{\"number\":34.5e3}", new MixedMap().$("number", 34.5e3));
		testCorrect("{\"number\":34.5e-3}", new MixedMap().$("number", 34.5e-3));
		testCorrect("{\"number\":-34.0}", new MixedMap().$("number", -34.0));
		testCorrect("{\"number\":-34e3}", new MixedMap().$("number", -34e3));
		testCorrect("{\"number\":-34.5e3}", new MixedMap().$("number", -34.5e3));
		testCorrect("{\"number\":-34.5e-3}", new MixedMap().$("number", -34.5e-3));
		// The really big numbers become BigDecimals
		testCorrect("{\"number\":34e3000}", new MixedMap().$("number", new BigDecimal("34e3000")));
		testCorrect("{\"number\":465498e-54894}", new MixedMap().$("number", new BigDecimal("465498e-54894")));
		testCorrect("{\"number\":-34e3000}", new MixedMap().$("number", new BigDecimal("-34e3000")));
		testCorrect("{\"number\":-465498e-54894}", new MixedMap().$("number", new BigDecimal("-465498e-54894")));

		// String testing, first some simple tests
		testCorrect("{\"text\":\"\"}", new MixedMap().$("text", ""));
		testCorrect("{\"text\":\"simple\"}", new MixedMap().$("text", "simple"));
		testCorrect("{\"text\":\" simple \"}", new MixedMap().$("text", " simple "));
		testCorrect("{\"text\":\"A longer sentence...\"}", new MixedMap().$("text", "A longer sentence..."));
		testCorrect("{\"text\":\"\\u0073\\u0069\\u006d\\u0070\\u006C\\u0065\"}",
		            new MixedMap().$("text", "simple"));
		testCorrect("{\"text\":\"\\u006a\\u006A\"}", new MixedMap().$("text", "jj"));

		// Testing unicode support
		testCorrect("{\"text\":\"\\u9001 \\u91A8 \\uD6D6\"}", new MixedMap().$("text", "送 醨 훖"));
		testCorrect("{\"text\":\"送 醨 훖\"}", new MixedMap().$("text", "送 醨 훖"));

		// Test escaping characters
		testCorrect("{\"text\":\"\\t \\b \\n \\r \\\\ \\\"\"}", new MixedMap().$("text", "\t \b \n \r \\ \""));

		// Test the extra random whitespace (which should be ignored)
		testCorrect("\n\n  \t{\"number\":49846546573379,   \t\"text\"   \n :\t\" \\tbla\"}   \t",
		            new MixedMap().$("number", 49846546573379L).$("text", " \tbla"));

		// Test the arrays
		testCorrect("[]", new MixedList());
		testCorrect("[0,1,2,3,4,5,6]", new MixedList().$(0).$(1).$(2).$(3).$(4).$(5).$(6));
		testCorrect("[-1,{},true,false,null,{\"x\":[]}]",
		            new MixedList().$(-1)
		                           .$(new MixedMap())
		                           .$(true)
		                           .$(false)
		                           .$(null)
		                           .$(new MixedMap().$("x", new MixedList())));

		// Test a complex object
		testCorrect("{ \"array\" : [], \"long\" : 1234567890, \"inner\":{\"inner\":{}}, \"text\" : \"text\"  }",
		            new MixedMap().$("array", new MixedList())
		                          .$("long", 1234567890L)
		                          .$("inner", new MixedMap().$("inner", new MixedMap()))
		                          .$("text", "text"));

		// Test null values in objects
		testCorrect("{ \"null\" : null }", new MixedMap().$("null", null));
	}

	private void testCorrect(String json, Object expected) throws ParseException {
		Object parsed = JSONDecoder.parse(json);
		Assert.assertEquals(expected, parsed);
	}

	@Test
	public void testObjectDecodingErrors() throws IOException {
		testIncorrect("", "Premature end of file found @ line 1 character 1");
		testIncorrect("{", "Premature end of file found @ line 1 character 2");
		testIncorrect("[", "Premature end of file found @ line 1 character 2");
		testIncorrect("\"", "Premature end of file found @ line 1 character 2");
		testIncorrect("{{", "Unexpected character '{', expected a start of string @ line 1 character 2");
		testIncorrect("{\n\t{", "Unexpected character '{', expected a start of string @ line 2 character 2");
		testIncorrect("{  1", "Unexpected character '1', expected a start of string @ line 1 character 4");
		testIncorrect("\"123", "Premature end of file found @ line 1 character 5");
		testIncorrect("\"123\n\"", "Control character in string found @ line 2 character 0");
		testIncorrect("123. ", "Fraction part started, but no digits found @ line 1 character 5");
		testIncorrect("123.1e ", "Exponential part started, but no digits found @ line 1 character 7");
		testIncorrect("treu", "Unexpected character 'e', expected a 'u' @ line 1 character 3");
		testIncorrect("falze", "Unexpected character 'z', expected a 's' @ line 1 character 4");
		testIncorrect("nul", "Premature end of file found @ line 1 character 4");
		testIncorrect("\"\\u000z\"", "Invalid character for unicode character \'z\' @ line 1 character 7");
		testIncorrect("{ \"dup\":1, \"dup\":2 }", "Duplicate key \"dup\" in object @ line 1 character 12");
	}

	private void testIncorrect(String json, String expectedMessage) throws IOException {
		try {
			Object object = JSONDecoder.parse(json);
			Assert.fail("Expected to fail with message \""
			            + expectedMessage
			            + "\", but it didn't, it returned: "
			            + object);
		} catch (ParseException ex) {
			Assert.assertEquals(expectedMessage, ex.getMessage());
		}
	}

	@Test
	public void testWhitespace() throws IOException, ParseException {
		testCorrect("   \n\t{   \t\t\"test\"  \r \t  : \n\ttrue  }  ", new MixedMap().$("test", true));
		Assert.assertTrue(dec("  \ntrue").parseTrue());
		Assert.assertFalse(dec("  \n\t  false").parseFalse());
		Assert.assertNull(dec("  \rnull").parseNull());
		Assert.assertEquals(123, dec("  \r123").parseNumber());
		Assert.assertEquals(1234567890l, dec("  \r\t\n 1234567890").parseNumber());
	}

	@Test
	public void testJsonStream() throws IOException, ParseException {
		JSONDecoder dec = dec("truefalse{}null[1,2]{\"bla\":\"test\"}true");
		Assert.assertEquals(true, dec.parseValue());
		Assert.assertEquals(false, dec.parseValue());
		Assert.assertEquals(new MixedMap(), dec.parseValue());
		Assert.assertEquals(null, dec.parseValue());
		Assert.assertEquals(new MixedList().$(1, 2), dec.parseValue());
		Assert.assertEquals(new MixedMap().$("bla", "test"), dec.parseValue());
		Assert.assertEquals(true, dec.parseValue());

		try {
			Assert.fail("Expected EOF, but read: " + dec.parseValue());
		} catch (ParseException ex) {
			Assert.assertEquals("Premature end of file found @ line 1 character 40", ex.getMessage());
		}
	}

	private JSONDecoder dec(String json) throws ParseException {
		return new JSONDecoder(new StringReader(json));
	}
}