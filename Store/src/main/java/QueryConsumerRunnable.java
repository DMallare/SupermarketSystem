import com.google.gson.Gson;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueryConsumerRunnable implements Runnable {
    private final Store store;
    private static final String KAFKA_HOST = System.getProperty("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getProperty("KAFKA_PORT");
    private static final String REQUEST_TOPIC = "request";
    private static final String RESPONSE_TOPIC = "response";
    private static final String EMPTY = "";
    private final String DO_NOT_WAIT = "0";
    private final String WAIT_FOR_LEADER = "1";
    private final String WAIT_FOR_ALL = "all";  // most durable option
    private String correlationId;
    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;


    public QueryConsumerRunnable() {
        // consumer properties
        store = Store.getStoreInstance();
        Properties consumerProps = new Properties();
        consumerProps.setProperty("bootstrap.servers", KAFKA_HOST + ":" + KAFKA_PORT);
        consumerProps.setProperty("group.id", "QueryConsumer");
        consumerProps.setProperty("enable.auto.commit", "true");
        consumerProps.setProperty("auto.commit.interval.ms", "20");
        consumerProps.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList(REQUEST_TOPIC));

        // producer props
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", KAFKA_HOST + ":" + KAFKA_PORT);
        producerProps.put("acks", DO_NOT_WAIT);
        producerProps.put("retries", 0);
        producerProps.put("linger.ms", 50);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(producerProps);
    }


    @Override
    public void run() {
        String response = EMPTY;
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10));
            for (ConsumerRecord<String, String> record : records) {
                response = parseRequest(record.value());
                correlationId = record.key();

                // enqueue message
                ProducerRecord<String, String> responseRecord =
                        new ProducerRecord<>(RESPONSE_TOPIC, correlationId, response);

                producer.send(responseRecord);
            }
        }
    }


    private String parseRequest(String message) {
        String response = EMPTY;
        try {
            Query query = new Gson().fromJson(message, Query.class);

            if (query.getType().equals("Store")) {
                StoreQueryResponse results =
                        store.getTopNItemsForStore(query.getN(), query.getId());
                response = new Gson().toJson(results);

            } else if (query.getType().equals("Item")) {
                ItemQueryResponse results =
                        store.getTopNStoresForItem(query.getN(), query.getId());
                response = new Gson().toJson(results);

            } else {
                response = "Query not supported";
            }
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger(QueryConsumerRunnable.class.getName()).log(Level.SEVERE, null, e);
            return response;
        }
    }

}
