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

It is easy to achieve the same state in a Kafka producer or consumer by using the provided BytesSerializer
and BytesDeserializer:

```java
    // Kafka byte producer

    final Properties properties = new Properties();
    // add other properties
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, BytesSerializer.class.getName());

    final KafkaProducer<Long, Bytes> producer = new KafkaProducer<Long, Bytes>(properties);

    final byte[] eventBody = new byte[] { 0x01, 0x02, 0x03, 0x04 };
    Bytes wrapper = Bytes.wrap(eventBody);
    ProducerRecord<Long, Bytes> pr =
        new ProducerRecord<Long, Bytes>(myTopic, myPartitionId, myTimeStamp, wrapper);

    /* send pr */
```

```java
    // Kafka byte consumer

    final Properties properties = new Properties();
    // add other properties
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class.getName());

    final KafkaConsumer<Long, Bytes> consumer = new KafkaConsumer<Long, Bytes>(properties);

    ConsumerRecord<Long, Bytes> cr = /* receive event */
    // cr.value() is of type Bytes
    // cr.value().get() is a byte[] with values { 0x01, 0x02, 0x03, 0x04 }
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

```java
    // Kafka UTF-8 string producer

    final Properties properties = new Properties();
    // add other properties
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    final KafkaProducer<Long, Bytes> producer = new KafkaProducer<Long, Bytes>(properties);

    final String exampleJson = "{\"name\":\"John\", \"number\":9001}";
    ProducerRecord<Long, Bytes> pr =
        new ProducerRecord<Long, Bytes>(myTopic, myPartitionId, myTimeStamp, exampleJson);

    /* send pr */
```

```java
    // Kafka UTF-8 string consumer

    final Properties properties = new Properties();
    // add other properties
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    final KafkaConsumer<Long, Bytes> consumer = new KafkaConsumer<Long, Bytes>(properties);

    ConsumerRecord<Long, Bytes> cr = /* receive event */
    final String receivedJson = cr.value();
```

For the AMQP side, both Java and .NET provide built-in ways to convert strings to and from UTF-8 byte sequences:

```java
    // Create Java AMQP string event for sending

    final String exampleJson = "{\"name\":\"John\", \"number\":9001}";
    final EventData ed = EventData.create(exampleJson.getBytes(StandardCharsets.UTF_8));
```

```java
    // Interpret Java AMQP event as string

    EventData ed = /* receive event */
    String receivedJson = new String(ed.getBytes(), StandardCharsets.UTF_8);
```

```csharp
    // Create .NET AMQP string event for sending

    string exampleJson = "{\"name\":\"John\", \"number\":9001}";
    EventData working = new EventData(Encoding.UTF8.GetBytes(exampleJson));
```

```csharp
    // Interpret .NET AMQP event as string

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
However, unlike event bodies, which both sides agree are byte sequences, property values have types for the
AMQP clients, whereas they are also just byte sequences in Kafka. HTTPS is a special case: at the point of
sending, all property values are UTF-8 text. The Event Hubs service does a limited amount of interpretation
to convert appropriate property values to AMQP-encoded 32- and 64-bit signed integers, 64-bit floating point
numbers, and booleans; any property value which does not fit one of those types is treated as a string.

Mixing the two approaches to property typing means that a Kafka consumer sees the raw AMQP byte sequence,
including the AMQP type information, and that an AMQP consumer sees the untyped byte sequence sent by the Kafka
producer, which the application must interpret.

To ease the situation for Kafka consumers receiving properties from AMQP or HTTPS producers, we have created the
AmqpDeserializer class, modeled after the other deserializers in the Kafka ecosystem, which interprets the
type information in the AMQP byte sequences to deserialize the data bytes into a Java type.

```java
    final AmqpDeserializer amqpDeser = new AmqpDeserializer();

    ConsumerRecord<Long, Bytes> cr = /* receive event */
    final Header[] headers = cr.headers().toArray();

    final Header headerNamedMyStringProperty = /* find header with key "MyStringProperty" */
    final Header headerNamedMyIntegerProperty = /* find header with key "MyIntegerProperty" */

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

    // If the application knows the expected type for a property, there are methods which do not require
    // a cast, but will throw if the property is not of the type expected.
    try {
        final String propertyString = amqpDeser.deserializeString(headerNamedMyStringProperty.value());
        // do work here
    }
    catch (IllegalArgumentException e) {
        // property was not a string
    }
    try {
        final long propertyLong = amqpDeser.deserializeSignedInteger(headerNamedMyIntegerProperty.value());
    }
    catch (IllegalArgumentException e) {
        // property was not a signed integer
    }
```




## Event System Properties

Offset
EnqueuedTimeUtc == Timestamp
SequenceNumber == not available
PartitionKey == not available?
