using System;
using System.Threading.Tasks;
using Confluent.Kafka;
using Newtonsoft.Json;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.ApplicationInsights;
using Microsoft.ApplicationInsights.DataContracts;
using System.Configuration;
using System.Threading;

namespace ProducerStatisticsLibrdKafka
{
    public class Program
    {
        public static TelemetryClient telemetryClient;
        public static void Main(string[] args)
        {
            CreateHostBuilder(args).Build().Run();
            Console.ReadLine();
        }

        public static async Task ProduceMessages(int delayInMilliseconds)
        {
            string brokerList = ConfigurationManager.AppSettings["EH_FQDN"];
            string connectionString = ConfigurationManager.AppSettings["EH_CONNECTION_STRING"];
            string topic = ConfigurationManager.AppSettings["EH_NAME"];
            string caCertLocation = ConfigurationManager.AppSettings["CA_CERT_LOCATION"];

            var config = new ProducerConfig
            {
                BootstrapServers = brokerList,
                SecurityProtocol = SecurityProtocol.SaslSsl,
                SaslMechanism = SaslMechanism.Plain,
                SaslUsername = "$ConnectionString",
                SaslPassword = connectionString,
                SslCaLocation = caCertLocation,
                //librdkafka statistics emit interval. Required to be configured for capturing the librdkafka statistics
                StatisticsIntervalMs = 1000
            };

            // Set the StatisticsHandler to read and process the statistics from the librdkafka
            using (var producer = new ProducerBuilder<long, string>(config).SetKeySerializer(Serializers.Int64).SetValueSerializer(Serializers.Utf8).SetErrorHandler((_, e) => Console.WriteLine($"Error: {e.Reason}")).SetStatisticsHandler((_, json) =>
            {
                // Deserialize the json content
                var statistics = JsonConvert.DeserializeObject<ProducerStatistics>(json);

                // Fetch the metrics from the broker
                foreach (var broker in statistics.Brokers)
                {
                        var tx = new MetricTelemetry();
                        tx.Name = "transmitted requests";
                        tx.Sum = broker.Value.Tx;
                        telemetryClient.TrackMetric(tx);

                        var txbytes = new MetricTelemetry();
                        txbytes.Name = "transmitted bytes";
                        txbytes.Sum = broker.Value.TxBytes;
                        telemetryClient.TrackMetric(txbytes);

                        var txerrs = new MetricTelemetry();
                        txerrs.Name = "transmitted errors";
                        txerrs.Sum = broker.Value.TxErrs;
                        telemetryClient.TrackMetric(txerrs);

                        var txretries = new MetricTelemetry();
                        txretries.Name = "request retries";
                        txretries.Sum = broker.Value.TxRetries;
                        telemetryClient.TrackMetric(txretries);

                        var reqTimeouts = new MetricTelemetry();
                        reqTimeouts.Name = "Request Timeouts";
                        reqTimeouts.Sum = broker.Value.ReqTimeouts;
                        telemetryClient.TrackMetric(reqTimeouts);

                    Console.WriteLine($"Prop value sent for broker = {broker.Key} => transmitted requests (tx) = {broker.Value.Tx}");
                    Console.WriteLine($"Prop value sent for broker = {broker.Key} => transmitted bytes (txbytes) = {broker.Value.TxBytes}");
                    Console.WriteLine($"Prop value sent for broker = {broker.Key} => transmitted errors (txerrs) = {broker.Value.TxErrs}");
                    Console.WriteLine($"Prop value sent for broker = {broker.Key} => transmitted retries (txretries) = {broker.Value.TxRetries}");
                    Console.WriteLine($"Prop value sent for broker = {broker.Key} => request timeouts = {broker.Value.ReqTimeouts}");

                }
            }
            ).Build())
            {
                while (true)
                {
                    try
                    {
                        var message = "producer-msg";
                        var dr = await producer.ProduceAsync(topic, new Message<long, string> { Value = message });
                        Thread.Sleep(new Random().Next(delayInMilliseconds));
                        Console.WriteLine($"Delivered '{dr.Value}' to '{dr.TopicPartitionOffset}'");
                    }
                    catch (KafkaException e)
                    {
                        Console.WriteLine($"Delivery failed: {e.Error.Reason}");
                    }
                }
            }
            Console.ReadLine();
        }

        public static IHostBuilder CreateHostBuilder(string[] args) =>
            Host.CreateDefaultBuilder(args)
                .ConfigureServices((hostContext, services) =>
                {
                    // Add the consumers as worker
                    services.AddHostedService<Worker>();

                    // instrumentation key is read automatically from appsettings.json
                    services.AddApplicationInsightsTelemetryWorkerService();

                    // Build ServiceProvider.
                    IServiceProvider serviceProvider = services.BuildServiceProvider();
                    // Obtain TelemetryClient instance from DI, for additional manual tracking or to flush.
                    telemetryClient = serviceProvider.GetRequiredService<TelemetryClient>();

                });
    }

}
