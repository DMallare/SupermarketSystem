import com.google.gson.Gson;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerRunnable implements Runnable {
    private static final String KAFKA_HOST = System.getProperty("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getProperty("KAFKA_PORT");
    private static final String PURCHASE_TOPIC = "purchases";
    private final PurchaseDao purchaseDao;
    private KafkaConsumer<String, String> consumer;

    public ConsumerRunnable() {
        purchaseDao = new PurchaseDao();
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", KAFKA_HOST + ":" + KAFKA_PORT);
        props.setProperty("group.id", "DBConsumer");
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "20");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new KafkaConsumer<>(props);
    }

    @Override
    public void run() {
        consumer.subscribe(Arrays.asList(PURCHASE_TOPIC));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10));
            for (ConsumerRecord<String, String> record : records)
                // System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
                makePurchase(record.value());
        }
    }

    private boolean makePurchase(String purchaseString) {
        Purchase newPurchase = new Gson().fromJson(purchaseString, Purchase.class);
        return purchaseDao.createPurchase(newPurchase);
    }

}
