from azure.identity import DefaultAzureCredential, AzureCliCredential

class AzureTokenProvider():
    def __init__(self, token_scope):
        # Token provider needs a scope for which the token must be requested.
        # In this case, an Azure Event Hub
        self.token_scope = token_scope

    def token(self):
        """
        Returns a (str) ID/Access Token to be sent to the Kafka
        client.
        """
        credential = AzureCliCredential()
        token = credential.get_token(self.token_scope).token
        return token