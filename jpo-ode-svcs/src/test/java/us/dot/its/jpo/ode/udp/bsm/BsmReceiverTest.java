package us.dot.its.jpo.ode.udp.bsm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.testUtilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.testUtilities.TestUDPClient;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.util.DateTimeUtils;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties
@SpringBootTest(
    classes = {OdeKafkaProperties.class, UDPReceiverProperties.class},
    properties = {
        "ode.receivers.bsm.receiver-port=15352",
        "ode.kafka.topics.raw-encoded-json.bsm=topic.BsmReceiverTest"
    }
)
@ContextConfiguration(classes = {
    OdeKafkaProperties.class, UDPReceiverProperties.class,
    RawEncodedJsonTopics.class
})
class BsmReceiverTest {

  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Test
  void testRun() throws Exception {
    String fileContent =
        Files.readString(Paths.get(
            "src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM.json"));

    // create the needed topic for production
    try {
      embeddedKafka.addTopics(new NewTopic(rawEncodedJsonTopics.getBsm(), 1, (short) 1));
    } catch (Exception e) {
      // ignore because we only care that the topics exist not that they're unique
    }

    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneId.of("UTC")));
    // create the BsmReceiver and submit it to a runner
    BsmReceiver bsmReceiver = new BsmReceiver(udpReceiverProperties.getBsm(), odeKafkaProperties,
        rawEncodedJsonTopics.getBsm());
    ExecutorService executorService = Executors.newCachedThreadPool();
    executorService.submit(bsmReceiver);

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getBsm().getReceiverPort());
    udpClient.send(SupportedMessageType.BSM.getStartFlag() + fileContent);

    // setup the consumer for the topic bsmreceiver produces to
    var consumerProps = KafkaTestUtils.consumerProps(
        "BsmReceiverTest", "true", embeddedKafka);
    DefaultKafkaConsumerFactory<Integer, String> cf =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    Consumer<Integer, String> consumer = cf.createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(consumer, rawEncodedJsonTopics.getBsm());

    // read record from produce topic
    var singleRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getBsm());
    // confirm it looks as expected
    assertEquals(
        "{\"metadata\":{\"bsmSource\":\"EV\",\"recordType\":\"bsmTx\",\"securityResultCode\":\"success\",\"receivedMessageDetails\":{\"locationData\":{\"latitude\":\"unavailable\",\"longitude\":\"unavailable\",\"elevation\":\"unavailable\",\"speed\":\"unavailable\",\"heading\":\"unavailable\"},\"rxSource\":\"RSU\"},\"payloadType\":\"us.dot.its.jpo.ode.model.OdeAsn1Payload\",\"serialId\":{\"streamId\":\"de02c4a6-9ae3-4417-8bbc-636119ac0767\",\"bundleSize\":1,\"bundleId\":0,\"recordId\":0,\"serialNumber\":0},\"odeReceivedAt\":\"2024-11-26T23:53:21.120Z\",\"schemaVersion\":7,\"maxDurationTime\":0,\"recordGeneratedBy\":\"OBU\",\"sanitized\":false,\"asn1\":\"303031347B0A2020226D65746164617461223A207B0A202020202262736D536F75726365223A20224556222C0A20202020226C6F6746696C654E616D65223A202262736D54782E677A222C0A20202020227265636F726454797065223A202262736D5478222C0A20202020227365637572697479526573756C74436F6465223A202273756363657373222C0A202020202272656365697665644D65737361676544657461696C73223A207B0A202020202020226C6F636174696F6E44617461223A207B0A2020202020202020226C61746974756465223A202234302E35363537383831222C0A2020202020202020226C6F6E676974756465223A20222D3130352E30333136373432222C0A202020202020202022656C65766174696F6E223A202231343839222C0A2020202020202020227370656564223A2022302E34222C0A20202020202020202268656164696E67223A20223236372E34220A2020202020207D2C0A202020202020227278536F75726365223A20224E41220A202020207D2C0A20202020227061796C6F616454797065223A202275732E646F742E6974732E6A706F2E6F64652E6D6F64656C2E4F646542736D5061796C6F6164222C0A202020202273657269616C4964223A207B0A2020202020202273747265616D4964223A202238303137383063622D643931642D343400\",\"originIp\":\"127.0.0.1\"},\"payload\":{\"dataType\":\"us.dot.its.jpo.ode.model.OdeHexByteArray\",\"data\":{\"bytes\":\"303031347B0A2020226D65746164617461223A207B0A202020202262736D536F75726365223A20224556222C0A20202020226C6F6746696C654E616D65223A202262736D54782E677A222C0A20202020227265636F726454797065223A202262736D5478222C0A20202020227365637572697479526573756C74436F6465223A202273756363657373222C0A202020202272656365697665644D65737361676544657461696C73223A207B0A202020202020226C6F636174696F6E44617461223A207B0A2020202020202020226C61746974756465223A202234302E35363537383831222C0A2020202020202020226C6F6E676974756465223A20222D3130352E30333136373432222C0A202020202020202022656C65766174696F6E223A202231343839222C0A2020202020202020227370656564223A2022302E34222C0A20202020202020202268656164696E67223A20223236372E34220A2020202020207D2C0A202020202020227278536F75726365223A20224E41220A202020207D2C0A20202020227061796C6F616454797065223A202275732E646F742E6974732E6A706F2E6F64652E6D6F64656C2E4F646542736D5061796C6F6164222C0A202020202273657269616C4964223A207B0A2020202020202273747265616D4964223A202238303137383063622D643931642D343400\"}}}",
        singleRecord.value());

  }
}