using System;
using System.Threading;
using System.Threading.Tasks;
using Confluent.Kafka;
using Newtonsoft.Json;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.ApplicationInsights;
using Microsoft.ApplicationInsights.DataContracts;
using System.Configuration;

namespace ConsumerLaglibrdkafka
{
    public class Program
    {
        public static TelemetryClient telemetryClient;
        public static void Main(string[] args)
        {
            CreateHostBuilder(args).Build().Run();
            Console.ReadLine();
        }

        public static async Task ConsumeMessages(int consumerId, int delayInMilliseconds)
        {
            string brokerList = ConfigurationManager.AppSettings["EH_FQDN"];
            string connectionString = ConfigurationManager.AppSettings["EH_CONNECTION_STRING"];
            string topic = ConfigurationManager.AppSettings["EH_NAME"];
            string caCertLocation = ConfigurationManager.AppSettings["CA_CERT_LOCATION"];
            string consumerGroup = ConfigurationManager.AppSettings["CONSUMER_GROUP"];

            var conf = new ConsumerConfig
            {
                BootstrapServers = brokerList,
                SecurityProtocol = SecurityProtocol.SaslSsl,
                SocketTimeoutMs = 60000,               
                SessionTimeoutMs = 30000,
                SaslMechanism = SaslMechanism.Plain,
                SaslUsername = "$ConnectionString",
                SaslPassword = connectionString,
                SslCaLocation = caCertLocation,
                GroupId = consumerGroup,
                AutoOffsetReset = AutoOffsetReset.Earliest,
                // librdkafka statistics emit interval. Required to be configured for capturing the librdkafka statistics
                StatisticsIntervalMs = 1000
            };

            // Set the StatisticsHandler to read and process the statistics from the librdkafka
            using (var c = new ConsumerBuilder<long, string>(conf).SetKeyDeserializer(Deserializers.Int64).SetValueDeserializer(Deserializers.Utf8).SetErrorHandler((_, e) => Console.WriteLine($"Error: {e.Reason}")).SetStatisticsHandler((_, json) =>
            {
                // Deserialize the json content
                var statistics = JsonConvert.DeserializeObject<ConsumerStatistics>(json);

                foreach (var topic in statistics.Topics)
                {
                    // Getting the consumer lag value from all the partitions
                    // You might see a partition id -1. This is for for internal UnAssigned (UA) partition according to the doc here: https://github.com/edenhill/librdkafka/blob/master/STATISTICS.md
                    foreach (var partition in topic.Value.Partitions)
                    {
                        var consumer_lag = new MetricTelemetry();
                        consumer_lag.Name = "consumer lag for " + partition.Key;
                        consumer_lag.Sum = partition.Value.ConsumerLag;
                        telemetryClient.TrackMetric(consumer_lag);

                        Console.WriteLine($"Consumer Lag value sent for topic = {topic.Key}, partition = {partition.Key} => consumerLag = {partition.Value.ConsumerLag}");
                    }

                    // Fetch the metrics from the broker
                    // Getting the Round Trip Time from the broker 
                    foreach (var broker in statistics.Brokers)
                    {
                        var roundTripTimeAvg = new MetricTelemetry();
                        roundTripTimeAvg.Name = "Round Trip Time for " + broker.Key;
                        roundTripTimeAvg.Sum = broker.Value.Rtt.avg;
                        telemetryClient.TrackMetric(roundTripTimeAvg);
                        Console.WriteLine($"Avg Round Trip Time value sent for broker = {broker.Key} => RoundTripTime = {roundTripTimeAvg.Sum}");
                    }
                }
            }
            ).Build())
            {
                
                c.Subscribe(topic);

                bool consuming = true;

                while (consuming)
                {
                    try
                    {
                        var cr = c.Consume();
                        Thread.Sleep(new Random().Next(delayInMilliseconds));
                        Console.WriteLine($"Consumer '{consumerId}' Consumed message '{cr.Value}' at: '{cr.TopicPartitionOffset}'");
                    }
                    catch (ConsumeException e)
                    {
                        Console.WriteLine($"Error occured: {e.Error.Reason}");
                    }
                }
                c.Close();
            }
        }

        public static IHostBuilder CreateHostBuilder(string[] args) =>
            Host.CreateDefaultBuilder(args)
                .ConfigureServices((hostContext, services) =>
                {
                    // Add the consumers as worker
                    services.AddHostedService<Worker>();

                    // Instrumentation key is read automatically from appsettings.json
                    services.AddApplicationInsightsTelemetryWorkerService();

                    // Build ServiceProvider.
                    IServiceProvider serviceProvider = services.BuildServiceProvider();
                    // Obtain TelemetryClient instance from DI, for additional manual tracking or to flush.
                    telemetryClient = serviceProvider.GetRequiredService<TelemetryClient>();

                });
    }

}
