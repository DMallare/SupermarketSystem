import java.io.FileInputStream;
import java.util.Properties;

public class Main {
    private static final int NUM_THREADS = 50;

    public static void main(String[] args) throws Exception {
        // set config properties
        FileInputStream configFile = new FileInputStream("config.properties");
        Properties props = new Properties(System.getProperties());
        props.load(configFile);
        System.setProperties(props);
        //System.getProperties().list(System.out)

        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i ++) {
            threads[i] = new Thread(new ConsumerRunnable());
            threads[i].start();
        }

        for (int i = 0; i < NUM_THREADS; i ++) {
            threads[i].join();
        }
    }
}
