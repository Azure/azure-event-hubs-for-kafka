//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;

public class TestDataReporter implements Runnable {

    private static final int NUM_MESSAGES = 100;
    final int oneMb = 1024 * 1024;
    final int kafkaSerializationOverhead = 512;

    private final String TOPIC;

    private Producer<String, String> producer;

    public TestDataReporter(final Producer<String, String> producer, String TOPIC) {
        this.producer = producer;
        this.TOPIC = TOPIC;
    }

    @Override
    public void run() {
        byte[] data = new byte[oneMb - kafkaSerializationOverhead];
        Arrays.fill(data, (byte)'a');
        String largeDummyValue = new String(data, StandardCharsets.UTF_8);

        for(int i = 0; i < 100; i++) {
            System.out.println("Publishing event: Key-" + i );
            final ProducerRecord<String, String> record = new ProducerRecord<String, String>(TOPIC, "Key-" + Integer.toString(i), "V1_" + Integer.toString(i) + largeDummyValue);
            producer.send(record, new Callback() {
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                        System.out.println(exception);
                        System.exit(1);
                    }
                }
            });
        }

        for(int i = 0; i < 50; i++) {
            System.out.println("Publishing updated event: Key-" + i );
            final ProducerRecord<String, String> record = new ProducerRecord<String, String>(TOPIC, "Key-" + Integer.toString(i), "V2_" + Integer.toString(i) + largeDummyValue);
            producer.send(record, new Callback() {
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                        System.out.println(exception);
                        System.exit(1);
                    }
                }
            });
        }
        System.out.println("Finished sending " + NUM_MESSAGES + " messages from thread #" + Thread.currentThread().getId() + "!");
    }
}