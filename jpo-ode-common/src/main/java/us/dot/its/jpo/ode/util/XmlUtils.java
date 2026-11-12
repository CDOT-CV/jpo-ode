/*******************************************************************************
 * Copyright 2018 572682
 * 
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   <p>http://www.apache.org/licenses/LICENSE-2.0
 * 
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package us.dot.its.jpo.ode.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper.Builder;
import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.codehaus.stax2.XMLStreamReader2;
import org.json.JSONObject;
import org.json.XML;

/**
 * Utility class for XML manipulation.
 */
public class XmlUtils {
  /**
   * Custom XML exception for handling XML parsing errors.
   */
  public static class XmlUtilsException extends Exception {

    private static final long serialVersionUID = 1L;

    public XmlUtilsException(String string) {
      super(string);
    }

    public XmlUtilsException(String string, Exception e) {
      super(string, e);
    }

  }

  private static XmlMapper staticXmlMapper = new XmlMapper();

  static {
    var builder = new Builder(staticXmlMapper);
    builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    builder.defaultUseWrapper(true);
    staticXmlMapper = builder.build();
  }

  // The Stax factory under the XML mapper, which reads XML as a stream
  private static final XMLInputFactory xmlInputFactory =
      staticXmlMapper.getFactory().getXMLInputFactory();

  /**
   * Use static methods, don't instantiate
   */
  private XmlUtils() {
  }

  /**
   * Embeds the arrayNode into an ObjectNode with the given childKey. By default a
   * JSON array such as {"parent":[1, 2, 3,]} will be converted to:
   * <ObjectNode><parent>1</parent><parent>2</parent><parent>3</parent></ObjectNode>.
   * This is not often desired as there is no paren object to encompass the array.
   * By calling this method given childKey = "child" and arrayNode = [1, 2, 3,],
   * method will return {"parent":{"child":[1, 2, 3,]}} which as a result will be
   * encoded to
   * <ObjectNode><parent><child>1</child><child>2</child><child>3</child></parent></ObjectNode>.
   * Which is a more representative of the JSON ObjectNode.
   *
   * @param childKey The key to be given to the child array object
   * @param arrayNode The array node to be embedded in a ObjectNode
   * @return OBjectNode representation of the given arrayNode redy to be converted
   *         to XML
   */
  public static ObjectNode createEmbeddedJsonArrayForXmlConversion(String childKey, 
      JsonNode arrayNode) {
    ObjectNode childNode = staticXmlMapper.createObjectNode();
    childNode.set(childKey, arrayNode);
    return childNode;
  }

  /**
   * Find an element of an XML string by its name.  The XML is read as a stream, so the element is
   * found wherever it is, and its start and end tags may hold whitespace, attributes or a namespace
   * prefix.  The text of the element is returned exactly as it is in the XML, keeping the order and
   * structure of its content.  A namespace prefix declared on an ancestor of the element is not
   * declared in the text returned.
   *
   * @param xml The XML String to be searched
   * @param tagName The local name of the element, without a prefix
   * @return The text of the first element with the name, from its start tag to its end tag, or null
   *         if there is none
   * @throws IllegalArgumentException if the XML is not well formed before the end of the element
   */
  public static String findXmlContentString(String xml, String tagName) {
    XMLStreamReader2 reader = null;
    try {
      reader = (XMLStreamReader2) xmlInputFactory.createXMLStreamReader(new StringReader(xml));
      while (reader.hasNext()) {
        if (reader.next() == XMLStreamConstants.START_ELEMENT
            && tagName.equals(reader.getLocalName())) {
          final int start = (int) reader.getLocationInfo().getStartingCharOffset();
          // skip element: goes to the matching end element
          reader.skipElement();
          final int end = (int) reader.getLocationInfo().getEndingCharOffset();
          return xml.substring(start, end);
        }
      }
      return null;
    } catch (XMLStreamException e) {
      throw new IllegalArgumentException("Error reading XML to find element <" + tagName + ">", e);
    } finally {
      closeQuietly(reader);
    }
  }

  private static void closeQuietly(XMLStreamReader2 reader) {
    if (reader != null) {
      try {
        reader.closeCompletely();
      } catch (XMLStreamException e) {
        // Nothing to release for a string
      }
    }
  }

  /**
   * Static method to attempt to serialize an object into XML.
   *
   * @param o The object to be serialized
   * @return The serialized XML String
   * @throws XmlUtilsException Throws an exception when failing to serialize the object
   */
  public static String toXmlStatic(Object o) throws XmlUtilsException {
    String xml;
    try {
      xml = staticXmlMapper.writeValueAsString(o);
    } catch (Exception e) {
      throw new XmlUtilsException("Error encoding object to XML", e);
    }
    return xml;
  }

  /**
   * Static method to attempt to transform an XML String into a JSONObject.
   *
   * @param xml The xml String to be transformed
   * @return A JSONObject representing the XML
   * @throws XmlUtilsException Throws an exception when failing to transform into an JSONObject
   */
  public static JSONObject toJSONObject(String xml) throws XmlUtilsException {
    try {
      return XML.toJSONObject(xml, true);
    } catch (Exception e) {
      throw new XmlUtilsException("Error decoding " + xml + "to JSONObject", e);
    }
  }

}
