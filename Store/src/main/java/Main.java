import java.io.FileInputStream;
import java.util.Properties;

public class Main {
    private static final int NUM_PURCHASE_THREADS = 50;

    public static void main(String[] args) throws Exception {
        // set config properties
        FileInputStream configFile = new FileInputStream("config.properties");
        Properties props = new Properties(System.getProperties());
        props.load(configFile);
        System.setProperties(props);

        Thread[] purchaseThreads = new Thread[NUM_PURCHASE_THREADS];
        for (int i = 0; i < NUM_PURCHASE_THREADS; i ++) {
            purchaseThreads[i] =
                    new Thread(new PurchaseConsumerRunnable());
            purchaseThreads[i].start();
        }

        Thread queryRequestThread =
                new Thread (new QueryConsumerRunnable());
        queryRequestThread.start();

        for (int i = 0; i < NUM_PURCHASE_THREADS; i ++) {
            purchaseThreads[i].join();
        }

        queryRequestThread.join();
    }

}
