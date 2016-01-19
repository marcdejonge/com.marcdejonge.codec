package com.marcdejonge.test.codec;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.UnexpectedTypeException;

public class ObjectTest {
	public static class A {
		private final int x;
		private final String y;
		private final BigInteger z;

		public A() {
			this(0);
		}

		public A(int x) {
			this(x, "test");
		}

		public A(int x, String y) {
			this(x, y, BigInteger.valueOf(0));
		}

		public A(int x, String y, BigInteger z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public A(MixedMap object) throws UnexpectedTypeException {
			x = object.getInt("x");
			y = object.getString("y");
			z = object.getBigInteger("z");
		}

		public int getX() {
			return x;
		}

		public String getY() {
			return y;
		}

		public BigInteger getZ() {
			return z;
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, y, z);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null || getClass() != obj.getClass()) {
				return false;
			} else {
				A other = (A) obj;
				return x == other.x
				       && Objects.equals(y, other.y)
				       && Objects.equals(z, other.z);
			}
		}
	}

	@Test
	public void testA() throws UnexpectedTypeException, IOException {
		A original = new A(3, "test", BigInteger.valueOf(1234567890L));
		MixedMap m = MixedMap.from(original);
		A result = m.as(A.class);

		Assert.assertEquals(original, result);
	}
}