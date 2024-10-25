package us.dot.its.jpo.ode.traveler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.model.OdeMsgMetadata;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.plugin.RoadSideUnit.RSU;
import us.dot.its.jpo.ode.plugin.SNMP;
import us.dot.its.jpo.ode.plugin.ServiceRequest;
import us.dot.its.jpo.ode.plugin.SituationDataWarehouse.SDW;
import us.dot.its.jpo.ode.plugin.j2735.DdsAdvisorySituationData;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.TravelerInputData;
import us.dot.its.jpo.ode.traveler.TimTransmogrifier.TimTransmogrifierException;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.util.XmlUtils.XmlUtilsException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = OdeProperties.class)
class TimTransmogrifierTest {

    @Autowired
    OdeProperties odeProperties;

    private static final String schemaVersion = "7";

    @Test
    void testGetRsu() {

        RSU expected = new RSU("127.0.0.1", "v3user", "password", 1, 2000);

        // rsuUsername and rsuPassword are null
        RSU actual1 = new RSU("127.0.0.1", null, null, 1, 2000);
        TimTransmogrifier.updateRsuCreds(actual1, odeProperties);
        assertEquals(expected, actual1);

        // rsuUsername and rsuPassword are not-null
        RSU actual2 = new RSU("127.0.0.1", "v3user", "password", 1, 2000);
        TimTransmogrifier.updateRsuCreds(actual2, odeProperties);
        assertEquals(expected, actual2);

        // rsuUsername and rsuPassword are blank
        RSU actual3 = new RSU("127.0.0.1", "", "", 1, 2000);
        TimTransmogrifier.updateRsuCreds(actual3, odeProperties);
        assertEquals(expected, actual3);
    }

