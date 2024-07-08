# Using Spring with Azure Event Hubs for Apache Kafka Ecosystems

### [Tutorial for Spring Boot](https://github.com/Azure/azure-docs-sdk-java/blob/63723649d2555de3f044f07195f280d4e481cc79/docs-ref-conceptual/spring-framework/configure-spring-cloud-stream-binder-java-app-kafka-azure-event-hub.md)
### [Tutorial for Spring Cloud Stream Binder](https://github.com/MicrosoftDocs/azure-dev-docs/blob/main/articles/java/spring-framework/configure-spring-cloud-stream-binder-java-app-azure-event-hub.md)

The tutorial for using Spring Boot with Event Hubs for Kafka lives in the Azure Java SDK repo linked above. It is different from the other Event Hubs for Kafka tutorials, in that it uses your Azure auth credentials to automatically populate the connection string and FQDN. For further details, you can see where this population occurs [here](https://github.com/MicrosoftDocs/azure-dev-docs/blob/main/articles/java/spring-framework/auto-configure-azure-sdk-clients.md).
