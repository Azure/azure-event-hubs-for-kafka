//Copyright (c) Microsoft Corporation. All rights reserved.
//Copyright 2016-2017 Confluent Inc., 2015-2016 Andreas Heider
//Licensed under the MIT License.
//Licensed under the Apache License, Version 2.0
//
//Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Confluent.Kafka;
using Confluent.Kafka.Serialization;

namespace EventHubsForKafkaSample
{
    class Worker
    {
        public static async Task Producer(string brokerList, string connStr, string topic, string cacertlocation)
        {
            try
            {
                var config = new Dictionary<string, object> {
                    { "bootstrap.servers", brokerList },
                    { "security.protocol", "SASL_SSL" },
                    { "sasl.mechanism", "PLAIN" },
                    { "sasl.username", "$ConnectionString" },
                    { "sasl.password", connStr },
                    { "ssl.ca.location", cacertlocation },
                    //{ "debug", "security,broker,protocol" }       //Uncomment for librdkafka debugging information
                };

                using (var producer = new Producer<long, string>(config, new LongSerializer(), new StringSerializer(Encoding.UTF8)))
                {
                    Console.WriteLine("Sending 10 messages to topic: " + topic + ", broker(s): " + brokerList);
                    for (int x = 0; x < 10; x++)
                    {
                        var msg = string.Format("Sample message #{0} sent at {1}", x, DateTime.Now.ToString("yyyy-MM-dd_HH:mm:ss.ffff"));
                        var deliveryReport = await producer.ProduceAsync(topic, DateTime.UtcNow.Ticks, msg);
                        Console.WriteLine(string.Format("Message {0} sent (value: '{1}')", x, msg));
                    }
                }
            }
            catch (Exception e)
            {
                Console.WriteLine(string.Format("Exception Occurred - {0}", e.Message));
            }
        }

        public static void Consumer(string brokerList, string connStr, string consumergroup, string topic, string cacertlocation)
        {
            var config = new Dictionary<string, object> {
                    { "bootstrap.servers", brokerList },
                    { "security.protocol","SASL_SSL" },
                    { "sasl.mechanism","PLAIN" },
                    { "sasl.username", "$ConnectionString" },
                    { "sasl.password", connStr },
                    { "ssl.ca.location", cacertlocation },
                    { "group.id", consumergroup },
                    { "request.timeout.ms", 60000 },
                    //{ "auto.offest.reset", "earliest" },          //Uncomment for setting offest value, valid args: [latest, earliest, none]
                    //{ "debug", "security,broker,protocol" }       //Uncomment for librdkafka debugging information
                };

            using (var consumer = new Consumer<long, string>(config, new LongDeserializer(), new StringDeserializer(Encoding.UTF8)))
            {
                consumer.OnMessage += (_, msg)
                  => Console.WriteLine($"Received: '{msg.Value}'");

                consumer.OnError += (_, error)
                  => Console.WriteLine($"Error: {error}");

                consumer.OnConsumeError += (_, msg)
                  => Console.WriteLine($"Consume error ({msg.TopicPartitionOffset}): {msg.Error}");

                Console.WriteLine("Consuming messages from topic: " + topic + ", broker(s): " + brokerList);
                consumer.Subscribe(topic);

                while (true)
                {
                    consumer.Poll(TimeSpan.FromMilliseconds(1000));
                }
            }

        }
    }
}