    @Test
    void testObfuscateRsuPassword() {
        String actual = TimTransmogrifier.obfuscateRsuPassword(
                "{\"metadata\":{\"request\":{\"ode\":{\"version\":3,\"verb\":\"POST\"},\"sdw\":null,\"rsus\":[{\"rsuTarget\":\"127.0.0.1\",\"rsuUsername\":\"v3user\",\"rsuPassword\": \"password\",\"rsuRetries\":0,\"rsuTimeout\":2000,\"rsuIndex\":10},{\"rsuTarget\":\"127.0.0.2\",\"rsuUsername\":\"v3user\",\"rsuPassword\": \"password\",\"rsuRetries\":1,\"rsuTimeout\":1000,\"rsuIndex\":10},{\"rsuTarget\":\"127.0.0.3\",\"rsuUsername\":\"v3user\",\"rsuPassword\": \"password\",\"rsuRetries\":1,\"rsuTimeout\":1000,\"rsuIndex\":10}],\"snmp\":{\"rsuid\":\"00000083\",\"msgid\":31,\"mode\":1,\"channel\":178,\"interval\":2,\"deliverystart\":\"2017-06-01T17:47:11-05:00\",\"deliverystop\":\"2018-01-01T17:47:11-05:15\",\"enable\":1,\"status\":4}},\"payloadType\":\"us.dot.its.jpo.ode.model.OdeMsgPayload\",\"serialId\":{\"streamId\":\"59651ecc-240c-4440-9011-4a43c926817b\",\"bundleSize\":1,\"bundleId\":0,\"recordId\":0,\"serialNumber\":0},\"odeReceivedAt\":\"2018-11-16T19:21:22.568Z\",\"schemaVersion\":6,\"recordGeneratedAt\":\"2017-03-13T06:07:11Z\",\"recordGeneratedBy\":\"TMC\",\"sanitized\":false},\"payload\":{\"dataType\":\"us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage\",\"data\":{\"msgCnt\":13,\"timeStamp\":\"2017-03-13T01:07:11-05:00\",\"packetID\":\"EC9C236B0000000000\",\"urlB\":\"null\",\"dataframes\":[{\"sspTimRights\":0,\"frameType\":\"advisory\",\"msgId\":{\"roadSignID\":{\"position\":{\"latitude\":41.678473,\"longitude\":-108.782775,\"elevation\":917.1432},\"viewAngle\":\"1010101010101010\",\"mutcdCode\":\"warning\",\"crc\":\"0000000000000000\"},\"furtherInfoID\":null},\"startDateTime\":\"2017-12-01T17:47:11-05:00\",\"durationTime\":22,\"priority\":0,\"sspLocationRights\":3,\"regions\":[{\"name\":\"bob\",\"regulatorID\":23,\"segmentID\":33,\"anchorPosition\":{\"latitude\":41.678473,\"longitude\":-108.782775,\"elevation\":917.1432},\"laneWidth\":7,\"directionality\":3,\"closedPath\":false,\"direction\":\"1010101010101010\",\"description\":\"geometry\",\"path\":null,\"geometry\":{\"direction\":\"1010101010101010\",\"extent\":1,\"laneWidth\":33,\"circle\":{\"position\":{\"latitude\":41.678473,\"longitude\":-108.782775,\"elevation\":917.1432},\"radius\":15,\"units\":7}},\"oldRegion\":null}],\"sspMsgTypes\":2,\"sspMsgContent\":3,\"content\":\"Advisory\",\"items\":[\"125\",\"some text\",\"250\",\"\\u002798765\"],\"url\":\"null\"}],\"asnDataFrames\":null}}}");
        assertEquals(
                "{\"metadata\":{\"request\":{\"ode\":{\"version\":3,\"verb\":\"POST\"},\"sdw\":null,\"rsus\":[{\"rsuTarget\":\"127.0.0.1\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"*\",\"rsuRetries\":0,\"rsuTimeout\":2000,\"rsuIndex\":10},{\"rsuTarget\":\"127.0.0.2\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"*\",\"rsuRetries\":1,\"rsuTimeout\":1000,\"rsuIndex\":10},{\"rsuTarget\":\"127.0.0.3\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"*\",\"rsuRetries\":1,\"rsuTimeout\":1000,\"rsuIndex\":10}],\"snmp\":{\"rsuid\":\"00000083\",\"msgid\":31,\"mode\":1,\"channel\":178,\"interval\":2,\"deliverystart\":\"2017-06-01T17:47:11-05:00\",\"deliverystop\":\"2018-01-01T17:47:11-05:15\",\"enable\":1,\"status\":4}},\"payloadType\":\"us.dot.its.jpo.ode.model.OdeMsgPayload\",\"serialId\":{\"streamId\":\"59651ecc-240c-4440-9011-4a43c926817b\",\"bundleSize\":1,\"bundleId\":0,\"recordId\":0,\"serialNumber\":0},\"odeReceivedAt\":\"2018-11-16T19:21:22.568Z\",\"schemaVersion\":6,\"recordGeneratedAt\":\"2017-03-13T06:07:11Z\",\"recordGeneratedBy\":\"TMC\",\"sanitized\":false},\"payload\":{\"dataType\":\"us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage\",\"data\":{\"msgCnt\":13,\"timeStamp\":\"2017-03-13T01:07:11-05:00\",\"packetID\":\"EC9C236B0000000000\",\"urlB\":\"null\",\"dataframes\":[{\"sspTimRights\":0,\"frameType\":\"advisory\",\"msgId\":{\"roadSignID\":{\"position\":{\"latitude\":41.678473,\"longitude\":-108.782775,\"elevation\":917.1432},\"viewAngle\":\"1010101010101010\",\"mutcdCode\":\"warning\",\"crc\":\"0000000000000000\"},\"furtherInfoID\":null},\"startDateTime\":\"2017-12-01T17:47:11-05:00\",\"durationTime\":22,\"priority\":0,\"sspLocationRights\":3,\"regions\":[{\"name\":\"bob\",\"regulatorID\":23,\"segmentID\":33,\"anchorPosition\":{\"latitude\":41.678473,\"longitude\":-108.782775,\"elevation\":917.1432},\"laneWidth\":7,\"directionality\":3,\"closedPath\":false,\"direction\":\"1010101010101010\",\"description\":\"geometry\",\"path\":null,\"geometry\":{\"direction\":\"1010101010101010\",\"extent\":1,\"laneWidth\":33,\"circle\":{\"position\":{\"latitude\":41.678473,\"longitude\":-108.782775,\"elevation\":917.1432},\"radius\":15,\"units\":7}},\"oldRegion\":null}],\"sspMsgTypes\":2,\"sspMsgContent\":3,\"content\":\"Advisory\",\"items\":[\"125\",\"some text\",\"250\",\"\\u002798765\"],\"url\":\"null\"}],\"asnDataFrames\":null}}}",
                actual);
    }

    public void assertConvertArray(String array, String arrayKey, String elementKey, Object expectedXml)
            throws JsonUtilsException, XmlUtilsException {
        JsonNode obj = JsonUtils.toObjectNode(array);
        JsonNode oldObj = obj.get(arrayKey);

        JsonNode newObj = XmlUtils.createEmbeddedJsonArrayForXmlConversion(elementKey, oldObj);
        String actualXml = XmlUtils.toXmlStatic(newObj);

        assertEquals(expectedXml, actualXml);
    }

