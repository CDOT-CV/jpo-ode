package us.dot.its.jpo.ode.services.asn1.message;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.model.Asn1Encoding.EncodingRule;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeAsn1Payload;
import us.dot.its.jpo.ode.model.OdeMapMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = OdeKafkaProperties.class)
class Asn1DecodeMAPJSONTest {
    private final String json = "{\"metadata\":{\"recordType\":\"mapTx\",\"securityResultCode\":\"success\",\"payloadType\":\"us.dot.its.jpo.ode.model.OdeAsn1Payload\",\"serialId\":{\"streamId\":\"b91c5c0f-1c42-457e-b7c5-54505c942667\",\"bundleSize\":1,\"bundleId\":0,\"recordId\":0,\"serialNumber\":0},\"odeReceivedAt\":\"2024-03-15T19:04:47.440601200Z\",\"schemaVersion\":6,\"maxDurationTime\":0,\"recordGeneratedBy\":\"RSU\",\"sanitized\":false,\"mapSource\":\"RSU\",\"originIp\":\"192.168.0.1\"},\"payload\":{\"dataType\":\"us.dot.its.jpo.ode.model.OdeHexByteArray\",\"data\":{\"bytes\":\"03810040038081B10012839338023000205E96094D40DF4C2CA626C8516E02DC3C2010640000000289E01C009F603F42E88039900000000A41107B027D80FD0A4200C6400000002973021C09F603DE0C16029200000080002A8A008D027D98FEE805404FB0E1085F60588200028096021200000080002AA0007D027D98FE9802E04FB1200C214456228000A02B1240005022C03240000020000D56B40BC04FB35FF655E2C09F623FB81C835FEC0DB240A0A2BFF4AEBF82C660000804B0089000000800025670034013ECD7FB9578E027D9AFF883C4E050515FFA567A41635000040258024800000400012B8F81F409F663FAC094013ECD7FC83DDB02829AFFA480BC04FB02C6E0000804B09C5000000200035EA98A9604F60DA6C7C113D505C35FFE941D409F65C05034C050500C9880004409BC800000006D2BD3CEC813C40CDE062C1FD400000200008791EA3DB3CF380A009F666F05005813D80FFE0A0588C00040092106A00000000BC75CAC009F66DB54C04A813D80A100801241ED40000000078EBAE3B6DA7A008809E2050904008811F100000000BC72389009F60ECA8002049C400000002F1B2CA3027D93A71FA813EC204BC400000002F1B2B34027B0397608880CD10000000039B8E1A51036820505080D51000000003A7461ED1036760505080DD1000000003B2F62311006260505160BCA00000080002B785E2A80A0A6C028DE728145037F1F9E456488000202B2540001022C1894000001000057058C5B81414D806DBCD4028A18F4DF23A050502C8D0000404B05A5000000800035B6471BC05053602431F380A2864087BDB0141458064AB0D6C00053FC013EC0B0680006012C15940000020000D6C06C6581414D807FB972028A1901D78DC050536020EC1800A0A6C039D639813D80B0780006012C1494000002000096AB8C6581414D8062BE32028A1B01417E04050A360172D77009E2058440003009409C200000040006B3486A480A0A1CAB7134C8117DCC02879B018FAE2C050F3601CED54809E21012720000000067FBAD0007E7E84045C80000000100661580958004041C8000000019F3658401CDFA2C0D64000002000144016C02C36DDFFF0282984ACC1EE05052C36F0AC02828669D82DA8F821480A0A10F140002C8E0001004B03190000008000519FD190C43B2E0066108B08401428C342A0CE02828258A0604A6BE959AEE0E6050502C920001004B02D90000008000459FA164404FB30A8580A00A14619C306701414C32CE10E02829659081F814141029030164B0000802E8000802000035FDB1D84C09EC6C003BA14814140B0540003012C187400040080011B13F6EDB804F115FA6DFC10AFC94FC6A57EE07DCE2BFA7BED3B5FFCD72E80A1E018C900008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000\"}}}";

    @Autowired
    OdeKafkaProperties odeKafkaProperties;

