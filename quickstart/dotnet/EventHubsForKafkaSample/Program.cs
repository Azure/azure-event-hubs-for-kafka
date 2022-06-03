using System.Configuration;
using EventHubsForKafkaSample;

string brokerList = ConfigurationManager.AppSettings["EH_FQDN"];
string connectionString = ConfigurationManager.AppSettings["EH_CONNECTION_STRING"];
string topic = ConfigurationManager.AppSettings["EH_NAME"];
string caCertLocation = ConfigurationManager.AppSettings["CA_CERT_LOCATION"];
string consumerGroup = ConfigurationManager.AppSettings["CONSUMER_GROUP"];

Console.WriteLine("Initializing Producer");
Worker.Producer(brokerList, connectionString, topic, caCertLocation).Wait();
Console.WriteLine();
Console.WriteLine("Initializing Consumer");
Worker.Consumer(brokerList, connectionString, consumerGroup, topic, caCertLocation);
Console.ReadKey();

