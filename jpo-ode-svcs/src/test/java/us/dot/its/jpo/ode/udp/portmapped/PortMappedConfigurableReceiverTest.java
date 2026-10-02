package us.dot.its.jpo.ode.udp.portmapped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;
import org.json.JSONObject;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties.ReceiverProperties;

class PortMappedConfigurableReceiverTest {

  private static final int RECEIVER_PORT = 15480;
  private static final String TOPIC = "topic.PortMappedConfigurableReceiverTest";
  private static final String ORIGIN_IP = "192.0.2.42";

  @Test
  @SuppressWarnings("unchecked")
  void configuredSourceAddressIsPublishedInRoutedMessage() throws Exception {
    ReceiverProperties receiverProperties = new ReceiverProperties();
    receiverProperties.setReceiverPort(RECEIVER_PORT);
    receiverProperties.setBufferSize(4096);

    RawEncodedJsonTopics topics = new RawEncodedJsonTopics();
    topics.setBsm(TOPIC);

    PortMappedIngestConfig.PortMappedIngestSource ingestConfig =
        new PortMappedIngestConfig.PortMappedIngestSource();
    ingestConfig.setOriginIp(ORIGIN_IP);
    ingestConfig.setType("BSM");

    CountDownLatch published = new CountDownLatch(1);
    AtomicReference<String> publishedJson = new AtomicReference<>();
    KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    doAnswer(invocation -> {
      publishedJson.set(invocation.getArgument(1, String.class));
      published.countDown();
      return null;
    }).when(kafkaTemplate).send(eq(TOPIC), anyString());

    TestPortMappedReceiver receiver = new TestPortMappedReceiver(
        receiverProperties, kafkaTemplate, topics, ingestConfig);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(receiver);

    try (DatagramSocket sender = new DatagramSocket()) {
      byte[] data = HexUtils.fromHexString(Files.readString(Path.of(
          "src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM.txt")));
      sender.send(new DatagramPacket(data, data.length,
          InetAddress.getLoopbackAddress(), RECEIVER_PORT));

      assertTrue(published.await(5, TimeUnit.SECONDS));
      assertEquals(ORIGIN_IP, new JSONObject(publishedJson.get()).getJSONObject("metadata")
          .getString("originIp"));
    } finally {
      receiver.setStopped(true);
      receiver.closeSocket();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private static class TestPortMappedReceiver extends PortMappedConfigurableReceiver {

    TestPortMappedReceiver(ReceiverProperties receiverProperties,
        KafkaTemplate<String, String> kafkaTemplate, RawEncodedJsonTopics topics,
        PortMappedIngestConfig.PortMappedIngestSource ingestConfig) {
      super(receiverProperties, kafkaTemplate, topics, ingestConfig);
    }

    void closeSocket() {
      socket.close();
    }
  }
}
