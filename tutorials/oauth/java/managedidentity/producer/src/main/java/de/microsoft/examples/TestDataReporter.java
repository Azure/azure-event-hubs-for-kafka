//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.
package de.microsoft.examples;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class TestDataReporter implements Runnable {

    private static final int NUM_MESSAGES = 100;

    private final String topic;
    private final Producer<Long, String> producer;

    public TestDataReporter(final Producer<Long, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void run() {
        for(int i = 0; i < NUM_MESSAGES; i++) {
            long time = System.currentTimeMillis();
            System.out.println("Test Data #" + i + " from thread #" + Thread.currentThread().getId());

            final ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(topic, time, "Test Data #" + i);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.out.println(exception);
                    System.exit(1);
                }
            });
        }
        System.out.println("Finished sending " + NUM_MESSAGES + " messages from thread #" + Thread.currentThread().getId() + "!");
    }
}
