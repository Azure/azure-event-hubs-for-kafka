# Using AmqpDeserializer

This class is intended to make interoperation between AMQP- or HTTPS-based event producers and Kafka-based
event consumers easier by providing a way for the consumer code to deserialize the AMQP-encoded header values
on the events.

There are two general approaches to using it:

* calling the deserialize() method and then casting the returned Object to a useful type
* calling one of the more specialized deserialize*() methods which return specific types

To use the deserializer, simply add the java file to your project.

If you modify the deserializer, DeserTests.java is a set of Junit tests which can be used to
verify the behavior.

## deserialize()

```java
    AmqpDeserializer amqpDeser = new AmqpDeserializer();

    // The deserialize() method requires no prior knowledge of a property's type.
    // It returns Object and the application can check the type and perform a cast later.
    Object propertyOfUnknownType = amqpDeser.deserialize("topicname", myHeader.value());
    if (propertyOfUnknownType instanceof Integer) {
        final Integer propertyInt = (Integer)propertyOfUnknownType;
        // do work here
    }
```

As the comment says, deserialize() requires no knowledge of a property's type. Generally, expected headers
will also have an expected type, so this is not a huge advantage, but it can be a good way to convert
unexpected headers to an object which can be meaningfully logged.

## specialized deserializers

These are the specialized deserializer methods provided:

* deserializeDescribedType
* deserializeBoolean
* deserializeUnsignedInteger
* deserializeSignedInteger
* deserializeFloatingPoint
* deserializeCharacter
* deserializeTimestamp
* deserializeUUID
* deserializeSymbol
* deserializeString

They should only be called when the header is expected to be of the given type. For example:

```java
    AmqpDeserializer amqpDeser = new AmqpDeserializer();

    try {
        final long propertyLong = amqpDeser.deserializeSignedInteger(myHeader.value());
        // do work here
    }
    catch (IllegalArgumentException e) {
        // property was not a signed integer
    }
```

### deserializeDescribedType

A "described type" is an AMQP concept which applies additional semantic information to a type. The only
described type supported by this deserializer is a URI, which is a subset of strings.

### deserializeUnsignedInteger

AMQP has many different ways of encoding unsigned integers and this method will work on any of them.
It returns type long because that type can represent the entire range of any of the smaller unsigned types;
since Java does not support unsigned integers explicitly, for unsigned longs all the deserializer can do is
reproduce the bit pattern. It is up to the application to interpret it in an unsigned way.

This behavior differs from calling deserialize(), which returns an unsigned integer as the next larger
integer size.

### deserializeSignedInteger

AMQP also has many different ways of encoding signed integers, and this method will work on any of them.
It always returns type long because that type can represent the entire range of any signed integer type that
Java supports.

This behavior differs from calling deserialize(), which returns a signed integer as the encoded width.

### deserializeFloatingPoint

AMQP can encode 32-bit and 64-bit floating-point numbers. This method works on both and returns type double
because it can represent the entire range of both widths.

This behavior differs from calling deserialize(), which returns floating-point numbers as the encoded width.

### deserializeSymbol

A "symbol" is an AMQP concept. It is a 7-bit ASCII string and normally only used internally in described types.

### deserializeString

AMQP has two ways of encoding UTF-8 strings, one which reduces the byte count but can only represent fairly
short strings, and one which allows for very large strings. This method works on both.

