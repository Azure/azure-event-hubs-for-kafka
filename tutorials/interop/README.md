# Exchanging Events Between Consumers and Producers Using Different Protocols

Azure Event Hubs support three protocols for consumers and producers: AMQP, Kafka, and HTTPS.
Each one of these protocols has its own way of representing a message, so naturally the question arises:
if an application sends events to an Event Hub with one protocol and then consumes them with a different protocol,
what do the various parts and values of the event look like when they arrive at the consumer? This document
discusses best practices on both producer and consumer to ensure that the values within an event are interpreted
correctly by the consuming application.

The advice in this document specifically covers these clients, with the listed versions used in developing
the code snippets:

* Kafka Java client (version 1.1.1 from https://www.mvnrepository.com/artifact/org.apache.kafka/kafka-clients)
* Microsoft Azure Event Hubs Client for Java (version 1.1.0 from https://github.com/Azure/azure-event-hubs-java)
* Microsoft Azure Event Hubs Client for .NET (version 2.1.0 from https://github.com/Azure/azure-event-hubs-dotnet)
* Microsoft Azure Service Bus (version 5.0.0 from https://www.nuget.org/packages/WindowsAzure.ServiceBus)
* HTTPS (supports producers only)

Other AMQP clients may behave slightly differently. AMQP has a well-defined type system, but the specifics of
serializing language-specific types to and from that type system depend on the client, as does how the client provides access
to the parts of an AMQP message.

## Event Body

All of the Microsoft AMQP clients represent the event body as an uninterpreted bag of bytes: a producing application
passes a sequence of bytes to the client, and a consuming application receives that same sequence from the client.
All interpretation of the byte sequence happens within the application code.

When sending an event via HTTPS, the event body is the POSTed content, which is also treated as uninterpreted
bytes.

It is easy to achieve the same state in a Kafka producer or consumer by using the provided ByteArraySerializer
and ByteArrayDeserializer:

### Kafka byte[] producer
```java
    final Properties properties = new Properties();
    // add other properties
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

    final KafkaProducer<byte[], byte[]> producer = new KafkaProducer<byte[], byte[]>(properties);

    final byte[] eventBody = new byte[] { 0x01, 0x02, 0x03, 0x04 };
    ProducerRecord<byte[], byte[]> pr =
        new ProducerRecord<byte[], byte[]>(myTopic, myPartitionId, myTimeStamp, eventBody);

    /* send pr */
```

### Kafka byte[] consumer
```java
    final Properties properties = new Properties();
    // add other properties
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

    final KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(properties);

    ConsumerRecord<byte[], byte[]> cr = /* receive event */
    // cr.value() is a byte[] with values { 0x01, 0x02, 0x03, 0x04 }
```

This creates a completely transparent byte pipeline between the two halves of the application and allows the
application developer to manually serialize and deserialize in any way desired, including making deserialization
decisions on the fly, for example based on type or sender information in user-set properties on the event.

Applications that have a single, fixed event body type may be able to use other Kafka serializers and 
deserializers to transparently convert data. For example, consider an application which uses JSON. The
construction and interpretation of the JSON string happens at the application level, but at the Event Hubs
level the event body will always be a string, which is to say a sequence of bytes representing characters
in the UTF-8 encoding. In this case, the Kafka producer or consumer can take advantage of the provided
StringSerializer or StringDeserializer:

### Kafka UTF-8 string producer
```java
    final Properties properties = new Properties();
    // add other properties
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    final KafkaProducer<Long, String> producer = new KafkaProducer<Long, String>(properties);

    final String exampleJson = "{\"name\":\"John\", \"number\":9001}";
    ProducerRecord<Long, String> pr =
        new ProducerRecord<Long, String>(myTopic, myPartitionId, myTimeStamp, exampleJson);

    /* send pr */
```

### Kafka UTF-8 string consumer
```java
    final Properties properties = new Properties();
    // add other properties
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    final KafkaConsumer<Long, String> consumer = new KafkaConsumer<Long, String>(properties);

    ConsumerRecord<Long, Bytes> cr = /* receive event */
    final String receivedJson = cr.value();
```

For the AMQP side, both Java and .NET provide built-in ways to convert strings to and from UTF-8 byte sequences.
The Microsoft AMQP clients represent events as a class named EventData, and these examples show how to serialize
a UTF-8 string into an EventData event body in an AMQP producer, and how to deserialize an EventData event body
into a UTF-8 string in an AMQP consumer.

### Java AMQP UTF-8 string producer
```java
    final String exampleJson = "{\"name\":\"John\", \"number\":9001}";
    final EventData ed = EventData.create(exampleJson.getBytes(StandardCharsets.UTF_8));
```

### Java AMQP UTF-8 string consumer
```java
    EventData ed = /* receive event */
    String receivedJson = new String(ed.getBytes(), StandardCharsets.UTF_8);
```

### C# .NET UTF-8 string producer
```csharp
    string exampleJson = "{\"name\":\"John\", \"number\":9001}";
    EventData working = new EventData(Encoding.UTF8.GetBytes(exampleJson));
```

### C# .NET UTF-8 string consumer
```csharp
    EventData ed = /* receive event */

    // getting the event body bytes depends on which .NET client is used
    byte[] bodyBytes = ed.Body.Array;  // Microsoft Azure Event Hubs Client for .NET
    // byte[] bodyBytes = ed.GetBytes(); // Microsoft Azure Service Bus

    string receivedJson = Encoding.UTF8.GetString(bodyBytes);
```

Because Kafka is open-source, the application developer can inspect the implementation of any serializer
or deserializer and implement code which produces or consumes a compatible sequence of bytes on the AMQP side.


## Event User Properties

User-set properties can be set and retrieved from both AMQP clients
(in the Microsoft AMQP clients they are called properties) and Kafka (where they are called headers). HTTPS
senders can set user properties on an event by supplying them as HTTP headers in the POST operation.
However, Kafka treats both event bodies and event header values as byte sequences, whereas in AMQP clients
property values have types, which are communicated by encoding the property values according to the AMQP
type system.
HTTPS is a special case: at the point of
sending, all property values are UTF-8 text. The Event Hubs service does a limited amount of interpretation
to convert appropriate property values to AMQP-encoded 32- and 64-bit signed integers, 64-bit floating point
numbers, and booleans; any property value which does not fit one of those types is treated as a string.

Mixing these approaches to property typing means that a Kafka consumer sees the raw AMQP-encoded byte sequence,
including the AMQP type information, whereas an AMQP consumer sees the untyped byte sequence sent by the Kafka
producer, which the application must interpret.

To ease the situation for Kafka consumers receiving properties from AMQP or HTTPS producers, we have created the
AmqpDeserializer class, modeled after the other deserializers in the Kafka ecosystem, which interprets the
type information in the AMQP-encoded byte sequences to deserialize the data bytes into a Java type.

As a best practice, we recommend including a property in messages sent via AMQP or HTTPS which the Kafka
consumer can use to determine whether header values need AMQP deserialization. The value of the property is
not important; it just needs a well-known name that the Kafka consumer can find in the list of headers and
adjust its behavior accordingly.

> [!NOTE]
> The Event Hubs service natively converts some of the EventHubs specific [AmqpMessage properties](http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#type-properties) to [Kafka’s record headers](https://kafka.apache.org/32/javadoc/org/apache/kafka/common/header/Headers.html) as **strings**. Kafka message header is a list of &lt;key, value&gt; pairs where key is string and value is always a byte array. For these supported properties, the byte array will have an UTF8encoded string. 
>
> Here is the list of immutable properties that Event Hubs support in this conversion today. If you set values for user properties with the names in this list, you don’t need to deserialize at the Kafka consumer side.
> 
> - message-id
> - user-id
> - to
> - reply-to
> - content-type
> - content-encoding
> - creation-time

### AMQP to Kafka part 1: create and send an event in C# (.NET) with properties
```csharp
    // Create an event with properties "MyStringProperty" and "MyIntegerProperty"
    EventData working = new EventData(Encoding.UTF8.GetBytes("an event body"));
    working.Properties.Add("MyStringProperty", "hello");
    working.Properties.Add("MyIntegerProperty", 1234);

    // BEST PRACTICE: include a property which indicates that properties will need AMQP deserialization
    working.Properties.Add("AMQPheaders", 0);

    /* send working */
```

### AMQP to Kafka part 2: use AmqpDeserializer to deserialize those properties in a Kafka consumer
```java
    final AmqpDeserializer amqpDeser = new AmqpDeserializer();

    ConsumerRecord<Long, Bytes> cr = /* receive event */
    final Header[] headers = cr.headers().toArray();

    final Header headerNamedMyStringProperty = /* find header with key "MyStringProperty" */
    final Header headerNamedMyIntegerProperty = /* find header with key "MyIntegerProperty" */
    final Header headerNamedAMQPheaders = /* find header with key "AMQPheaders", or null if not found */

    // BEST PRACTICE: detect whether AMQP deserialization is needed
    if (headerNamedAMQPheaders != null) {
        // The deserialize() method requires no prior knowledge of a property's type.
        // It returns Object and the application can check the type and perform a cast later.
        Object propertyOfUnknownType = amqpDeser.deserialize("topicname", headerNamedMyStringProperty.value());
        if (propertyOfUnknownType instanceof String) {
            final String propertyString = (String)propertyOfUnknownType;
            // do work here
        }
        propertyOfUnknownType = amqpDeser.deserialize("topicname", headerNamedMyIntegerProperty.value());
        if (propertyOfUnknownType instanceof Integer) {
            final Integer propertyInt = (Integer)propertyOfUnknownType;
            // do work here
        }
    } else {
        /* event sent via Kafka, interpret header values the Kafka way */
    }
```

If the application knows the expected type for a property, there are deserialization methods
which do not require a cast afterwards, but will throw if the property is not of the type expected.

### AMQP to Kafka part 3: a different way of using AmqpDeserializer in a Kafka consumer
```java
    // BEST PRACTICE: detect whether AMQP deserialization is needed
    if (headerNamedAMQPheaders != null) {
        // Property "MyStringProperty" is expected to be of type string.
        try {
            final String propertyString = amqpDeser.deserializeString(headerNamedMyStringProperty.value());
            // do work here
        }
        catch (IllegalArgumentException e) {
            // property was not a string
        }

        // Property "MyIntegerProperty" is expected to be a signed integer type.
        // The method returns long because long can represent the value range of all AMQP signed integer types.
        try {
            final long propertyLong = amqpDeser.deserializeSignedInteger(headerNamedMyIntegerProperty.value());
            // do work here
        }
        catch (IllegalArgumentException e) {
            // property was not a signed integer
        }
    } else {
        /* event sent via Kafka, interpret header values the Kafka way */
    }
```

Going the other direction is more involved, because headers set by a Kafka producer will always be seen by
an AMQP consumer as raw bytes (type org.apache.qpid.proton.amqp.Binary for the Microsoft Azure Event Hubs
Client for Java, or System.Byte[] for Microsoft's .NET AMQP clients). The easiest path is to use one of the
Kafka-supplied serializers to generate the bytes for the header values on the Kafka producer side, and then
write compatible deserialization code on the AMQP consumer side, which is very simple.

As with AMQP-to-Kafka, we recommend as a best practice including a property in messages sent via Kafka which the
AMQP consumer can use to determine whether header values need deserialization. The value of the property is
not important; it just needs a well-known name that the AMQP consumer can find in the list of headers and
adjust its behavior accordingly. If the Kafka producer cannot be changed, it is also possible for the consuming
application to check if the the property value is of a binary or byte type and attempt deserialization based
on that.

### Kafka to AMQP part 1: create and send an event from Kafka with properties
```java
    final String topicName = /* topic name */
    final ProducerRecord<Long, String> pr = new ProducerRecord<Long, String>(topicName, /* other arguments */);
    final Headers h = pr.headers();

    // Set headers using Kafka serializers
    IntegerSerializer intSer = new IntegerSerializer();
    h.add("MyIntegerProperty", intSer.serialize(topicName, 1234));

    LongSerializer longSer = new LongSerializer();
    h.add("MyLongProperty", longSer.serialize(topicName, 5555555555L));

    ShortSerializer shortSer = new ShortSerializer();
    h.add("MyShortProperty", shortSer.serialize(topicName, (short)22222));

    FloatSerializer floatSer = new FloatSerializer();
    h.add("MyFloatProperty", floatSer.serialize(topicName, 1.125F));

    DoubleSerializer doubleSer = new DoubleSerializer();
    h.add("MyDoubleProperty", doubleSer.serialize(topicName, Double.MAX_VALUE));

    StringSerializer stringSer = new StringSerializer();
    h.add("MyStringProperty", stringSer.serialize(topicName, "hello world"));

    // BEST PRACTICE: include a property which indicates that properties will need deserialization
    h.add("RawHeaders", intSer.serialize(0));

    /* send pr */
```

### Kafka to AMQP part 2: manually deserialize those properties in C# (.NET)
```csharp
    EventData ed = /* receive event */

    // BEST PRACTICE: detect whether manual deserialization is needed
    if (ed.Properties.ContainsKey("RawHeaders"))
    {
        // Kafka serializers send bytes in big-endian order, whereas .NET on x86/x64 is little-endian.
        // Therefore it is frequently necessary to reverse the bytes before further deserialization.

        byte[] rawbytes = ed.Properties["MyIntegerProperty"] as System.Byte[];
        if (BitConverter.IsLittleEndian)
        {
             Array.Reverse(rawbytes);
        }
        int myIntegerProperty = BitConverter.ToInt32(rawbytes, 0);

        rawbytes = ed.Properties["MyLongProperty"] as System.Byte[];
        if (BitConverter.IsLittleEndian)
        {
             Array.Reverse(rawbytes);
        }
        long myLongProperty = BitConverter.ToInt64(rawbytes, 0);

        rawbytes = ed.Properties["MyShortProperty"] as System.Byte[];
        if (BitConverter.IsLittleEndian)
        {
             Array.Reverse(rawbytes);
        }
        short myShortProperty = BitConverter.ToInt16(rawbytes, 0);

        rawbytes = ed.Properties["MyFloatProperty"] as System.Byte[];
        if (BitConverter.IsLittleEndian)
        {
             Array.Reverse(rawbytes);
        }
        float myFloatProperty = BitConverter.ToSingle(rawbytes, 0);

        rawbytes = ed.Properties["MyDoubleProperty"] as System.Byte[];
        if (BitConverter.IsLittleEndian)
        {
             Array.Reverse(rawbytes);
        }
        double myDoubleProperty = BitConverter.ToDouble(rawbytes, 0);

        rawbytes = ed.Properties["MyStringProperty"] as System.Byte[];
	string myStringProperty = Encoding.UTF8.GetString(rawbytes);
    }
```

### Kafka to AMQP part 3: manually deserialize those properties in Java
```java
    final EventData ed = /* receive event */

    // BEST PRACTICE: detect whether manual deserialization is needed
    if (ed.getProperties().containsKey("RawHeaders")) {
        byte[] rawbytes =
            ((org.apache.qpid.proton.amqp.Binary)ed.getProperties().get("MyIntegerProperty")).getArray();
        int myIntegerProperty = 0;
        for (byte b : rawbytes) {
            myIntegerProperty <<= 8;
            myIntegerProperty |= ((int)b & 0x00FF);
        }

        rawbytes = ((org.apache.qpid.proton.amqp.Binary)ed.getProperties().get("MyLongProperty")).getArray();
        long myLongProperty = 0;
        for (byte b : rawbytes) {
            myLongProperty <<= 8;
            myLongProperty |= ((long)b & 0x00FF);
        }

        rawbytes = ((org.apache.qpid.proton.amqp.Binary)ed.getProperties().get("MyShortProperty")).getArray();
        short myShortProperty = (short)rawbytes[0];
        myShortProperty <<= 8;
        myShortProperty |= ((short)rawbytes[1] & 0x00FF);

        rawbytes = ((org.apache.qpid.proton.amqp.Binary)ed.getProperties().get("MyFloatProperty")).getArray();
        int intbits = 0;
        for (byte b : rawbytes) {
            intbits <<= 8;
            intbits |= ((int)b & 0x00FF);
        }
        float myFloatProperty = Float.intBitsToFloat(intbits);

        rawbytes = ((org.apache.qpid.proton.amqp.Binary)ed.getProperties().get("MyDoubleProperty")).getArray();
        long longbits = 0;
        for (byte b : rawbytes) {
            longbits <<= 8;
            longbits |= ((long)b & 0x00FF);
        }
        double myDoubleProperty = Double.longBitsToDouble(longbits);

        rawbytes = ((org.apache.qpid.proton.amqp.Binary)ed.getProperties().get("MyStringProperty")).getArray();
	String myStringProperty = new String(rawbytes, StandardCharsets.UTF_8);
    }
```
