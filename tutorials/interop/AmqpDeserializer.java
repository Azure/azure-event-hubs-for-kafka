package com.microsoft.azure.eventhubs.kafkasupport;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.common.serialization.Deserializer;

/***
 * This class implements a deserializer for bytes encoded with the AMQP type system as described by the AMQP 1.0 standard.
 * For more details of the type system, see http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-types-v1.0-os.html 
 *
 * Because it is intended for use in applications using the Kafka client, it is based on the Deserializer<> interface
 * defined by the Kafka client. However, it is not really intended to handle message bodies. Instead, it is intended to
 * provide a convenient way for the application to deserialize message headers in messages sent by AMQP clients.
 * 
 * The AMQP type system is extensive and complex. Most AMQP clients only implement and support a subset of it. The types
 * that this deserializer supports are based on which types are emitted by Microsoft's .NET and Java Event Hubs clients.
 * Specifically, the types listed below (with the AMQP type code) are supported:
 * 
 * (0x00)	described types -- only "com.microsoft:uri" is supported
 * (0x40)	null
 * (0x73)	UTF32 character
 * (0x83)	AMQP timestamp (64-bit milliseconds since the UNIX epoch)
 * (0x98)	UUID
 * (0x41, 0x42)	boolean true and false
 * (0x72, 0x82)	32- and 64-bit floating point numbers
 * (0xA0, 0xB0)	AMQP binary (byte sequence)
 * (0xA1, 0xB1)	UTF-8 string
 * (0xA3, 0xB3)	AMQP symbol (USASCII string)
 * (0x51, 0x61, 0x71, 0x54, 0x81, 0x55)				signed integers of 8, 16, 32, and 64 bits
 * (0x50, 0x60, 0x70, 0x52, 0x43, 0x80, 0x53, 0x44) unsigned integers of 8, 16, 32, and 64 bits
 * 
 * Other types (various sizes of fixed-point decimal numbers, and lists, maps, and arrays) are not supported.
 */
public class AmqpDeserializer implements Deserializer<Object> {
	private class Result {
		final Object result;
		final int bytesConsumed;
		
		Result(final Object r, final int c) {
			this.result = r;
			this.bytesConsumed = c;
		}
	}

	/***
	 * An AMQP described type consists of a descriptor (an AMQP symbol) and a value. 
	 * This class packs the two together for convenience when returning.
	 */
	public class DescribedType {
		private final String descriptor;
		private final Object value;
		
		/***
		 * The only supported described type has descriptor "com.microsoft:uri"
		 * 
		 * @return the descriptor of the described type
		 */
		public String getDescriptor() {
			return this.descriptor;
		}

		/***
		 * The only supported described type is a URI, which has a string value.
		 * 
		 * @return the value of the described type
		 */
		public Object getValue() {
			return this.value;
		}
		
		DescribedType(final String d, final Object v) {
			this.descriptor = d;
			this.value = v;
		}
	}

