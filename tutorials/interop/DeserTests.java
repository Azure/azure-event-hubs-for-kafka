package com.microsoft.azure.eventhubs.kafkasupport;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.Test;

import com.microsoft.azure.eventhubs.kafkasupport.AmqpDeserializer.DescribedType;


public class DeserTests {
	final AmqpDeserializer deser = new AmqpDeserializer();
	final String topicName = "unused";
	
	@Test
	public void BadInputBufferTest() {
		final byte[] noBytes = new byte[0]; 
		
		try {
			this.deser.deserialize(this.topicName, null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserialize(this.topicName, noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeDescribedType(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeDescribedType(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeBoolean(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeBoolean(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeUnsignedInteger(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeUnsignedInteger(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeSignedInteger(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeSignedInteger(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeFloatingPoint(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeFloatingPoint(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeCharacter(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeCharacter(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeTimestamp(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeTimestamp(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeUUID(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeUUID(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeSymbol(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeSymbol(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeString(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeString(noBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	private void invalidRangeOfTypesTest(final int first, final int last) {
		for (int i = first; i <= last; i++) {
			final byte[] rawbytes = { (byte)i, 0x01, 0x02 };
			try {
				this.deser.deserialize(this.topicName, rawbytes);
				fail("Expected UnsupportedOperationException");
			}
			catch (UnsupportedOperationException e) {
				// OK
			}
		}
	}
	
	@Test
	public void InvalidTypesTest() {
		final int[] ranges = {
				0x01, 0x3F,	// Nothing between 0x00 (described) and 0x40 (null)
				0x46, 0x4F,	// unused zero-data-byte range
				0x56, 0x5F,	// unused one-data-byte range
				0x62, 0x6F,	// unused two-data-bytes range
				0x75, 0x7F,	// unused four-data-bytes range
				0x85, 0x93,	// unused eight-data-bytes range, 16-data-bytes low range unused
				0x95, 0x97,	// 16-data-bytes mid range unused
				0x99, 0x9F,	// 16-data-bytes high range unused
				0xA2, 0xA2,
				0xA4, 0xAF,	// unused variable width
				0xB2, 0xB2,
				0xB4, 0xBF, // unused variable width
				0xC2, 0xCF,	// unused compound
				0xD2, 0xDF,	// unused compound
				0xE1, 0xEF,	// unused array
				0xF1, 0xFF	// unused array
		};
		for(int i = 0; i < ranges.length; i += 2) {
			invalidRangeOfTypesTest(ranges[i], ranges[i + 1]);
		}
	}
	
	private void verifyDescribedType(final byte[] rawbytes) {
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof DescribedType);
		DescribedType dt = (DescribedType)tryObj;
		assertNotNull(dt.getDescriptor());
		assertEquals(dt.getDescriptor(), "com.microsoft:uri");
		assertNotNull(dt.getValue());
		assertTrue(dt.getValue() instanceof String);
		assertEquals(dt.getValue(), "http://www.contoso.com/");
		
		dt = this.deser.deserializeDescribedType(rawbytes);
		assertNotNull(dt);
		assertNotNull(dt.getDescriptor());
		assertEquals(dt.getDescriptor(), "com.microsoft:uri");
		assertNotNull(dt.getValue());
		assertTrue(dt.getValue() instanceof String);
		assertEquals(dt.getValue(), "http://www.contoso.com/");
	}
	
	private void verifyBadDescribedType(final byte[] rawbytes) {
		try {
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		try {
			this.deser.deserializeDescribedType(rawbytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type00DescribedTest() {
		final byte[] goodBytesShortShort = {
				0x00,		// described type
				(byte)0xA3,	// descriptor is short symbol
				0x11,		// length 17
				(byte)'c', (byte)'o', (byte)'m', (byte)'.',
				(byte)'m', (byte)'i', (byte)'c', (byte)'r',
				(byte)'o', (byte)'s', (byte)'o', (byte)'f',
				(byte)'t', (byte)':', (byte)'u', (byte)'r', (byte)'i',
				(byte)0xA1,	// value is short string
				0x17,		// length 23
				(byte)'h', (byte)'t', (byte)'t', (byte)'p', (byte)':',
				(byte)'/', (byte)'/', (byte)'w', (byte)'w', (byte)'w',
				(byte)'.', (byte)'c', (byte)'o', (byte)'n', (byte)'t',
				(byte)'o', (byte)'s', (byte)'o', (byte)'.', (byte)'c',
				(byte)'o', (byte)'m', (byte)'/'
		};
		verifyDescribedType(goodBytesShortShort);

		byte[] goodBytesLongLong = {
				0x00,		// described type
				(byte)0xB3,	// descriptor is long symbol
				0x00, 0x00, 0x00, 0x11,		// length 17
				(byte)'c', (byte)'o', (byte)'m', (byte)'.',
				(byte)'m', (byte)'i', (byte)'c', (byte)'r',
				(byte)'o', (byte)'s', (byte)'o', (byte)'f',
				(byte)'t', (byte)':', (byte)'u', (byte)'r', (byte)'i',
				(byte)0xB1,	// value is long string
				0x00, 0x00, 0x00, 0x17,		// length 23
				(byte)'h', (byte)'t', (byte)'t', (byte)'p', (byte)':',
				(byte)'/', (byte)'/', (byte)'w', (byte)'w', (byte)'w',
				(byte)'.', (byte)'c', (byte)'o', (byte)'n', (byte)'t',
				(byte)'o', (byte)'s', (byte)'o', (byte)'.', (byte)'c',
				(byte)'o', (byte)'m', (byte)'/'
		};
		verifyDescribedType(goodBytesLongLong);
		
		final byte[] completelyBad = { 0x71 };
		try {
			this.deser.deserializeDescribedType(completelyBad);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		byte saved = goodBytesLongLong[1];
		goodBytesLongLong[1] = 0x71; // bad descriptor type
		verifyBadDescribedType(goodBytesLongLong);
		
		goodBytesLongLong[1] = (byte)0xA3; // legal descriptor type but doesn't match data
		verifyBadDescribedType(goodBytesLongLong);
		
		goodBytesLongLong[1] = saved;
		saved = goodBytesLongLong[5];
		goodBytesLongLong[5] = 0x05; // symbol length too short
		verifyBadDescribedType(goodBytesLongLong);

		goodBytesLongLong[5] = saved;
		saved = goodBytesLongLong[4];
		goodBytesLongLong[4] = 0x0A; // symbol length too long
		verifyBadDescribedType(goodBytesLongLong);
		
		goodBytesLongLong[4] = saved;
		saved = goodBytesLongLong[6];
		goodBytesLongLong[6] = (byte)'z'; // unrecognized symbol in descriptor
		verifyBadDescribedType(goodBytesLongLong);
		
		goodBytesLongLong[6] = saved;
		saved = goodBytesLongLong[23];
		goodBytesLongLong[23] = 0x71; // bad value type
		verifyBadDescribedType(goodBytesLongLong);
		
		goodBytesLongLong[23] = saved;
		saved = goodBytesLongLong[27];
		goodBytesLongLong[27] = 0x7F; // value length too long
		verifyBadDescribedType(goodBytesLongLong);
	}
	
	@Test
	public void Type40NullTest() {
		final byte[] rawbytes = { 0x40 };
		
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNull(tryObj);
	}
	
	@Test
	public void Type4142BooleanTest() {
		final byte[] rawbytesTrue = { 0x41 };
		final byte[] rawbytesFalse = { 0x42 };
		
		Object tryObj = this.deser.deserialize(this.topicName, rawbytesTrue);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Boolean);
		assertTrue((Boolean)tryObj);
		
		tryObj = this.deser.deserialize(this.topicName, rawbytesFalse);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Boolean);
		assertFalse((Boolean)tryObj);
		
		tryObj = this.deser.deserializeBoolean(rawbytesTrue);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Boolean);
		assertTrue((Boolean)tryObj);
		
		tryObj = this.deser.deserializeBoolean(rawbytesFalse);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Boolean);
		assertFalse((Boolean)tryObj);
		
		try {
			final byte[] rawbytesDifferentType = { 0x43 };
			tryObj = this.deser.deserializeBoolean(rawbytesDifferentType);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type4344ZeroTest() {
		byte[] rawbytes = { 0x43 }; // zero uint
		unsignedSubtest(rawbytes, 0L, Long.class);
		
		rawbytes[0] = 0x44; // zero ulong
		unsignedSubtest(rawbytes, 0L, Long.class);
		
		try {
			final byte[] rawbytesDifferentType = { 0x41 };
			this.deser.deserializeUnsignedInteger(rawbytesDifferentType);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type45EmptyListTest() {
		try {
			final byte[] rawbytes = { 0x45 }; // zero-length list
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	private void unsignedSubtest(final byte[] rawbytes, final long expectedValue, Class<?> expectedType) {
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj.getClass().equals(expectedType));
		// convert the hard way
		if (tryObj instanceof Short) {
			tryObj = new Long((Short)tryObj);
		}
		else if (tryObj instanceof Integer) {
			tryObj = new Long((Integer)tryObj);
		}
		assertEquals(tryObj, expectedValue);
		
		tryObj = this.deser.deserializeUnsignedInteger(rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Long);
		assertEquals(tryObj, expectedValue);
	}
	
	private void u8Subtest(final byte[] rawbytes, Class<?> expectedType) {
		rawbytes[1] = 0x00;
		unsignedSubtest(rawbytes, 0L, expectedType);

		rawbytes[1] = 0x01;
		unsignedSubtest(rawbytes, 1L, expectedType);
		
		rawbytes[1] = 0x7F;
		unsignedSubtest(rawbytes, 127L, expectedType);
		
		rawbytes[1] = (byte)0x80;
		unsignedSubtest(rawbytes, 128L, expectedType);
		
		rawbytes[1] = (byte)0xFF;
		unsignedSubtest(rawbytes, 255L, expectedType);
	}
	
	@Test
	public void Type505253U8Test() {
		// 50 Ubyte
		byte[] rawbytes = { 0x50, 0x00 };
		u8Subtest(rawbytes, Short.class);
		
		// 52 Small uint
		rawbytes[0] = 0x52;
		u8Subtest(rawbytes, Long.class);
		
		// 53 Small ulong
		rawbytes[0] = 0x53;
		u8Subtest(rawbytes, Long.class);
		
		// deserializeUnsignedInteger was tried with bad bytes in Type4344ZeroTest
		
		// Type4344ZeroTest also tried a one-byte buffer which is the only shorter buffer possible
	}
	
	@Test
	public void Type60U16Test() {
		byte[] rawbytes = { 0x60, 0x00, 0x00 };
		unsignedSubtest(rawbytes, 0L, Integer.class);
		
		rawbytes[2] = 0x01;
		unsignedSubtest(rawbytes, 1L, Integer.class);
		
		rawbytes[1] = 0x7F;
		rawbytes[2] = (byte)0xFF;
		unsignedSubtest(rawbytes, 32767L, Integer.class);
		
		rawbytes[1] = (byte)0x80;
		rawbytes[2] = 0x00;
		unsignedSubtest(rawbytes, 32768L, Integer.class);
		
		rawbytes[1] = (byte)0xFF;
		rawbytes[2] = (byte)0xFF;
		unsignedSubtest(rawbytes, 65535L, Integer.class);

		// deserializeUnsignedInteger was tried with bad bytes in Type4344ZeroTest
		
		final byte[] tooShortBytes = { 0x60, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeUnsignedInteger(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type70U32Test() {
		byte[] rawbytes = { 0x70, 0x00, 0x00, 0x00, 0x00 };
		unsignedSubtest(rawbytes, 0L, Long.class);
		
		rawbytes[4] = 0x01;
		unsignedSubtest(rawbytes, 1L, Long.class);
		
		rawbytes[1] = 0x7F;
		rawbytes[2] = (byte)0xFF;
		rawbytes[3] = (byte)0xFF;
		rawbytes[4] = (byte)0xFF;
		unsignedSubtest(rawbytes, 0x7FFFFFFFL, Long.class);
		
		rawbytes[1] = (byte)0x80;
		rawbytes[2] = 0x00;
		rawbytes[3] = 0x00;
		rawbytes[4] = 0x00;
		unsignedSubtest(rawbytes, 0x80000000L, Long.class);
		
		rawbytes[1] = (byte)0xFF;
		rawbytes[2] = (byte)0xFF;
		rawbytes[3] = (byte)0xFF;
		rawbytes[4] = (byte)0xFF;
		unsignedSubtest(rawbytes, 0xFFFFFFFFL, Long.class);

		// deserializeUnsignedInteger was tried with bad bytes in Type4344ZeroTest
		
		final byte[] tooShortBytes = { 0x70, 0x00, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeUnsignedInteger(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type80U64Test() {
		byte[] rawbytes = { (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		unsignedSubtest(rawbytes, 0L, Long.class);
		
		rawbytes[8] = 0x01;
		unsignedSubtest(rawbytes, 1L, Long.class);
		
		rawbytes[1] = 0x7F;
		rawbytes[2] = (byte)0xFF;
		rawbytes[3] = (byte)0xFF;
		rawbytes[4] = (byte)0xFF;
		rawbytes[5] = (byte)0xFF;
		rawbytes[6] = (byte)0xFF;
		rawbytes[7] = (byte)0xFF;
		rawbytes[8] = (byte)0xFF;
		unsignedSubtest(rawbytes, Long.MAX_VALUE, Long.class);
		
		rawbytes[1] = (byte)0x80;
		rawbytes[2] = 0x00;
		rawbytes[3] = 0x00;
		rawbytes[4] = 0x00;
		rawbytes[5] = 0x00;
		rawbytes[6] = 0x00;
		rawbytes[7] = 0x00;
		rawbytes[8] = 0x00;
		unsignedSubtest(rawbytes, 0x8000000000000000L, Long.class);
		
		rawbytes[1] = (byte)0xFF;
		rawbytes[2] = (byte)0xFF;
		rawbytes[3] = (byte)0xFF;
		rawbytes[4] = (byte)0xFF;
		rawbytes[5] = (byte)0xFF;
		rawbytes[6] = (byte)0xFF;
		rawbytes[7] = (byte)0xFF;
		rawbytes[8] = (byte)0xFF;
		unsignedSubtest(rawbytes, 0xFFFFFFFFFFFFFFFFL, Long.class);

		// deserializeUnsignedInteger was tried with bad bytes in Type4344ZeroTest
		
		final byte[] tooShortBytes = { (byte)0x80, 0x00, 0x00, 0x00, 0x00};
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeUnsignedInteger(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	private void signedSubtest(final byte[] rawbytes, final long expectedValue, Class<?> expectedType) {
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj.getClass().equals(expectedType));
		// convert the hard way
		if (tryObj instanceof Byte) {
			tryObj = new Long((Byte)tryObj);
		}
		else if (tryObj instanceof Short) {
			tryObj = new Long((Short)tryObj);
		}
		else if (tryObj instanceof Integer) {
			tryObj = new Long((Integer)tryObj);
		}
		assertEquals(tryObj, expectedValue);
		
		tryObj = this.deser.deserializeSignedInteger(rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Long);
		assertEquals(tryObj, expectedValue);
	}
	
	private void s8Subtest(final byte[] rawbytes, Class<?> expectedType) {
		rawbytes[1] = 0x00;
		signedSubtest(rawbytes, 0L, expectedType);

		rawbytes[1] = 0x01;
		signedSubtest(rawbytes, 1L, expectedType);
		
		rawbytes[1] = 0x7F;
		signedSubtest(rawbytes, 127L, expectedType);
		
		rawbytes[1] = (byte)0x80;
		signedSubtest(rawbytes, -128L, expectedType);
		
		rawbytes[1] = (byte)0xFF;
		signedSubtest(rawbytes, -1L, expectedType);
	}
	
	@Test
	public void Type515455S8Test() {
		// 51 Byte
		byte[] rawbytes = { 0x51, 0x00 };
		s8Subtest(rawbytes, Byte.class);
		
		// 54 Small int
		rawbytes[0] = 0x54;
		s8Subtest(rawbytes, Integer.class);
		
		// 55 Small long
		rawbytes[0] = 0x55;
		s8Subtest(rawbytes, Long.class);
	}
	
	@Test
	public void Type61S16Test() {
		byte[] rawbytes = { 0x61, 0x00, 0x00 };
		signedSubtest(rawbytes, 0L, Short.class);
		
		rawbytes[2] = 0x01;
		signedSubtest(rawbytes, 1L, Short.class);
		
		rawbytes[1] = 0x7F;
		rawbytes[2] = (byte)0xFF;
		signedSubtest(rawbytes, 32767L, Short.class);
		
		rawbytes[1] = (byte)0x80;
		rawbytes[2] = 0x00;
		signedSubtest(rawbytes, -32768L, Short.class);
		
		rawbytes[1] = (byte)0xFF;
		rawbytes[2] = (byte)0xFF;
		signedSubtest(rawbytes, -1L, Short.class);

		try {
			final byte[] rawbytesDifferentType = { 0x41 };
			this.deser.deserializeSignedInteger(rawbytesDifferentType);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		final byte[] tooShortBytes = { 0x61, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeSignedInteger(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type71S32Test() {
		byte[] rawbytes = { 0x71, 0x00, 0x00, 0x00, 0x00 };
		signedSubtest(rawbytes, 0L, Integer.class);
		
		rawbytes[4] = 0x01;
		signedSubtest(rawbytes, 1L, Integer.class);
		
		rawbytes[1] = 0x7F;
		rawbytes[2] = (byte)0xFF;
		rawbytes[3] = (byte)0xFF;
		rawbytes[4] = (byte)0xFF;
		signedSubtest(rawbytes, Integer.MAX_VALUE, Integer.class);
		
		rawbytes[1] = (byte)0x80;
		rawbytes[2] = 0x00;
		rawbytes[3] = 0x00;
		rawbytes[4] = 0x00;
		signedSubtest(rawbytes, Integer.MIN_VALUE, Integer.class);
		
		rawbytes[1] = (byte)0xFF;
		rawbytes[2] = (byte)0xFF;
		rawbytes[3] = (byte)0xFF;
		rawbytes[4] = (byte)0xFF;
		signedSubtest(rawbytes, -1L, Integer.class);

		// deserializeSignedInteger was tried with bad bytes in Type61S16Test
		
		final byte[] tooShortBytes = { 0x71, 0x00, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeSignedInteger(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type81S64Test() {
		byte[] rawbytes = { (byte)0x81, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		signedSubtest(rawbytes, 0L, Long.class);
		
		rawbytes[8] = 0x01;
		signedSubtest(rawbytes, 1L, Long.class);
		
		rawbytes[1] = 0x7F;
		rawbytes[2] = (byte)0xFF;
		rawbytes[3] = (byte)0xFF;
		rawbytes[4] = (byte)0xFF;
		rawbytes[5] = (byte)0xFF;
		rawbytes[6] = (byte)0xFF;
		rawbytes[7] = (byte)0xFF;
		rawbytes[8] = (byte)0xFF;
		signedSubtest(rawbytes, Long.MAX_VALUE, Long.class);
		
		rawbytes[1] = (byte)0x80;
		rawbytes[2] = 0x00;
		rawbytes[3] = 0x00;
		rawbytes[4] = 0x00;
		rawbytes[5] = 0x00;
		rawbytes[6] = 0x00;
		rawbytes[7] = 0x00;
		rawbytes[8] = 0x00;
		signedSubtest(rawbytes, Long.MIN_VALUE, Long.class);
		
		rawbytes[1] = (byte)0xFF;
		rawbytes[2] = (byte)0xFF;
		rawbytes[3] = (byte)0xFF;
		rawbytes[4] = (byte)0xFF;
		rawbytes[5] = (byte)0xFF;
		rawbytes[6] = (byte)0xFF;
		rawbytes[7] = (byte)0xFF;
		rawbytes[8] = (byte)0xFF;
		signedSubtest(rawbytes, -1L, Long.class);

		// deserializeSignedInteger was tried with bad bytes in Type61S16Test
		
		final byte[] tooShortBytes = { (byte)0x81, 0x00, 0x00, 0x00, 0x00};
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeSignedInteger(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	
	private void floatSubtest(final float testValue) {
		final int intbits = Float.floatToRawIntBits(testValue);
		final byte[] rawbytes = { 0x72,
				(byte)(intbits >> 24),
				(byte)(intbits >> 16),
				(byte)(intbits >> 8),
				(byte)intbits
			};
		
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj.getClass().equals(Float.class));
		assertEquals(tryObj, testValue);
		
		tryObj = this.deser.deserializeFloatingPoint(rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Double);
		assertEquals(tryObj, new Double(testValue));
	}
	
	@Test
	public void Type72F32Test() {
		floatSubtest(0.0F);
		floatSubtest(1.0F);
		floatSubtest(-1.0F);
		floatSubtest(Float.MIN_VALUE);
		floatSubtest(Float.MAX_VALUE);
		floatSubtest(Float.MIN_NORMAL);
		floatSubtest(Float.NaN);
		floatSubtest(Float.NEGATIVE_INFINITY);
		floatSubtest(Float.POSITIVE_INFINITY);
		
		final byte[] badTypeBytes = { 0x71, 0x00, 0x00, 0x00, 0x00 };
		try {
			this.deser.deserializeFloatingPoint(badTypeBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		final byte[] tooShortBytes = { 0x72, 0x00, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeFloatingPoint(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}

	private void doubleSubtest(final double testValue) {
		final long longbits = Double.doubleToRawLongBits(testValue);
		final byte[] rawbytes = { (byte)0x82,
				(byte)(longbits >> 56),
				(byte)(longbits >> 48),
				(byte)(longbits >> 40),
				(byte)(longbits >> 32),
				(byte)(longbits >> 24),
				(byte)(longbits >> 16),
				(byte)(longbits >> 8),
				(byte)longbits
			};
		
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj.getClass().equals(Double.class));
		assertEquals(tryObj, testValue);
		
		tryObj = this.deser.deserializeFloatingPoint(rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Double);
		assertEquals(tryObj, testValue);
	}
	
	@Test
	public void Type82F64Test() {
		doubleSubtest(0.0D);
		doubleSubtest(1.0D);
		doubleSubtest(-1.0D);
		doubleSubtest(Double.MIN_VALUE);
		doubleSubtest(Double.MAX_VALUE);
		doubleSubtest(Double.MIN_NORMAL);
		doubleSubtest(Double.NaN);
		doubleSubtest(Double.NEGATIVE_INFINITY);
		doubleSubtest(Double.POSITIVE_INFINITY);
		
		// deserializeFloatingPoint() was tried with bad bytes in Type72F32Test
		
		final byte[] tooShortBytes = { (byte)0x82, 0x00, 0x00, 0x00, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeFloatingPoint(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	
	
	@Test
	public void Type74Decimal32Test() {
		try {
			final byte[] rawbytes = { 0x74, 0x00, 0x00, 0x00, 0x00 };
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	@Test
	public void Type84Decimal64Test() {
		try {
			final byte[] rawbytes = { (byte)0x84, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	@Test
	public void Type94Decimal128Test() {
		try {
			final byte[] rawbytes = { (byte)0x94, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	private void charSubtest(final byte[] rawbytes, final char[] expectedValue) {
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof char[]);
		char[] tryChars = (char[])tryObj;
		assertEquals(tryChars.length, expectedValue.length);
		for (int i = 0; i < tryChars.length; i++) {
			assertEquals(tryChars[i], expectedValue[i]);
		}
		
		tryObj = this.deser.deserializeCharacter(rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof char[]);
		tryChars = (char[])tryObj;
		assertEquals(tryChars.length, expectedValue.length);
		for (int i = 0; i < tryChars.length; i++) {
			assertEquals(tryChars[i], expectedValue[i]);
		}
	}
	
	@Test
	public void Type73UTF32CharTest() {
		byte[] rawbytes = { 0x73, 0x00, 0x00, 0x27, 0x64 };
		charSubtest(rawbytes, Character.toChars(0x2764));

		rawbytes[3] = 0x00;
		rawbytes[4] = (byte)'A';
		charSubtest(rawbytes, new char[] { 'A' });
		
		rawbytes[4] = 0x00;
		charSubtest(rawbytes, new char[] { '\u0000' });
		
		final byte[] badTypeBytes = { 0x71, 0x00, 0x00, 0x00, 0x00 };
		try {
			this.deser.deserializeCharacter(badTypeBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		final byte[] tooShortBytes = { 0x73, 0x00, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeCharacter(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type83TimestampTest() {
		final byte[] rawbytes = { (byte)0x83, 0x00, 0x00, 0x00, 0x00, 0x12, 0x34, 0x56, 0x78 };
		final Instant expected = Instant.ofEpochMilli(0x0000000012345678L);

		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Instant);
		assertEquals((Instant)tryObj, expected);
		
		tryObj = this.deser.deserializeTimestamp(rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof Instant);
		assertEquals((Instant)tryObj, expected);
		
		final byte[] badTypeBytes = { (byte)0x84, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		try {
			this.deser.deserializeTimestamp(badTypeBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		final byte[] tooShortBytes = { (byte)0x83, 0x00, 0x00, 0x00, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeTimestamp(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	@Test
	public void Type98UUIDTest() {
		final byte[] rawbytes = { (byte)0x98, 0x12, 0x34, 0x56, 0x78, (byte)0xA1, (byte)0xA2, (byte)0xB1, (byte)0xB2, (byte)0xC1, (byte)0xC2,
				0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
		final UUID expected = new UUID(0x12345678A1A2B1B2L, 0xC1C2010203040506L);
		
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof UUID);
		assertEquals((UUID)tryObj, expected);
		
		tryObj = this.deser.deserializeUUID(rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof UUID);
		assertEquals((UUID)tryObj, expected);
		
		final byte[] badTypeBytes = { (byte)0x94, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		try {
			this.deser.deserializeUUID(badTypeBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		final byte[] tooShortBytes = { (byte)0x98, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		try {
			this.deser.deserialize(this.topicName, tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			this.deser.deserializeUUID(tooShortBytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}
	
	
	
	@Test
	public void TypeA0ShortBinaryTest() {
		byte[] rawbytes = { (byte)0xA0, 0x07, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
		
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof byte[]);
		byte[] tryBytes = (byte[])tryObj;
		assertEquals(tryBytes.length, rawbytes.length - 2);
		for (int i = 0; i < tryBytes.length; i++) {
			assertEquals(tryBytes[i], rawbytes[i + 2]);
		}
		
		rawbytes[1] = 0x7F; // too long
		try {
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}

		rawbytes[1] = 0x00; // empty
		tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof byte[]);
		tryBytes = (byte[])tryObj;
		assertEquals(tryBytes.length, 0);
	}
	
	@Test
	public void TypeB0LongBinaryTest() {
		byte[] rawbytes = { (byte)0xB0, 0x00, 0x00, 0x01, 0x07, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x42,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x43,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x44,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x45,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x46,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x47,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x48,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x49,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4A,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4B,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4C,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4D,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4E,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4F,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x50,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x51
		};
		
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof byte[]);
		byte[] tryBytes = (byte[])tryObj;
		assertEquals(tryBytes.length, rawbytes.length - 5);
		for (int i = 0; i < tryBytes.length; i++) {
			assertEquals(tryBytes[i], rawbytes[i + 5]);
		}
		
		rawbytes[1] = 0x7F; // too long
		try {
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}

		rawbytes[1] = 0x00;
		rawbytes[3] = 0x00; // represent short binary as long
		tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof byte[]);
		tryBytes = (byte[])tryObj;
		assertEquals(tryBytes.length, 7);
		for (int i = 0; i < tryBytes.length; i++) {
			assertEquals(tryBytes[i], rawbytes[i + 5]);
		}

		rawbytes[4] = 0x00; // empty
		tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof byte[]);
		tryBytes = (byte[])tryObj;
		assertEquals(tryBytes.length, 0);
	}
	
	private void stringishSubtest(final byte[] rawbytes, final boolean isSymbol, final String expected) {
		Object tryObj = this.deser.deserialize(this.topicName, rawbytes);
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof String);
		assertEquals((String)tryObj, expected);
		
		if (isSymbol) {
			tryObj = this.deser.deserializeSymbol(rawbytes);
		} else {
			tryObj = this.deser.deserializeString(rawbytes);
		}
		assertNotNull(tryObj);
		assertTrue(tryObj instanceof String);
		assertEquals((String)tryObj, expected);
	}
	
	private void shortStringishSubtest(final byte type, final boolean isSymbol) {
		byte[] rawbytes = { type, 0x0B, (byte)'h', (byte)'e', (byte)'l', (byte)'l', (byte)'o',
				(byte)' ', (byte)'w', (byte)'o', (byte)'r', (byte)'l', (byte)'d'
		};
		stringishSubtest(rawbytes, isSymbol, "hello world");
		
		rawbytes[0] = (byte)0xA0; // bad type
		try {
			if (isSymbol) {
				this.deser.deserializeSymbol(rawbytes);
			} else {
				this.deser.deserializeString(rawbytes);
			}
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		rawbytes[0] = type;
		rawbytes[1] = 0x7F; // too long
		try {
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			if (isSymbol) {
				this.deser.deserializeSymbol(rawbytes);
			} else {
				this.deser.deserializeString(rawbytes);
			}
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}

		rawbytes[1] = 0x00; // empty string
		stringishSubtest(rawbytes, isSymbol, "");
	}
	
	private void longStringishSubtest(final byte type, final boolean isSymbol) {
		byte[] rawbytes = { type, 0x00, 0x00, 0x01, 0x0B, (byte)'h', (byte)'e', (byte)'l', (byte)'l', (byte)'o',
				(byte)' ', (byte)'w', (byte)'o', (byte)'r', (byte)'l', (byte)'d',
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x42,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x43,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x44,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x45,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x46,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x47,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x48,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x49,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4A,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4B,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4C,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4D,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4E,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x4F,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x50,
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x51
		};
		stringishSubtest(rawbytes, isSymbol, "hello worldAAAAAAAAAAAAAAABAAAAAAAAAAAAAAACAAAAAAAAAAAAAAADAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAFAAAAAAAAAAAAAAAGAAAAAAAAAAAAAAAHAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAJAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAALAAAAAAAAAAAAAAAMAAAAAAAAAAAAAAANAAAAAAAAAAAAAAAOAAAAAAAAAAAAAAAPAAAAAAAAAAAAAAAQ");
		
		rawbytes[0] = (byte)0xB0; // bad type
		try {
			if (isSymbol) {
				this.deser.deserializeSymbol(rawbytes);
			} else {
				this.deser.deserializeString(rawbytes);
			}
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		rawbytes[0] = type;
		rawbytes[1] = 0x7F; // too long
		try {
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		try {
			if (isSymbol) {
				this.deser.deserializeSymbol(rawbytes);
			} else {
				this.deser.deserializeString(rawbytes);
			}
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
		
		rawbytes[1] = 0x00;
		rawbytes[3] = 0x00; // represent short string as long
		stringishSubtest(rawbytes, isSymbol, "hello world");

		rawbytes[4] = 0x00; // empty string
		stringishSubtest(rawbytes, isSymbol, "");
	}
	
	@Test
	public void TypeA1ShortStringTest() {
		shortStringishSubtest((byte)0xA1, false);
	}
	
	@Test
	public void TypeB1LongStringTest() {
		longStringishSubtest((byte)0xB1, false);
	}
	
	@Test
	public void TypeA3ShortSymbolTest() {
		shortStringishSubtest((byte)0xA3, true);
	}
	
	@Test
	public void TypeB3LongSymbolTest() {
		longStringishSubtest((byte)0xB3, true);
	}
	
	@Test
	public void TypeC0ShortListTest() {
		try {
			final byte[] rawbytes = { (byte)0xC0, 0x03, 0x02, 0x41, 0x42 };
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	@Test
	public void TypeC1ShortMapTest() {
		try {
			final byte[] rawbytes = { (byte)0xC1, 0x03, 0x02, 0x41, 0x42 }; // not a valid map
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	@Test
	public void TypeD0LongListTest() {
		try {
			final byte[] rawbytes = { (byte)0xD0, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x02, 0x41, 0x42 };
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	@Test
	public void TypeD1LongMapTest() {
		try {
			final byte[] rawbytes = { (byte)0xD1, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x02, 0x41, 0x42 }; // not a valid map
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	@Test
	public void TypeE0ShortArrayTest() {
		try {
			final byte[] rawbytes = { (byte)0xE0, 0x04, 0x02, 0x51, 0x01, 0x02 };
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
	
	@Test
	public void TypeF0ShortArrayTest() {
		try {
			final byte[] rawbytes = { (byte)0xF0, 0x00, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00, 0x02, 0x51, 0x01, 0x02 };
			this.deser.deserialize(this.topicName, rawbytes);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// OK
		}
	}
}
