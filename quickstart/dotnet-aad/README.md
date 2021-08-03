# Event Hub Kafka endpoint Azure AD Authentication using C#

Event Hub is a big-data streaming platform and event ingestion service part of the messaging service ecosystem in Azure

Event Hub for Kafka support both SAS (shared access signatures) and OAuth 2.0 (in our case Azure AD).

I started to look for sample code around using a C# library authenticating with an OAuth 2.0 token obtained from Azure AD but I didn't found anything, so I decided to create my sample code and the best .NET Kafka library I found out there it was the “Confluent.Kafka”.

This sample will connect to Event Hub kafka endpoint using the Confluent.Kafka library grabbing an Azure AD Access Token with the help of the Azure.Identity library. 


## Executing the sample 

In order to execute the sample code you need to fill some variable with some configuration of your Event Hub (of course for production grade app this stuff should come from a configuration and not hardcoded into the code).

    private static string eventHubNamespaceFQDNwithPort = "<event hub namespace name>.servicebus.windows.net:9093";

    private static string eventHubNamespace = "<event hub namespace name>";

    // connectionstring - primary or secondary key
    private static string policyConnectionString = "Endpoint=sb://<event hub namespace name>.servicebus.windows.net/;SharedAccessKeyName=<policy name>;SharedAccessKey=<policy key>;EntityPath=<event hub name>";

    //in kafka world this is the topic in event hub is the event hub name under the namespace
    private static string topicName = "<event hub name>";


The, the code leverage the Azure.Identity package, in particular the 'DefaultAzureCredentialOptions' class to obtain a token.
Please follo the instruction on the article you find linked at the bottom to configure a Principal and provide the secret to this sample app. 


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




## Microsoft Techcommunity Blog Post

You can find the full blog post at this url:
https://techcommunity.microsoft.com/t5/fasttrack-for-azure/event-hub-kafka-endpoint-azure-ad-authentication-using-c/ba-p/2586185



