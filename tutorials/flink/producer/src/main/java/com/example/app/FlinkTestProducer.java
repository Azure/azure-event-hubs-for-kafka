//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Properties;

import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkTestProducer {

    private static final String TOPIC = "test";
    private static final String FILE_PATH = "src/main/resources/producer.config";

    public static void main(String... args) {
        try {
            Properties properties = new Properties();
            properties.load(new FileReader(FILE_PATH));

            KafkaSinkBuilder<Long> kafkaSinkBuilder = KafkaSink.<Long>builder();
            for(String property: properties.stringPropertyNames())
            {
                kafkaSinkBuilder.setProperty(property, properties.getProperty(property));
            }
            KafkaSink<Long> kafkaSink = kafkaSinkBuilder.build();
            

            final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            DataStream<Long> sourceStream = env.fromSequence(0, 200);
            sourceStream.sinkTo(kafkaSink);
            env.execute("Send to eventhub using Kafka api");
        } catch(FileNotFoundException e){
            System.out.println("FileNotFoundException: " + e);
        } catch (Exception e) {
            System.out.println("Failed with exception:: " + e);
        }
    }
}
