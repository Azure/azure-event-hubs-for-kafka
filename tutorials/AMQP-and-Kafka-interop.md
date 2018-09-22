# Exchanging Events Between Consumers and Producers Using Different Protocols

Azure Event Hubs support three protocols for consumers and producers: AMQP, REST (via HTTPS), and Kafka.
Each one of these protocols has its own way of representing a message, so naturally the question arises:
if my system sends events to an Event Hub with one protocol and then consumes them with a different protocol,
what do the various parts and values of the event look like when they arrive at the consumer? This document
discusses best practices on both producer and consumer to ensure that the values within an event are interpreted
correctly by the consuming application.

The advice in this document specifically covers these clients, with the listed versions used in developing
the code snippets:

* Kafka Java client (version 1.1.1 from https://www.mvnrepository.com/artifact/org.apache.kafka/kafka-clients)
* Microsoft Azure Event Hubs Client for Java (version 1.1.0 from https://github.com/Azure/azure-event-hubs-java)
* Microsoft Azure Event Hubs Client for .NET (version 2.1.0 from https://github.com/Azure/azure-event-hubs-dotnet)
* Microsoft Azure Service Bus (version 5.0.0 from https://www.nuget.org/packages/WindowsAzure.ServiceBus)

Other AMQP clients may behave slightly differently. AMQP itself has a well-defined type system, but the specifics of
serializing language-specific types to and from that type system depend on the client, as does how the client provides access
to the parts of an AMQP message.

## Event Body

All of the Microsoft AMQP clients represent the event body as an uninterpreted bag of bytes: a producing application
passes a sequence of bytes to the client, and a consuming application receives that same sequence from the client.
All interpretation of the byte sequence happens within the application code.

It is easy to achieve the same state in a Kafka producer or consumer by using the provided BytesSerializer and BytesDeserializer:

```java
    // Kafka byte producer

    final Properties properties = new Properties();
    // add other properties
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, BytesSerializer.class.getName());

    final KafkaProducer<Long, Bytes> producer = new KafkaProducer<Long, Bytes>(properties);

    final byte[] eventBody = new byte[] { 0x01, 0x02, 0x03, 0x04 };
    Bytes wrapper = Bytes.wrap(eventBody);
    ProducerRecord<Long, Bytes> record = new ProducerRecord<Long, Bytes>(myTopic, myPartitionId, myTimeStamp, wrapper);

    // send record
```

```java
    // Kafka byte consumer

    final Properties properties = new Properties();
    // add other properties
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class.getName());

    final KafkaConsumer<Long, Bytes> consumer = new KafkaConsumer<Long, Bytes>(properties);

    // receive a record as ConsumerRecord<Long, Bytes> record
    // record.value() is of type Bytes
    // record.value().get() is a byte[] with values { 0x01, 0x02, 0x03, 0x04 }
```

This creates a completely transparent byte pipeline between the two halves of the application and allows the
application developer to manually serialize and deserialize in any way desired, including making deserialization
decisions on the fly, for example based on type or sender information in user-set properties on the event.

Applications that have a single, fixed event body type may be able to use other Kafka serializers and 
deserializers to transparently convert data. For example, consider an application which uses JSON. The actual
construction and interpretation of
the JSON string happens at the application level, but at the Event Hubs level the event body will always be a string,
which is to say a sequence of bytes representing characters in the UTF-8 encoding. In this case, the Kafka producer or
consumer can take advantage of the provided StringSerializer or StringDeserializer:

```java
    // Kafka UTF-8 string producer

    final Properties properties = new Properties();
    // add other properties
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    final KafkaProducer<Long, Bytes> producer = new KafkaProducer<Long, Bytes>(properties);

    final String exampleJson = "{\"name\":\"Goku\", \"power level\":9001}";
    ProducerRecord<Long, Bytes> pr = new ProducerRecord<Long, Bytes>(myTopic, myPartitionId, myTimeStamp, exampleJson);

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

    final String exampleJson = "{\"name\":\"Goku\", \"power level\":9001}";
    final EventData ed = EventData.create(exampleJson.getBytes(StandardCharsets.UTF_8));
```

```java
    // Interpret Java AMQP event as string

    EventData ed = /* receive event */
    String receivedJson = new String(ed.getBytes(), StandardCharsets.UTF_8);
```

```csharp
    // Create C# AMQP string event for sending

    string exampleJson = "{\"name\":\"Goku\", \"power level\":9001}";
    EventData working = new EventData(Encoding.UTF8.GetBytes(exampleJson));
```

```csharp
    // Interpret C# AMQP event as string

    EventData ed = /* receive event */

    // getting the event body bytes depends on the client
    byte[] bodyBytes = ed.Body.Array;  // Microsoft Azure Event Hubs Client for .NET
    // byte[] bodyBytes = ed.GetBytes(); // Microsoft Azure Service Bus

    string receivedJson = Encoding.UTF8.GetString(bodyBytes);
```


## Event User Properties


## Event System Properties

Offset
EnqueuedTimeUtc == Timestamp
SequenceNumber == not available
PartitionKey == not available?
