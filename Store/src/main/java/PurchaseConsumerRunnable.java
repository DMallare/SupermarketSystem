import com.google.gson.Gson;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class PurchaseConsumerRunnable implements Runnable {
    private Store store;
    private static final String KAFKA_HOST = System.getProperty("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getProperty("KAFKA_PORT");
    private static final String PURCHASE_TOPIC = "purchases";
    private KafkaConsumer<String, String> consumer;

    public PurchaseConsumerRunnable() throws IOException {
        store = Store.getStoreInstance();
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", KAFKA_HOST + ":" + KAFKA_PORT);
        props.setProperty("group.id", "StoreConsumer");
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
                storePurchase(record.value());
        }
    }

    /**
     * Adds the given purchase to the store
     * @param purchaseString - the message containing the purchase data
     * @return - true if the purchase was successfully added to the store,
     *           false otherwise
     */
    private boolean storePurchase(String purchaseString) {
        // create PurchaseItems and the Purchase itself from the message
        Purchase newPurchase = new Gson().fromJson(purchaseString, Purchase.class);
        PurchaseItems purchaseItems = new Gson().fromJson(newPurchase.getPurchaseItems(), PurchaseItems.class);

        // add data to the store
        for (int i = 0; i < purchaseItems.getItems().size(); i++) {
            PurchaseItem item = purchaseItems.getItems().get(i);
            String itemId = item.getItemID();
            int quantity = item.getNumberOfItems();
            store.addItemToStorePurchases(newPurchase.getStoreID(), itemId, quantity);
            store.addStoreToItemPurchases(newPurchase.getStoreID(), itemId, quantity);
        }
        return true;
    }

}
