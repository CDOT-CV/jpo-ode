package us.dot.its.jpo.ode.util;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.util.XmlUtils.XmlUtilsException;

public class XmlUtilsTest {

    @Test
    public void testToJsonObject() throws XmlUtilsException {
        var myXml = "<recordId>5651E543</recordId>";
        var json = XmlUtils.toJSONObject(myXml);
        var parsedRecordId = json.getString("recordId");
        // JSON shouldn't be converted to scientific notation
        assertEquals("5651E543", parsedRecordId);
    }

    private static String inWrapper(String element) {
        return "<OdeAsn1Data><metadata><a>1</a></metadata><payload><data>" + element
            + "</data></payload></OdeAsn1Data>";
    }

    @Test
    void findXmlContentStringPlain() {
        var element = "<MessageFrame><messageId>31</messageId><value/></MessageFrame>";
        assertEquals(element, XmlUtils.findXmlContentString(inWrapper(element), "MessageFrame"));
    }

    @Test
    void findXmlContentStringWhitespaceInTags() {
        var element = "<MessageFrame\n  ><messageId>31</messageId></MessageFrame >";
        assertEquals(element, XmlUtils.findXmlContentString(inWrapper(element), "MessageFrame"));
    }

    @Test
    void findXmlContentStringAttributes() {
        var element = "<MessageFrame a=\"x>y\" b='&lt;/MessageFrame>'><messageId>31</messageId></MessageFrame>";
        assertEquals(element, XmlUtils.findXmlContentString(inWrapper(element), "MessageFrame"));
    }

    @Test
    void findXmlContentStringPrefixed() {
        var element = "<j:MessageFrame xmlns:j=\"urn:j2735\"><j:messageId>31</j:messageId></j:MessageFrame>";
        assertEquals(element, XmlUtils.findXmlContentString(inWrapper(element), "MessageFrame"));
    }

    @Test
    void findXmlContentStringSelfClosing() {
        var element = "<MessageFrame />";
        assertEquals(element, XmlUtils.findXmlContentString(inWrapper(element + "<after/>"),
            "MessageFrame"));
    }

    @Test
    void findXmlContentStringNestedSameName() {
        var element = "<MessageFrame><x><MessageFrame>inner</MessageFrame></x><y>2</y></MessageFrame>";
        assertEquals(element, XmlUtils.findXmlContentString(inWrapper(element), "MessageFrame"));
    }

    @Test
    void findXmlContentStringSkipsCommentAndCdata() {
        var element = "<MessageFrame><messageId>31</messageId></MessageFrame>";
        var xml = inWrapper("<!-- <MessageFrame>comment</MessageFrame> -->"
            + "<c><![CDATA[<MessageFrame>cdata</MessageFrame>]]></c>" + element);
        assertEquals(element, XmlUtils.findXmlContentString(xml, "MessageFrame"));
    }

    @Test
    void findXmlContentStringCrlfAndNonAscii() {
        var element = "<MessageFrame>\r\n  <name>Ünïcødé 🚗</name>\r\n</MessageFrame>";
        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<root>\r\n<n>héllo 🚗</n>\r\n"
            + element + "\r\n</root>";
        assertEquals(element, XmlUtils.findXmlContentString(xml, "MessageFrame"));
    }

    @Test
    void findXmlContentStringKeepsOrderOfRepeatedSiblings() {
        var element = "<MessageFrame><items><a>1</a><b>2</b><a>3</a><b>4</b><a>5</a></items></MessageFrame>";
        assertEquals(element, XmlUtils.findXmlContentString(inWrapper(element), "MessageFrame"));
    }

    @Test
    void findXmlContentStringNotFound() {
        assertNull(XmlUtils.findXmlContentString(inWrapper("<other/>"), "MessageFrame"));
    }

    @Test
    void findXmlContentStringNotFoundIgnoresAttributeName() {
        assertNull(XmlUtils.findXmlContentString(inWrapper("<other MessageFrame=\"1\"/>"),
            "MessageFrame"));
    }

    @Test
    void findXmlContentStringMalformed() {
        assertThrows(IllegalArgumentException.class,
            () -> XmlUtils.findXmlContentString("<root><MessageFrame><a></MessageFrame></root>",
                "MessageFrame"));
    }
}
