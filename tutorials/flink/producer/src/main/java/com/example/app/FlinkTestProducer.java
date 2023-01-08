package com.example.app;

//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Properties;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkTestProducer {

    private static final String TOPIC = "<EventhubName>";
    private static final String FILE_PATH = "src/main/resources/producer.config";

    public static void main(String... args) {
        try {
            
            // load properties from file
            Properties properties = new Properties();
            properties.load(new FileReader(FILE_PATH));

            // set properties.
            KafkaSinkBuilder<String> kafkaSinkBuilder = KafkaSink.<String>builder();
            for(String property: properties.stringPropertyNames())
            {
                kafkaSinkBuilder.setProperty(property, properties.getProperty(property));
            }

            // set serializer type.
            kafkaSinkBuilder.setRecordSerializer(KafkaRecordSerializationSchema.builder()
            .setTopic(TOPIC)
            .setValueSerializationSchema(new SimpleStringSchema())
            .build());

            KafkaSink<String> kafkaSink = kafkaSinkBuilder.build();

            // create stream environment, send simple stream to eventhub.
            final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            DataStream<String> sourceStream = env.fromElements("1", "2", "3");
            sourceStream.sinkTo(kafkaSink);

            // run the job.
            env.execute("Send to eventhub using Kafka api");
        } catch(FileNotFoundException e){
            System.out.println("FileNotFoundException: " + e);
        } catch (Exception e) {
            System.out.println("Failed with exception:: " + e);
        }
    }
}
