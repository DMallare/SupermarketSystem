import com.google.gson.Gson;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerRunnable implements Runnable {
    private static final String KAFKA_HOST = System.getProperty("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getProperty("KAFKA_PORT");
    private static final String PURCHASE_TOPIC = "purchases";
    private static final int NUM_SHARDS = 4;
    private static ArrayList<PurchaseDao> purchaseDaos;
    private KafkaConsumer<String, String> consumer;


    public ConsumerRunnable() {
        // create connections to each shard
        createPurchaseDaos();

        // Set up Kafka consumer
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", KAFKA_HOST + ":" + KAFKA_PORT);
        props.setProperty("group.id", "DBConsumer");
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "20");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new KafkaConsumer<>(props);
    }


    private static void createPurchaseDaos() {
        try {
            purchaseDaos = new ArrayList<>();
            purchaseDaos.add(new PurchaseDao(ConnectionDaoShard1.getConnection()));
            purchaseDaos.add(new PurchaseDao(ConnectionDaoShard2.getConnection()));
            purchaseDaos.add(new PurchaseDao(ConnectionDaoShard3.getConnection()));
            purchaseDaos.add(new PurchaseDao(ConnectionDaoShard4.getConnection()));
        } catch (SQLException ex) {
            System.err.println("Could not initialize all DB shard connections");
        }
    }


    @Override
    public void run() {
        consumer.subscribe(Arrays.asList(PURCHASE_TOPIC));
        boolean successfulInsert = false;
        String purchaseString = "";

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10));
            for (ConsumerRecord<String, String> record : records) {
                purchaseString = record.value();
                successfulInsert = makePurchase(purchaseString);
                if (!successfulInsert) {
                    System.err.println("ERROR: Unable to insert " + purchaseString + " into the DB");
                }
            }
        }
    }


    private boolean makePurchase(String purchaseString) {
        Purchase newPurchase = new Gson().fromJson(purchaseString, Purchase.class);
        int key = hash(newPurchase);
        PurchaseDao purchaseDao = purchaseDaos.get(key);
        return purchaseDao.createPurchase(newPurchase);
    }


    private int hash(Purchase purchase) {
        return (purchase.getCustomerID() + purchase.getStoreID()) % NUM_SHARDS;
    }

}
