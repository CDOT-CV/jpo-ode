package us.dot.its.jpo.ode.plugin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ServiceRequest class.
 */
public class ServiceRequestTest {
  @Test
  void testServiceRequestWithRsusSerializationToXml() throws Exception {
    // Create a sample RSU object using RoadSideUnit.RSU
    RoadSideUnit.RSU rsu = new RoadSideUnit.RSU();
    rsu.setRsuTarget("192.168.1.1");
    rsu.setRsuRetries(2);
    rsu.setRsuTimeout(3000);
    rsu.setRsuIndex(1);
    rsu.setRsuUsername("admin");
    rsu.setRsuPassword("password");
    rsu.setSnmpProtocol(SnmpProtocol.NTCIP1218);

    // Add RSU to the ServiceRequest
    ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setRsus(new RoadSideUnit.RSU[] { rsu });

    // Serialize to XML
    XmlMapper xmlMapper = new XmlMapper();
    String xml = xmlMapper.writeValueAsString(serviceRequest);

    // Assert XML contains expected RSU fields
    Assertions.assertTrue(xml.contains("<rsus><rsus>"));
    Assertions.assertTrue(xml.contains("<rsuTarget>192.168.1.1</rsuTarget>"));
    Assertions.assertTrue(xml.contains("<rsuRetries>2</rsuRetries>"));
    Assertions.assertTrue(xml.contains("<rsuTimeout>3000</rsuTimeout>"));
    Assertions.assertTrue(xml.contains("<rsuIndex>1</rsuIndex>"));
    Assertions.assertTrue(xml.contains("<rsuUsername>admin</rsuUsername>"));
    Assertions.assertTrue(xml.contains("<rsuPassword>password</rsuPassword>"));
    Assertions.assertTrue(xml.contains("<snmpProtocol>NTCIP1218</snmpProtocol>"));
    Assertions.assertTrue(xml.contains("</rsus></rsus>"));

    ObjectNode consumed = xmlMapper.readValue(xml, ObjectNode.class);
    String xmlBack = xmlMapper.writeValueAsString(consumed);
    ServiceRequest deserialized = xmlMapper.readValue(xmlBack, ServiceRequest.class);
    Assertions.assertNotNull(deserialized);
    Assertions.assertNotNull(deserialized.getRsus());
    Assertions.assertEquals(1, deserialized.getRsus().length);
    RoadSideUnit.RSU deserializedRsu = deserialized.getRsus()[0];
    Assertions.assertEquals("192.168.1.1", deserializedRsu.getRsuTarget());
    Assertions.assertEquals(2, deserializedRsu.getRsuRetries());
    Assertions.assertEquals(3000, deserializedRsu.getRsuTimeout());
    Assertions.assertEquals(1, deserializedRsu.getRsuIndex());
    Assertions.assertEquals("admin", deserializedRsu.getRsuUsername());
    Assertions.assertEquals("password", deserializedRsu.getRsuPassword());
    Assertions.assertEquals(SnmpProtocol.NTCIP1218, deserializedRsu.getSnmpProtocol());
  }
}
