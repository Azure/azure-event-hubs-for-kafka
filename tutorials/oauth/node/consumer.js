const { Kafka, logLevel } = require('kafkajs')
const { AuthenticationContext } = require('adal-node');

// do your changes here
const appId = '<app-id>'; // also known as client id
const appPassword = '<app-password>'; // also known as client secret
const tenantId = '<tenant-id>';
const audience = 'https://<your-namespace>.servicebus.windows.net';
const namespace = '<your-namespace>.servicebus.windows.net:9093';

// no more changes below this line
const authorityHostUrl = 'https://login.microsoftonline.com';
const authorityUrl = `${authorityHostUrl}/${tenantId}/`;
const context = new AuthenticationContext(authorityUrl);
const TOPIC = 'test';


// authenticate with Azure AD
new Promise((resolve, reject) => {
  // call ADAL authentication
  context
    .acquireTokenWithClientCredentials(
      audience, appId, appPassword, (error, tokenResponse) => {

    // check for errors
    if (error) {

      // reject promise
      return reject(error);
    }

    // obtain token response
    this.session = tokenResponse;

    // resolve promise
    return resolve(this.session);
  });
})
.then(myToken => {

  // debug
  // console.log('outside token:', myToken);

  // create a new kafka object
  const kafka = new Kafka({
    clientId: 'my-app',
    brokers: [namespace],
    logLevel: logLevel.NOTHING,
    ssl: true,
    sasl: {
      mechanism: 'oauthbearer',
      oauthBearerProvider: async () => {
        return {
          value: myToken.accessToken
        }
      },
    },
    retry: {
      initialRetryTime: 100,
      retries: 1
    }
  });

  // obtain a consumer
  const consumer = kafka.consumer({ groupId: 'consumergroup' });

  // consume messages asynchronously
  const run = async () => {

    // connect the consumer
    await consumer.connect()
      .then(console.log('Connected! Hit CTRL-C to stop!\n'));

    // subscribe to topic
    await consumer.subscribe({ topic: TOPIC, fromBeginning: true })
      .then(() => {
        console.log(`Subscribed to TOPIC: ${TOPIC}`);
        console.log(`Waiting for incoming messages...`);
      });

    // run the consumer
    await consumer.run({

      // obtain each message and print it
      eachMessage: async ({ topic, partition, message }) => {
        console.log({
          partition,
          offset: message.offset,
          value: 'MESSAGE: ' + message.value.toString(),
        });
      },
    });
  }

  run()
    .catch(
      e => kafka.logger()
                .error(`Producer: ${e.message}`, { stack: e.stack }));
});
