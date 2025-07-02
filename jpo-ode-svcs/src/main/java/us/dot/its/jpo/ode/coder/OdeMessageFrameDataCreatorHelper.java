package us.dot.its.jpo.ode.coder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.MessageFrame.MessageFrame;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import us.dot.its.jpo.ode.model.OdeMessageFramePayload;
import us.dot.its.jpo.ode.model.OdeMsgMetadata;
import us.dot.its.jpo.ode.model.RxSource;
import us.dot.its.jpo.ode.plugin.ServiceRequest;

/**
 * Helper class for creating OdeMessageFrameData objects from consumed data.
 */
@Slf4j
public class OdeMessageFrameDataCreatorHelper {

  private OdeMessageFrameDataCreatorHelper() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Creates an OdeMessageFrameData object from consumed XML data.
   *
   * @param consumedData The XML data string to be processed
   * @param simpleXmlMapper The XmlMapper for XML operations
   * @return OdeMessageFrameData object containing the processed data
   * @throws JsonProcessingException if there is an error processing the JSON data
   */
  public static OdeMessageFrameData createOdeMessageFrameData(String consumedData, 
      XmlMapper simpleXmlMapper) throws JsonProcessingException {
    ObjectNode consumed = simpleXmlMapper.readValue(consumedData, ObjectNode.class);
    JsonNode metadataNode = consumed.findValue(OdeMsgMetadata.METADATA_STRING);
    ServiceRequest request = null;
    if (metadataNode instanceof ObjectNode object) {
      object.remove(OdeMsgMetadata.ENCODINGS_STRING);
      if (object.has("request")) {
        String xmlBack = simpleXmlMapper.writeValueAsString(object.get("request"));
        request = simpleXmlMapper.readValue(xmlBack, ServiceRequest.class);
        object.remove("request");
      }
    }
    
    OdeMessageFrameMetadata metadata =
          simpleXmlMapper.convertValue(metadataNode, OdeMessageFrameMetadata.class);
    // Request will be null if not included
    metadata.setRequest(request);
    // Assign the rxSource if it does not exist due to the schema requiring it
    if (metadata.getReceivedMessageDetails() != null && metadata.getReceivedMessageDetails().getRxSource() == null) {
      metadata.getReceivedMessageDetails().setRxSource(RxSource.NA);
    }

    if (metadata.getSchemaVersion() <= 4) {
      metadata.setReceivedMessageDetails(null);
    }

    JsonNode messageFrameNode = consumed.findValue("MessageFrame");
    MessageFrame<?> messageFrame =
        simpleXmlMapper.convertValue(messageFrameNode, MessageFrame.class);
    OdeMessageFramePayload payload = new OdeMessageFramePayload(messageFrame);
    return new OdeMessageFrameData(metadata, payload);
  }
}