    @Test
    void testConvertRsusArray() throws JsonUtilsException, XmlUtilsException {
        String single = "{\"ode\":{\"version\":3,\"verb\":\"POST\"},\"rsus\":{\"rsu_\":[{\"rsuTarget\":\"127.0.0.3\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"password\",\"rsuRetries\":1,\"rsuTimeout\":1000,\"rsuIndex\":10}]},\"snmp\":{\"rsuid\":\"00000083\",\"msgid\":31,\"mode\":1,\"channel\":178,\"interval\":2,\"deliverystart\":\"2017-06-01T17:47:11-05:00\",\"deliverystop\":\"2018-01-01T17:47:11-05:15\",\"enable\":1,\"status\":4}}";
        String singleXmlExpected = "<ObjectNode><rsus><rsu_><rsuTarget>127.0.0.3</rsuTarget><rsuUsername>v3user</rsuUsername><rsuPassword>password</rsuPassword><rsuRetries>1</rsuRetries><rsuTimeout>1000</rsuTimeout><rsuIndex>10</rsuIndex></rsu_></rsus></ObjectNode>";
        assertConvertArray(single, TimTransmogrifier.RSUS_STRING, TimTransmogrifier.RSUS_STRING, singleXmlExpected);

        String multi = "{\"ode\":{\"version\":3,\"verb\":\"POST\"},\"rsus\":{\"rsu_\":[{\"rsuTarget\":\"127.0.0.1\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"password\",\"rsuRetries\":0,\"rsuTimeout\":2000,\"rsuIndex\":10},{\"rsuTarget\":\"127.0.0.2\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"password\",\"rsuRetries\":1,\"rsuTimeout\":1000,\"rsuIndex\":10},{\"rsuTarget\":\"127.0.0.3\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"password\",\"rsuRetries\":1,\"rsuTimeout\":1000,\"rsuIndex\":10}]},\"snmp\":{\"rsuid\":\"00000083\",\"msgid\":31,\"mode\":1,\"channel\":178,\"interval\":2,\"deliverystart\":\"2017-06-01T17:47:11-05:00\",\"deliverystop\":\"2018-01-01T17:47:11-05:15\",\"enable\":1,\"status\":4}}";
        String multiXmlExpected = "<ObjectNode><rsus><rsu_><rsuTarget>127.0.0.1</rsuTarget><rsuUsername>v3user</rsuUsername><rsuPassword>password</rsuPassword><rsuRetries>0</rsuRetries><rsuTimeout>2000</rsuTimeout><rsuIndex>10</rsuIndex></rsu_><rsu_><rsuTarget>127.0.0.2</rsuTarget><rsuUsername>v3user</rsuUsername><rsuPassword>password</rsuPassword><rsuRetries>1</rsuRetries><rsuTimeout>1000</rsuTimeout><rsuIndex>10</rsuIndex></rsu_><rsu_><rsuTarget>127.0.0.3</rsuTarget><rsuUsername>v3user</rsuUsername><rsuPassword>password</rsuPassword><rsuRetries>1</rsuRetries><rsuTimeout>1000</rsuTimeout><rsuIndex>10</rsuIndex></rsu_></rsus></ObjectNode>";
        assertConvertArray(multi, TimTransmogrifier.RSUS_STRING, TimTransmogrifier.RSUS_STRING, multiXmlExpected);
    }

    @Test
    void testBuildASDNoSDWReturnsNull() throws TimTransmogrifierException {
        DdsAdvisorySituationData actualASD = TimTransmogrifier.buildASD(new ServiceRequest());
        assertNull(actualASD);
    }

    @Test
    void testBuildASDNullTimeRethrowsTimeParsingExceptionFromSNMP() {

        ServiceRequest inputServiceRequest = new ServiceRequest();
        inputServiceRequest.setSdw(new SDW());
        inputServiceRequest.setSnmp(new SNMP());

        try {
            TimTransmogrifier.buildASD(inputServiceRequest);
            fail("Expected TimTransmogrifierException");
        } catch (Exception e) {
            assertInstanceOf(TimTransmogrifierException.class, e);
        }
    }

    @Test
    void testBuildASDNullTimeRethrowsTimeParsingExceptionFromSDW() {

        ServiceRequest inputServiceRequest = new ServiceRequest();
        inputServiceRequest.setSdw(new SDW());

        try {
            TimTransmogrifier.buildASD(inputServiceRequest);
            fail("Expected TimTransmogrifierException");
        } catch (Exception e) {
            assertInstanceOf(TimTransmogrifierException.class, e);
        }
    }