	/***
	 * Required by the Deserializer<> interface, but this deserializer has no configurable values.
	 */
	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		// Nothing to do
	}
	
	/***
	 * Deserializes an AMQP described type (0x00).
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  DecribedType encapsulates the descriptor and value which make up a described type.
	 * @throws  IllegalArgumentException if the bytes do not represent an AMQP described type or the described type is malformed or not recognized
	 */
	public DescribedType deserializeDescribedType(final byte[] data) throws IllegalArgumentException {
		return (DescribedType)_deserializeDescribedType(data).result;
	}
	
	private Result _deserializeDescribedType(final byte[] data) throws IllegalArgumentException {
		sanitize(data);
		
		if (data[0] != 0x00) {
			// Not a described type
			throw new IllegalArgumentException(String.format("Bytes are not an AMQP described type, code %02X", data[0]));
		}
		int offset = 1;
		Result symbol = deserializeSymbol(data, offset);
		offset += symbol.bytesConsumed;
		if ((((String)symbol.result).compareTo("com.microsoft:uri")) != 0) {
			throw new IllegalArgumentException("Unrecognized described type " + (String)symbol.result);
		}
		Result uriString = deserializeString(data, offset);
		return new Result(new DescribedType((String)symbol.result, uriString.result), offset + uriString.bytesConsumed);
	}
	
	/***
	 * Deserializes an AMQP boolean (0x41, 0x42).
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  boolean represented by the bytes
	 * @throws  IllegalArgumentException if the bytes do not represent an AMQP boolean
	 */
	public Boolean deserializeBoolean(final byte[] data) throws IllegalArgumentException {
		return (Boolean)_deserializeBoolean(data).result;
	}
	
	private Result _deserializeBoolean(final byte[] data) throws IllegalArgumentException {
		sanitize(data);
		
		Boolean retval = null;
		if (data[0] == 0x41) {
			retval = new Boolean(true);
		}
		else if (data[0] == 0x42) {
			retval = new Boolean(false);
		}
		else {
			throw new IllegalArgumentException(String.format("Bytes are not an AMQP boolean, code %02X", data[0]));
		}
		return new Result(retval, 1);
	}
	
	/***
	 * Deserialize any of the AMQP unsigned integer types. Because Java does not support unsigned, return the
	 * value as a long, which can represent the unsigned range of any of the smaller types. There is nothing
	 * larger than a 64-bit integer, so unsigned long is a matter of interpretation.
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  unsigned long represented by the bytes, zero-extended to 64 bits
	 * @throws  IllegalArgumentException if the bytes do not represent any AMQP unsigned integer type
	 */
	public Long deserializeUnsignedInteger(final byte[] data) throws IllegalArgumentException {
		Result r = _deserializeUnsignedInteger(data);
		
		long unsignedResult = 0L;
		if (r.result instanceof Short) {
			unsignedResult = (Short)r.result;
			unsignedResult &= 0x000000000000FFFFL;
		}
		else if (r.result instanceof Integer) {
			unsignedResult = (Integer)r.result;
			unsignedResult &= 0x00000000FFFFFFFFL;
		}
		else if (r.result instanceof Long) {
			unsignedResult = (Long)r.result;
		}
		else {
			throw new IllegalArgumentException(String.format("Bytes are not an AMQP unsigned integer, code %02X", data[0]));
		}
		
		return new Long(unsignedResult);
	}

	private Result _deserializeUnsignedInteger(final byte[] data) throws IllegalArgumentException {
		sanitize(data);
		
		Result retval = null;
		switch (data[0]) {
			case 0x43:	// zero uint
			case 0x44:	// zero ulong
				retval = new Result(new Long(0L), 1);
				break;
			
			case 0x50:	// ubyte
				retval = deserializeU8(data);
				break;
				
			case 0x52:	// small uint (0-255)
			case 0x53:	// small ulong (0-255)
				retval = deserializeU8(data);
				retval = new Result(new Long((Short)retval.result), retval.bytesConsumed);
				break;
				
			case 0x60:	// ushort
				retval = deserializeU16(data);
				break;
				
			case 0x70:	// uint
				retval = deserializeU32(data);
				break;
				
			case (byte)0x80:	// ulong
				retval = deserializeU64(data);
				break;
				
			default:
				throw new IllegalArgumentException(String.format("Bytes are not an AMQP unsigned integer, code %02X", data[0]));
		}
		
		return retval;
	}
	
	private Result deserializeU8(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 2) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		int dummy = (int)data[1];
		dummy &= 0x000000FF;
		return new Result(new Short((short)dummy), 2);
	}
	
	/***
	 * Deserialize any of the AMQP signed integer types. Return the value as a long, which can represent the
	 * range of any of the smaller types.
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  signed long represented by the bytes, sign-extended to 64 bits
	 * @throws  IllegalArgumentException if the bytes do not represent any AMQP signed integer type
	 */
	public Long deserializeSignedInteger(final byte[] data) throws IllegalArgumentException {
		Result r = _deserializeSignedInteger(data);
		
		long signedResult = 0L;
		if (r.result instanceof Byte) {
			signedResult = (Byte)r.result;
		}
		else if (r.result instanceof Short) {
			signedResult = (Short)r.result;
		}
		else if (r.result instanceof Integer) {
			signedResult = (Integer)r.result;
		}
		else if (r.result instanceof Long) {
			signedResult = (Long)r.result;
		}
		else {
			throw new IllegalArgumentException(String.format("Bytes are not an AMQP signed integer, code %02X", data[0]));
		}
		
		return new Long(signedResult);
	}
	
	private Result _deserializeSignedInteger(final byte[] data) throws IllegalArgumentException {
		sanitize(data);
		
		Result retval = null;
		switch (data[0]) {
			case 0x51:	// byte
				retval = deserializeS8(data);
				break;

			case 0x54:	// small int (-128 to 127)
				retval = deserializeS8(data);
				retval = new Result(new Integer((Byte)retval.result), retval.bytesConsumed);
				break;
				
			case 0x55:	// small long (-128 to 127)
				retval = deserializeS8(data);
				retval = new Result(new Long((Byte)retval.result), retval.bytesConsumed);
				break;
				
			case 0x61:	// short
				retval = deserializeS16(data);
				break;
				
			case 0x71:	// int
				retval = deserializeS32(data);
				break;
				
			case (byte)0x81:	// long
				retval = deserializeS64(data);
				break;
				
			default:
				throw new IllegalArgumentException(String.format("Bytes are not an AMQP signed integer, code %02X", data[0]));
		}
		
		return retval;
	}
	
	private Result deserializeS8(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 2) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		return new Result(new Byte(data[1]), 2);
	}
	
	private int build2(final byte[] data, final int offset) {
		// Assumes caller has checked length
		int dummy = ((int)data[offset] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((int)data[offset + 1] & 0x000000FF);
		return dummy;
	}
	
	private Result deserializeU16(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 3) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		return new Result(new Integer(build2(data, 1)), 3);
	}
	
	private Result deserializeS16(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 3) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		return new Result(new Short((short)build2(data, 1)), 3);
	}
	
	private long build4(final byte[] data, final int offset) {
		// Assumes caller has checked length
		long dummy = ((long)data[offset] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 1] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 2] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 3] & 0x000000FF);
		return dummy;
	}
	
	private Result deserializeU32(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 5) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		return new Result(new Long(build4(data, 1)), 5);
	}
	
	private Result deserializeS32(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 5) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		return new Result(new Integer((int)build4(data, 1)), 5);
	}
	
	
	/***
	 * Deserialize any of the AMQP floating point types. Return the value as a double, which can represent the
	 * range of any of the smaller types.
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  64-bit floating-point value
	 * @throws  IllegalArgumentException if the bytes do not represent any AMQP floating-point type
	 */
	public Double deserializeFloatingPoint(final byte[] data) throws IllegalArgumentException {
		Result r = _deserializeFloatingPoint(data);
		
		double floatingResult = 0.0;
		switch (data[0]) {
			case 0x72:	// float
				floatingResult = (Float)r.result;
				break;
				
			case (byte)0x82:	// double
				floatingResult = (Double)r.result;
				break;
				
			default:
				throw new IllegalArgumentException(String.format("Bytes are not an AMQP floating point number, code %02X", data[0]));
		}
		
		return new Double(floatingResult);
	}
	
	private Result _deserializeFloatingPoint(final byte[] data) throws IllegalArgumentException {
		sanitize(data);
		
		Result retval = null;
		switch (data[0]) {
			case 0x72:	// float
				retval = deserializeFloat(data);
				break;
				
			case (byte)0x82:	// double
				retval = deserializeDouble(data);
				break;
				
			default:
				throw new IllegalArgumentException(String.format("Bytes are not an AMQP floating point number, code %02X", data[0]));
		}
		
		return retval;
	}
	
	private Result deserializeFloat(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 5) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		int intBits = (int)build4(data, 1);
		return new Result(Float.intBitsToFloat(intBits), 5);
	}
	
	/***
	 * Deserializes a single UTF32 character. Java does not support UTF32 directly but has library support
	 * to convert it to (potentially multi-byte sequences of) supported character sets.
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  UTF32 character represented in standard character set, may be multi-byte
	 * @throws  IllegalArgumentException if the bytes do not represent the AMQP character type
	 */
	public char[] deserializeCharacter(final byte[] data) throws IllegalArgumentException {
		return (char[])_deserializeCharacter(data).result;
	}
	
	private Result _deserializeCharacter(final byte[] data) throws IllegalArgumentException {
		sanitize(data);
		
		if ((data[0] != 0x73) || (data.length < 5)) {
			// Not a UTF32 character
			throw new IllegalArgumentException(String.format("Bytes are not a UTF32 character or data buffer too short, code %02X", data[0]));
		}
		int intBits = (int)build4(data, 1);
		return new Result(Character.toChars(intBits), 5);
	}

	private long build8(final byte[] data, final int offset) {
		// Assumes caller has checked length
		long dummy = ((long)data[offset] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 1] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 2] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 3] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 4] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 5] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 6] & 0x000000FF);
		dummy <<= 8;
		dummy |= ((long)data[offset + 7] & 0x000000FF);
		return dummy;
	}
	
	private Result deserializeU64(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 9) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		return new Result(new Long(build8(data, 1)), 9);
	}

	private Result deserializeS64(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 9) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		return new Result(new Long(build8(data, 1)), 9);
	}
	
	private Result deserializeDouble(final byte[] data) throws IllegalArgumentException {
		// Assumes caller has checked type byte
		if (data.length < 9) {
			throw new IllegalArgumentException("Data buffer too short");
		}
		long longBits = build8(data, 1);
		return new Result(Double.longBitsToDouble(longBits), 9);
	}

	/***
	 * Deserializes an AMQP timestamp as a Java Instant in UTC timezone.
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  Instant representing the AMQP timestamp
	 * @throws  IllegalArgumentException if the bytes do not represent the AMQP timestamp type
	 */
	public Instant deserializeTimestamp(final byte[] data) throws IllegalArgumentException {
		return (Instant)_deserializeTimestamp(data).result;
	}
	
	private Result _deserializeTimestamp(final byte[] data) throws IllegalArgumentException {
		sanitize(data);
		
		if ((data[0] != (byte)0x83) || (data.length < 9)) {
			// Not a timestamp
			throw new IllegalArgumentException(String.format("Bytes are not an AMQP timestamp or data buffer too short, code %02X", data[0]));
		}
		return new Result(Instant.ofEpochMilli(build8(data, 1)), 9);
	}
	
	/***
	 * Deserializes a UUID.
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  UUID
	 * @throws  IllegalArgumentException if the bytes do not represent the AMQP UUID type
	 */
	public UUID deserializeUUID(final byte[] data) throws IllegalArgumentException {
		return (UUID)_deserializeUUID(data).result;
	}
	
	private Result _deserializeUUID(final byte[] data) throws IllegalArgumentException {
		sanitize(data);
		
		if ((data[0] != (byte)0x98) || (data.length < 17)) {
			// Not a UUID
			throw new IllegalArgumentException(String.format("Bytes are not an AMQP UUID or data buffer too short, code %02X", data[0]));
		}
		final long mostSig = build8(data, 1);
		final long leastSig = build8(data, 9);
		return new Result(new UUID(mostSig, leastSig), 17);
	}

	/***
	 * Deserializes an AMQP symbol as a Java string.
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  String
	 * @throws	IllegalArgumentException if the bytes do not represent any AMQP symbol type
	 */
	public String deserializeSymbol(final byte[] data) throws IllegalArgumentException {
		return (String)deserializeSymbol(data, 0).result;
	}
	
	private Result deserializeSymbol(final byte[] data, final int startingOffset) throws IllegalArgumentException {
		sanitize(data);
		
		if (data[startingOffset] == (byte)0xA3) {
			return deserializeShortString(data, startingOffset, StandardCharsets.US_ASCII);
		} else if (data[startingOffset] == (byte)0xB3) {
			return deserializeLongString(data, startingOffset, StandardCharsets.US_ASCII);
		}
		throw new IllegalArgumentException(String.format("Bytes are not an AMQP symbol, code %02X", data[startingOffset]));
	}

	/***
	 * Deserializes an AMQP string as a Java UTF-8 string.
	 * 
	 * @param data  AMQP-encoded bytes
	 * @return  String
	 * @throws  IllegalArgumentException if the bytes do not represent any AMQP string type
	 */
	public String deserializeString(final byte[] data) throws IllegalArgumentException {
		return (String)deserializeString(data, 0).result;
	}
	
	private Result deserializeString(final byte[] data, final int startingOffset) throws IllegalArgumentException {
		sanitize(data);
		
		if (data[startingOffset] == (byte)0xA1) {
			return deserializeShortString(data, startingOffset, StandardCharsets.UTF_8);
		} else if (data[startingOffset] == (byte)0xB1) {
			return deserializeLongString(data, startingOffset, StandardCharsets.UTF_8);
		}
		throw new IllegalArgumentException(String.format("Bytes are not an AMQP string, code %02X", data[startingOffset]));
	}
	
	private Result deserializeShortString(final byte[] data, final int startingOffset, Charset encoding) throws IllegalArgumentException {
		final int encodedLength = ((int)data[startingOffset + 1] & 0x000000FF);
		if ((startingOffset + 2 + encodedLength) > data.length) {
			// Malformed
			throw new IllegalArgumentException("Expected string length does not match actual data length");
		}
		return new Result(new String(Arrays.copyOfRange(data, startingOffset + 2, startingOffset + 2 + encodedLength), encoding), encodedLength + 2);
	}
	
	private Result deserializeLongString(final byte[] data, final int startingOffset, Charset encoding) throws IllegalArgumentException {
		final long encodedLength = build4(data, startingOffset + 1);
		if ((startingOffset + 5 + encodedLength) > data.length) {
			// Malformed
			throw new IllegalArgumentException("Expected string length does not match actual data length");
		}
		return new Result(new String(Arrays.copyOfRange(data, startingOffset + 5, (int)(startingOffset + 5 + encodedLength)), encoding),
				(int)encodedLength + 5);
	}
	
	/***
	 * Deserialization method from the interface. Automatically deserializes any type based on the
	 * AMQP encoding. The downside is that since it can deserialize any type, it can only return Object
	 * and the caller must determine the type and perform a cast in order to make use of it.
	 * 
	 *  @param topic  Topic name. Required by the interface but not used.
	 *  @param data   AMQP-encoded bytes
	 *  @return  deserialized object
	 *  @throws  IllegalArgumentException if the bytes represent a supported AMQP type but are malformed
	 *  @throws  UnsupportedOperationException if the bytes represent a known but unsupported AMQP type, or an unknown type
	 */
	@Override
	public Object deserialize(String topic, byte[] data) throws IllegalArgumentException, UnsupportedOperationException {
		return deserialize(data).result;
	}
	
	private Result deserialize(byte[] data) throws IllegalArgumentException, UnsupportedOperationException {
		sanitize(data);

		Result retval = null;
		switch (data[0]) {
			case 0x00:
				retval = _deserializeDescribedType(data);
				break;
				
			case 0x40:
				// AMQP NULL
				retval = new Result(null, 1);
				break;
				
			case 0x41:
				// AMQP boolean TRUE
			case 0x42:
				// AMQP boolean FALSE
				retval = _deserializeBoolean(data);
				break;
				
			case 0x43:
				// AMQP zero uint
			case 0x44:
				// AMQP zero ulong
			case 0x50:
				// AMQP ubyte
			case 0x52:
				// AMQP small uint
			case 0x53:
				// AMQP small ulong
			case 0x60:
				// AMQP ushort
			case 0x70:
				// AMQP uint
			case (byte)0x80:
				// AMQP ulong
				retval = _deserializeUnsignedInteger(data);
				break;
				
			// case 0x45:
				// AMQP empty list

			case 0x51:
				// AMQP byte
				retval = deserializeS8(data);
			case 0x54:
				// AMQP small int
			case 0x55:
				// AMQP small long
			case 0x61:
				// AMQP short
			case 0x71:
				// AMQP int
			case (byte)0x81:
				// AMQP long
				retval = _deserializeSignedInteger(data);
				break;
				
			case 0x72:
				// AMQP float
			case (byte)0x82:
				// AMQP double
				retval = _deserializeFloatingPoint(data); 
				break;
				
			case 0x73:
				// AMQP UTF32 char
				retval = _deserializeCharacter(data);
				break;
				
			//case 0x74:
				// AMQP decimal32

			case (byte)0x83:
				// AMQP timestamp is milliseconds since Unix epoch, which Java has a standard class for
				retval = _deserializeTimestamp(data);
				break;
				
			//case (byte)0x84:
				// AMQP decimal64
			
			//case (byte)0x94:
				// AMQP decimal128
			
			case (byte)0x98:
				// AMQP UUID
				retval = _deserializeUUID(data);
				break;
				
			case (byte)0xA0:
				// AMQP short binary
				// Return as byte array
				{
					final int encodedLength = ((int)data[1] & 0x000000FF);
					if (encodedLength > (data.length - 2)) {
						// Malformed
						throw new IllegalArgumentException("Expected binary length does not match actual data length");
					}
					retval = new Result(Arrays.copyOfRange(data, 2, 2 + encodedLength), encodedLength + 2);
				}
				break;
				
			case (byte)0xA1:
				// AMQP short string, UTF8
			case (byte)0xB1:
				// AMQP long string, UTF8
				retval = deserializeString(data, 0);
				break;
				
			case (byte)0xA3:
				// AMQP short symbol, ASCII
			case (byte)0xB3:
				// AMQP long symbol, ASCII
				retval = deserializeSymbol(data, 0);
				break;
				
			case (byte)0xB0:
				// AMQP long binary
				// Return as byte array
				{
					final long encodedLength = build4(data, 1);
					if (encodedLength > (data.length - 5)) {
						// Malformed
						throw new IllegalArgumentException("Expected binary length does not match actual data length");
					}
					retval = new Result(Arrays.copyOfRange(data, 5, (int)(5 + encodedLength)), (int)encodedLength + 5);
				}
				break;
				
			//case (byte)0xC0:
				// AMQP short list
			
			//case (byte)0xC1:
				// AMQP short map
			
			//case (byte)0xD0:
				// AMQP long list
			
			//case (byte)0xD1:
				// AMQP long map
			
			//case (byte)0xE0:
				// AMQP short array
			
			//case (byte)0xF0:
				// AMQP long array
				
			default:
				throw new UnsupportedOperationException(String.format("Deserialization not implemented for AMQP type %02X", data[0]));
		}
		
		return retval;
	}
	
	private void sanitize(final byte[] data) {
		if ((data == null) || (data.length <= 0)) {
			throw new IllegalArgumentException("Data argument must not be null or zero length");
		}
	}

	/***
	 * Required by the Deserializer<> interface, but this deserializer has nothing to do on close.
	 */
	@Override
	public void close() {
		// Nothing to do
	}
}
