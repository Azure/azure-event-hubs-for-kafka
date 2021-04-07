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
const numOfMessagesToSend = 5;

// create messages array function
const createMessagesArray = () => {
  let messages = [];

  // build some messages
  for(let i = 0; i < numOfMessagesToSend; i++) {
    messages.push({
      key: "key ".concat(i),
      value: "hello world #".concat(i)
    });
  }

  return messages;
};

// authenticate with Azure AD
new Promise((resolve, reject) => {
  // call ADAL authentication
  context.acquireTokenWithClientCredentials(audience, appId, appPassword, (error, tokenResponse) => {

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

  // obtain a producer
  const producer = kafka.producer();

  // produce some messages asynchronously
  const run = async () => {

    // connect the producer
    await producer.connect();

    // log
    console.log('Sending messages...');

    // send some messages
    await producer.send({
      topic: TOPIC,
      messages: createMessagesArray()
    });

    // log
    console.log(`${numOfMessagesToSend} Messages sent...`);

    // disconnect
    await producer.disconnect();

    // exit
    process.exit(0);
  }

  // run asynchronously
  run()
    .catch(
      e=> kafka.logger()
               .error(`Producer: ${e.message}`, { stack: e.stack }));
});

const errorTypes = ['unhandledRejection', 'uncaughtException'];
const signalTraps = ['SIGTERM', 'SIGINT', 'SIGUSR2'];

errorTypes.map(type => {
  process.on(type, async () => {
    try {
      console.log(`process.on ${type}`);
      await producer.disconnect();
      process.exit(0);
    } catch (_) {
      process.exit(1);
    }
  });
});

signalTraps.map(type => {
  process.once(type, async () => {
    try {
      await producer.disconnect();
    } finally {
      process.kill(process.pid, type);
    }
  });
});