    @Test
    void testBuildASDValidTimeFromSNMP() throws TimTransmogrifierException {

        SNMP inputSNMP = new SNMP();
        inputSNMP.setDeliverystart("2017-06-01T17:47:11-05:00");
        inputSNMP.setDeliverystop("2018-03-01T17:47:11-05:15");

        ServiceRequest inputServiceRequest = new ServiceRequest();
        inputServiceRequest.setSdw(new SDW());
        inputServiceRequest.setSnmp(inputSNMP);

        DdsAdvisorySituationData actualASD = TimTransmogrifier.buildASD(inputServiceRequest);

        assertNotNull(actualASD);
        assertEquals(6, actualASD.getAsdmDetails().getStartTime().getMonth());
        assertEquals(3, actualASD.getAsdmDetails().getStopTime().getMonth());
    }

    @Test
    void testBuildASDValidTimeFromSDW() throws TimTransmogrifierException {

        SDW inputSDW = new SDW();
        inputSDW.setDeliverystart("2017-06-01T17:47:11-05:00");
        inputSDW.setDeliverystop("2018-03-01T17:47:11-05:15");

        ServiceRequest inputServiceRequest = new ServiceRequest();
        inputServiceRequest.setSdw(inputSDW);

        DdsAdvisorySituationData actualASD = TimTransmogrifier.buildASD(inputServiceRequest);

        assertNotNull(actualASD);
        assertEquals(Integer.valueOf(6), actualASD.getAsdmDetails().getStartTime().getMonth());
        assertEquals(Integer.valueOf(3), actualASD.getAsdmDetails().getStopTime().getMonth());
    }

    @Test
    void testConvertToXMLASD() throws TimTransmogrifierException, JsonUtilsException, XmlUtilsException {

        SDW inputSDW = new SDW();
        inputSDW.setDeliverystart("2017-06-01T17:47:11-05:00");
        inputSDW.setDeliverystop("2018-03-01T17:47:11-05:15");

        ServiceRequest inputServiceRequest = new ServiceRequest();
        inputServiceRequest.setSdw(inputSDW);
        inputServiceRequest.setRsus(new RSU[0]);

        DdsAdvisorySituationData actualASD = TimTransmogrifier.buildASD(inputServiceRequest);
        actualASD.setRequestID("7876BA7F");
        actualASD.getAsdmDetails().setAsdmID("7876BA7F");

        TravelerInputData fakeTID = new TravelerInputData();
        fakeTID.setRequest(inputServiceRequest);

        ObjectNode encodableTID = JsonUtils.toObjectNode(JsonUtils.toJson(fakeTID, false));

        SerialId staticSerialId = new SerialId();
        staticSerialId.setStreamId("6c33f802-418d-4b67-89d1-326b4fc8b1e3");

        OdeMsgMetadata staticOdeMsgMetadata = new OdeMsgMetadata();

        staticOdeMsgMetadata.setSchemaVersion(Integer.parseInt(schemaVersion));

        String actualXML = TimTransmogrifier.convertToXml(actualASD, encodableTID, staticOdeMsgMetadata, staticSerialId);
        String expected = String.format("<OdeAsn1Data><metadata><payloadType>us.dot.its.jpo.ode.model.OdeAsdPayload</payloadType><serialId><streamId>6c33f802-418d-4b67-89d1-326b4fc8b1e3</streamId><bundleSize>1</bundleSize><bundleId>0</bundleId><recordId>0</recordId><serialNumber>0</serialNumber></serialId><odeReceivedAt>timeTime</odeReceivedAt><schemaVersion>%s</schemaVersion><maxDurationTime>0</maxDurationTime><sanitized>false</sanitized><request><sdw><ttl>thirtyminutes</ttl><deliverystart>2017-06-01T17:47:11-05:00</deliverystart><deliverystop>2018-03-01T17:47:11-05:15</deliverystop></sdw><rsus/></request><encodings><encodings><elementName>MessageFrame</elementName><elementType>MessageFrame</elementType><encodingRule>UPER</encodingRule></encodings><encodings><elementName>Ieee1609Dot2Data</elementName><elementType>Ieee1609Dot2Data</elementType><encodingRule>COER</encodingRule></encodings><encodings><elementName>AdvisorySituationData</elementName><elementType>AdvisorySituationData</elementType><encodingRule>UPER</encodingRule></encodings></encodings></metadata><payload><dataType>us.dot.its.jpo.ode.plugin.j2735.DdsAdvisorySituationData</dataType><data><AdvisorySituationData><dialogID>156</dialogID><seqID>5</seqID><groupID>00000000</groupID><requestID>7876BA7F</requestID><recordID>00000000</recordID><timeToLive>1</timeToLive><serviceRegion/><asdmDetails><asdmID>7876BA7F</asdmID><asdmType>2</asdmType><distType>03</distType><startTime><year>0</year><month>0</month><day>0</day><hour>0</hour><minute>0</minute></startTime><stopTime><year>0</year><month>0</month><day>0</day><hour>0</hour><minute>0</minute></stopTime><advisoryMessage><Ieee1609Dot2Data><protocolVersion>3</protocolVersion><content><unsecuredData><MessageFrame><messageId>31</messageId><value><TravelerInformation/></value></MessageFrame></unsecuredData></content></Ieee1609Dot2Data></advisoryMessage></asdmDetails></AdvisorySituationData></data></payload></OdeAsn1Data>", schemaVersion);
        assertEquals(expected, actualXML);
    }