    @Test
    void testConstructor() {
        OdeProperties properties = new OdeProperties();
        assertEquals("topic.OdeRawEncodedMAPJson", properties.getKafkaTopicOdeRawEncodedMAPJson());
    }

    @Test
    void testProcess() throws JSONException {
        OdeProperties properties = new OdeProperties();
        Asn1DecodeMAPJSON testDecodeMapJson = new Asn1DecodeMAPJSON(properties, odeKafkaProperties);

        OdeAsn1Data resultOdeObj = testDecodeMapJson.process(json);

        // Validate the metadata
        OdeMapMetadata jsonMetadataObj = (OdeMapMetadata) resultOdeObj.getMetadata();
        assertEquals(OdeMapMetadata.MapSource.RSU, jsonMetadataObj.getMapSource());
        assertEquals("unsecuredData", jsonMetadataObj.getEncodings().get(0).getElementName());
        assertEquals("MessageFrame", jsonMetadataObj.getEncodings().get(0).getElementType());
        assertEquals(EncodingRule.UPER, jsonMetadataObj.getEncodings().get(0).getEncodingRule());

        // Validate the payload
        String expectedPayload = "{\"bytes\":\"0012839338023000205E96094D40DF4C2CA626C8516E02DC3C2010640000000289E01C009F603F42E88039900000000A41107B027D80FD0A4200C6400000002973021C09F603DE0C16029200000080002A8A008D027D98FEE805404FB0E1085F60588200028096021200000080002AA0007D027D98FE9802E04FB1200C214456228000A02B1240005022C03240000020000D56B40BC04FB35FF655E2C09F623FB81C835FEC0DB240A0A2BFF4AEBF82C660000804B0089000000800025670034013ECD7FB9578E027D9AFF883C4E050515FFA567A41635000040258024800000400012B8F81F409F663FAC094013ECD7FC83DDB02829AFFA480BC04FB02C6E0000804B09C5000000200035EA98A9604F60DA6C7C113D505C35FFE941D409F65C05034C050500C9880004409BC800000006D2BD3CEC813C40CDE062C1FD400000200008791EA3DB3CF380A009F666F05005813D80FFE0A0588C00040092106A00000000BC75CAC009F66DB54C04A813D80A100801241ED40000000078EBAE3B6DA7A008809E2050904008811F100000000BC72389009F60ECA8002049C400000002F1B2CA3027D93A71FA813EC204BC400000002F1B2B34027B0397608880CD10000000039B8E1A51036820505080D51000000003A7461ED1036760505080DD1000000003B2F62311006260505160BCA00000080002B785E2A80A0A6C028DE728145037F1F9E456488000202B2540001022C1894000001000057058C5B81414D806DBCD4028A18F4DF23A050502C8D0000404B05A5000000800035B6471BC05053602431F380A2864087BDB0141458064AB0D6C00053FC013EC0B0680006012C15940000020000D6C06C6581414D807FB972028A1901D78DC050536020EC1800A0A6C039D639813D80B0780006012C1494000002000096AB8C6581414D8062BE32028A1B01417E04050A360172D77009E2058440003009409C200000040006B3486A480A0A1CAB7134C8117DCC02879B018FAE2C050F3601CED54809E21012720000000067FBAD0007E7E84045C80000000100661580958004041C8000000019F3658401CDFA2C0D64000002000144016C02C36DDFFF0282984ACC1EE05052C36F0AC02828669D82DA8F821480A0A10F140002C8E0001004B03190000008000519FD190C43B2E0066108B08401428C342A0CE02828258A0604A6BE959AEE0E6050502C920001004B02D90000008000459FA164404FB30A8580A00A14619C306701414C32CE10E02829659081F814141029030164B0000802E8000802000035FDB1D84C09EC6C003BA14814140B0540003012C187400040080011B13F6EDB804F115FA6DFC10AFC94FC6A57EE07DCE2BFA7BED3B5FFCD72E80A1E018C90000800\"}";
        OdeAsn1Payload jsonPayloadObj = (OdeAsn1Payload) resultOdeObj.getPayload();
        assertEquals("us.dot.its.jpo.ode.model.OdeHexByteArray", jsonPayloadObj.getDataType());
        assertEquals(expectedPayload, jsonPayloadObj.getData().toString());
    }
}
