import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;


@WebServlet(name = "TopItemForStoreServlet", value = "/items/store")
public class TopItemsForStoreServlet extends HttpServlet {
    private static final int NUM_TOP_ITEMS = 10;
    private static final String REQUEST_TOPIC = "request";
    private static final String RESPONSE_TOPIC = "response";
    private static final String KAFKA_HOST = System.getProperty("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getProperty("KAFKA_PORT");
    private final String DO_NOT_WAIT = "0";
    private final String WAIT_FOR_LEADER = "1";
    private final String WAIT_FOR_ALL = "all";  // most durable option
    private Producer<String, String> producer;
    private KafkaConsumer<String, String> consumer;
    private String correlationId;

    @Override
    public void init() throws ServletException {
        correlationId = UUID.randomUUID().toString();

        // producer properties
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", KAFKA_HOST + ":" + KAFKA_PORT);
        producerProps.put("acks", DO_NOT_WAIT);
        producerProps.put("retries", 0);
        producerProps.put("linger.ms", 50);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(producerProps);

        // consumer props
        Properties consumerProps = new Properties();
        consumerProps.setProperty("bootstrap.servers", KAFKA_HOST + ":" + KAFKA_PORT);
        consumerProps.setProperty("group.id", "QueryConsumer" + correlationId);
        consumerProps.setProperty("enable.auto.commit", "true");
        consumerProps.setProperty("auto.commit.interval.ms", "20");
        consumerProps.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new KafkaConsumer<>(consumerProps);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        String urlPath = request.getPathInfo();

        // check we have a URL
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameters");
            return;
        }

        // validate URL query params
        String[] urlParts = urlPath.split("/");
        if (!isUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Invalid Store ID");
            return;
        }

        try {
            // build message object
            StoreQuery query = new StoreQuery();
            query.setId(Integer.parseInt(urlParts[1]));
            query.setN(NUM_TOP_ITEMS);
            query.setType("Store");
            String message = new Gson().toJson(query);

            // enqueue message
            ProducerRecord<String, String> requestRecord = new ProducerRecord<>(REQUEST_TOPIC, correlationId, message);
            producer.send(requestRecord);

            // wait for response
            consumer.subscribe(Arrays.asList(RESPONSE_TOPIC));
            while(true) {
                ConsumerRecords<String, String> responseRecords = consumer.poll(Duration.ofMillis(10));
                for (ConsumerRecord<String, String> responseRecord : responseRecords) {
                    if (responseRecord.key().equals(correlationId)) {
                        response.setStatus((HttpServletResponse.SC_OK));
                        response.getWriter().write(responseRecord.value());
                        return;
                    }
                }
            }

        } catch (Exception e) {
            response.setStatus((HttpServletResponse.SC_NOT_FOUND));
            response.getWriter().write("Request was unable to be processed.");
        }

    }


    private boolean verifyStoreId(String storeIdString) {
        try {
            int storeId = Integer.parseInt(storeIdString);
            if (storeId < 0) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private boolean isUrlValid(String[] urlParts) {
        // urlPath = "/items/store/store_id
        // urlParts = [ , store_id]
        return verifyStoreId(urlParts[1]);
    }


    @Override
    public void destroy() {
        producer.close();
        consumer.close();
    }

}