    @Test
    void testConvertToXMLMessageFrame() throws TimTransmogrifierException, JsonUtilsException, XmlUtilsException {

        SDW inputSDW = new SDW();
        inputSDW.setDeliverystart("2017-06-01T17:47:11-05:00");
        inputSDW.setDeliverystop("2018-03-01T17:47:11-05:15");

        ServiceRequest inputServiceRequest = new ServiceRequest();
        inputServiceRequest.setSdw(inputSDW);
        inputServiceRequest.setRsus(new RSU[0]);

        DdsAdvisorySituationData actualASD = TimTransmogrifier.buildASD(inputServiceRequest);
        actualASD.setRequestID("7876BA7F");
        actualASD.getAsdmDetails().setAsdmID("7876BA7F");

        TravelerInputData fakeTID = new TravelerInputData();
        fakeTID.setRequest(inputServiceRequest);

        ObjectNode encodableTID = JsonUtils.toObjectNode(JsonUtils.toJson(fakeTID, false));

        SerialId staticSerialId = new SerialId();
        staticSerialId.setStreamId("6c33f802-418d-4b67-89d1-326b4fc8b1e3");

        OdeMsgMetadata staticOdeMsgMetadata = new OdeMsgMetadata();
        staticOdeMsgMetadata.setSchemaVersion(Integer.parseInt(schemaVersion));

        String actualXML = TimTransmogrifier.convertToXml(null, encodableTID, staticOdeMsgMetadata, staticSerialId);
        var expected = String.format("<OdeAsn1Data><metadata><payloadType>us.dot.its.jpo.ode.model.OdeTimPayload</payloadType><serialId><streamId>6c33f802-418d-4b67-89d1-326b4fc8b1e3</streamId><bundleSize>1</bundleSize><bundleId>0</bundleId><recordId>0</recordId><serialNumber>0</serialNumber></serialId><odeReceivedAt>timeTime</odeReceivedAt><schemaVersion>%s</schemaVersion><maxDurationTime>0</maxDurationTime><sanitized>false</sanitized><request><sdw><ttl>thirtyminutes</ttl><deliverystart>2017-06-01T17:47:11-05:00</deliverystart><deliverystop>2018-03-01T17:47:11-05:15</deliverystop></sdw><rsus/></request><encodings><encodings><elementName>MessageFrame</elementName><elementType>MessageFrame</elementType><encodingRule>UPER</encodingRule></encodings></encodings></metadata><payload><dataType>MessageFrame</dataType><data><MessageFrame><messageId>31</messageId><value><TravelerInformation/></value></MessageFrame></data></payload></OdeAsn1Data>", schemaVersion);
        assertEquals(expected, actualXML);
    }

    @Test
    void testCreateOdeTimData() throws JsonUtilsException {

        JSONObject testObject = JsonUtils
                .toJSONObject("{\"metadata\":{\"object\":\"value\"},\"payload\":{\"object\":\"value\"}}");
        JSONObject actualOdeTimData = TimTransmogrifier.createOdeTimData(testObject);

        assertEquals(
                "{\"metadata\":{\"payloadType\":\"us.dot.its.jpo.ode.model.OdeTimPayload\",\"object\":\"value\"},\"payload\":{\"dataType\":\"TravelerInformation\",\"object\":\"value\"}}",
                actualOdeTimData.toString());
    }

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {

        Constructor<TimTransmogrifier> constructor = TimTransmogrifier.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail("Expected IllegalAccessException.class");
        } catch (Exception e) {
            assertEquals(InvocationTargetException.class, e.getClass());
        }
    }

}
