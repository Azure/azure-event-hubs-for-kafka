using Azure.Core;
using Azure.Identity;
using Confluent.Kafka;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace KafkaProducer
{
    class Program
    {

        private static string eventHubNamespaceFQDNwithPort = "<event hub namespace name>.servicebus.windows.net:9093";

        private static string eventHubNamespace = "<event hub namespace name>";

        // connectionstring - primary or secondary key
        private static string policyConnectionString = "Endpoint=sb://<event hub namespace name>.servicebus.windows.net/;SharedAccessKeyName=<policy name>;SharedAccessKey=<policy key>;EntityPath=<event hub name>";

        //in kafka world this is the topic in event hub is the event hub name under the namespace
        private static string topicName = "<event hub name>";





        static async Task Main(string[] args)
        {
			Console.WriteLine("Hello Event Hub Kafka client!");

			//remember to proberly feed the following static attributes:
			//eventHubNamespace 
			//eventHubNamespaceFQDNwithPort


			try
			{
				//----- SAS AUTH -----//
				//
				//to leverage SAS Key as authentication methods we need the following static attribute valorized in the code:
				//policyConnectionString
				//
				//var producer = GetProducerSAS();
				//
				//----- -------- -----//


				//----- AAD AUTH -----//
				//
				//to leverage AAD as authentication methods you need to provide to the application a valid principal informations according to what's needed by DefaultAzureCredentialOptions
				//
				//Get a Producer using an AAD Token
				var producer = GetProducerAADToken();
				//
				//----- -------- -----//

				Console.WriteLine("Initiating Execution");

				for (int x = 0; x < 100; x++)
				{
					var msg = new Message<Null, string> { Value = string.Format("This is a sample message - msg # {0} at {1}", x, DateTime.Now.ToString("yyyMMdd_HHmmSSfff")) };
						
					// publishes the message to Event Hubs
					var result = await producer.ProduceAsync(topicName, msg);
						
					Console.WriteLine($"Message {result.Value} sent to partition {result.TopicPartition } with result {result.Status}");
				}
								
			}
			catch (Exception e)
			{
				Console.WriteLine(string.Format("Exception Ocurred - {0}", e.Message));
			}
		}


		/// <summary>
		/// Kafka Client callback to get or refresh a Token
		/// </summary>
		/// <param name="client"></param>
		/// <param name="cfg"></param>
		static void OAuthTokenRefreshCallback(IClient client, string cfg)
		{
            try
            {
                DefaultAzureCredentialOptions defaultAzureCredentialOptions = new DefaultAzureCredentialOptions();
                DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredential(defaultAzureCredentialOptions);

                var tokenRequestContext = new TokenRequestContext(new string[] { $"https://{eventHubNamespace}.servicebus.windows.net/.default" });

                var accessToken = defaultAzureCredential.GetToken(tokenRequestContext);


                var token = accessToken.Token;
                var expire = accessToken.ExpiresOn.ToUnixTimeMilliseconds();

                //principal could be null for Kafka OAuth 2.0 auth
                client.OAuthBearerSetToken(token, expire, null);
            }
            catch (Exception ex)
            {
				client.OAuthBearerSetTokenFailure(ex.ToString());
			}
		}



		private static IProducer<Null, string> GetProducerAADToken()
		{
			var pConfig = new ProducerConfig
			{
				BootstrapServers = eventHubNamespaceFQDNwithPort,
				SaslMechanism = SaslMechanism.OAuthBearer,
				SecurityProtocol = SecurityProtocol.SaslSsl,
				BrokerVersionFallback = "0.10.0.0",
				ApiVersionFallbackMs = 0,
				Debug = "security,broker,protocol"
			};

			//instantiates the producer
			var producerBuilder = new ProducerBuilder<Null, string>(pConfig);

			//configure the call back to grab a token
			IProducer<Null, string> producer = producerBuilder.SetOAuthBearerTokenRefreshHandler(OAuthTokenRefreshCallback).Build();

			return producer;
		}


		private static IProducer<Null, string> GetProducerSAS()
		{
			//for Plain Sasl mechanism use an hard coded naming convention for user name and use the connection string as password

			var pConfig = new ProducerConfig
			{
				BootstrapServers = eventHubNamespaceFQDNwithPort,
				SaslMechanism = SaslMechanism.Plain,
				SecurityProtocol = SecurityProtocol.SaslSsl,
				SaslUsername = "$ConnectionString",
				SaslPassword = policyConnectionString,
				BrokerVersionFallback = "0.10.0.0",
				ApiVersionFallbackMs = 0,
				Debug = "security,broker,protocol"
			};

			//instantiates the producer
			var producerBuilder = new ProducerBuilder<Null, string>(pConfig);

			IProducer<Null, string> producer = producerBuilder.Build();

			return producer;
		}

	}
}
