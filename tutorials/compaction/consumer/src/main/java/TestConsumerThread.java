//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

public class TestConsumerThread implements Runnable {

    private final String TOPIC;
    
    //Each consumer needs a unique client ID per thread
    private static int id = 0;

    public TestConsumerThread(final String TOPIC){
        this.TOPIC = TOPIC;
    }

    public void run (){
        final Consumer<String, String> consumer = createConsumer();
        System.out.println("Polling");

        try {
            while (true) {
                final ConsumerRecords<String, String> consumerRecords = consumer.poll(1000);
                for(ConsumerRecord<String, String> cr : consumerRecords) {
                    System.out.printf("Consumer Record(key, value):(%s, %s)\n", cr.key(), cr.value().substring(0, 2));
                }
                consumer.commitAsync();
            }
        } catch (CommitFailedException e) {
            System.out.println("CommitFailedException: " + e);
        } finally {
            consumer.close();
        }
    }

    private Consumer<String, String> createConsumer() {
        try {
            final Properties properties = new Properties();
            synchronized (TestConsumerThread.class) {
                properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "KafkaExampleConsumer#" + id);
                id++;
            }
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

            //Get remaining properties from config file
            properties.load(new FileReader("src/main/resources/consumer.config"));

            // Create the consumer using properties.
            final Consumer<String, String> consumer = new KafkaConsumer<>(properties);

            // Subscribe to the topic.
            consumer.subscribe(Collections.singletonList(TOPIC));
            return consumer;
            
        } catch (FileNotFoundException e){
            System.out.println("FileNotFoundException: " + e);
            System.exit(1);
            return null;        //unreachable
        } catch (IOException e){
            System.out.println("IOException: " + e);
            System.exit(1);
            return null;        //unreachable
        }
    }
}
