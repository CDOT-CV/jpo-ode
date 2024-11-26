package us.dot.its.jpo.ode.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import us.dot.its.jpo.ode.util.JsonUtils;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

public class OdeTimDataTest {
    private static final String SCHEMA_VERSION = "7";
    private static final String ASN1_STRING = "001F80AE70165E87AD5DB73EE53601D49C0F775D9B0B01C266509C496663068FFFF93F43448C001EA007F95937EAD35AC9A5FA54EADF62C17316CB99385CDA00000000266509C4966630689388C200021000EBE86F264E051097630004008027BBAECD8C070999427125998C1A3FFFE4FD0D1230007A801FE564DFAB4D6B2697E953AB7D8B05CC5B2E64E1736800000000999427125998C1A24E230800084003AFA1BC993814425D8C0000003023DDD766C0913189880FB96879A18B9BEE7183450F963D09BEACCD8A5B06FB639381F59F27808066C418702727350EEAB14E752EA27C0AF0540260F9187757E2192FECE54EF4F0032D653EFB5938AE340F6D3122636E7F61F9BD3A1CB5B4695B634BFFADD4018543C5862B4D608430EA9F2FCE99599935425B5DD64F77EC1495571FB50D0B03FF442529D80EA12704C08088E30204E84B3BD03B6001002009EEEBB360A4000801004F775D9B00C1265E25E2A8F3809BC14F804485CC84244966B611857569128444CB48453B9DC84ACCB06E225C6B7C36122F9B9E008F85DC55C42092CD4611CE5A3B107BFB25F0864A19C5049086E7A024071790208BE4FAF8D63F87FC1027B83FF235FF8DFFC6116EAC028093B920DF0468311194237188767113D84037081D31FE2C6B3443FED434C4F1FF3311F183E46892F5DE8D049958EF3E21B27675C100";

    private static final String json = String.format("{\"metadata\":{\"logFileName\":\"\",\"recordType\":\"timMsg\",\"securityResultCode\":\"success\",\"receivedMessageDetails\":{\"rxSource\":\"NA\"},\"payloadType\":\"us.dot.its.jpo.ode.model.OdeTimPayload\",\"serialId\":{\"streamId\":\"89b9de68-7e91-4491-886f-d95276b67907\",\"bundleSize\":1,\"bundleId\":0,\"recordId\":0,\"serialNumber\":0},\"odeReceivedAt\":\"2024-11-26T07:29:12.257Z\",\"schemaVersion\":7,\"maxDurationTime\":0,\"recordGeneratedAt\":\"\",\"recordGeneratedBy\":\"RSU\",\"sanitized\":false,\"odePacketID\":\"\",\"odeTimStartDateTime\":\"\",\"asn1\":\"001F80AE70165E87AD5DB73EE53601D49C0F775D9B0B01C266509C496663068FFFF93F43448C001EA007F95937EAD35AC9A5FA54EADF62C17316CB99385CDA00000000266509C4966630689388C200021000EBE86F264E051097630004008027BBAECD8C070999427125998C1A3FFFE4FD0D1230007A801FE564DFAB4D6B2697E953AB7D8B05CC5B2E64E1736800000000999427125998C1A24E230800084003AFA1BC993814425D8C0000003023DDD766C0913189880FB96879A18B9BEE7183450F963D09BEACCD8A5B06FB639381F59F27808066C418702727350EEAB14E752EA27C0AF0540260F9187757E2192FECE54EF4F0032D653EFB5938AE340F6D3122636E7F61F9BD3A1CB5B4695B634BFFADD4018543C5862B4D608430EA9F2FCE99599935425B5DD64F77EC1495571FB50D0B03FF442529D80EA12704C08088E30204E84B3BD03B6001002009EEEBB360A4000801004F775D9B00C1265E25E2A8F3809BC14F804485CC84244966B611857569128444CB48453B9DC84ACCB06E225C6B7C36122F9B9E008F85DC55C42092CD4611CE5A3B107BFB25F0864A19C5049086E7A024071790208BE4FAF8D63F87FC1027B83FF235FF8DFFC6116EAC028093B920DF0468311194237188767113D84037081D31FE2C6B3443FED434C4F1FF3311F183E46892F5DE8D049958EF3E21B27675C100\",\"originIp\":\"172.18.0.1\"},\"payload\":{\"dataType\":\"us.dot.its.jpo.ode.plugin.j2735.J2735Tim\",\"data\":{\"msgCnt\":\"1\",\"timeStamp\":\"417415\",\"packetID\":\"AD5DB73EE53601D49C\",\"dataFrames\":[{\"notUsed\":\"0\",\"frameType\":\"advisory\",\"msgId\":{\"roadSignID\":{\"position\":{\"latitude\":38.8311689,\"longitude\":-104.8408366},\"viewAngle\":\"1111111111111111\",\"mutcdCode\":\"warning\"}},\"startYear\":\"2024\",\"startTime\":\"428312\",\"durationTime\":\"30\",\"priority\":\"5\",\"notUsed1\":\"0\",\"regions\":[{\"name\":\"I_US-24_RSU_10.16.28.6\",\"id\":{\"region\":0,\"id\":0},\"anchor\":{\"latitude\":38.8311689,\"longitude\":-104.8408366},\"laneWidth\":5000,\"directionality\":\"both\",\"closedPath\":false,\"description\":{\"path\":{\"scale\":0,\"offset\":{\"ll\":{\"nodes\":[{\"delta\":{\"nodeLL1\":{\"lon\":1726,\"lat\":111}}},{\"delta\":{\"nodeLL5\":{\"lon\":1208360,\"lat\":77510}}}]}}}}}],\"notUsed2\":\"0\",\"notUsed3\":\"0\",\"content\":{\"workZone\":{\"SEQUENCE\":[{\"item\":{\"itis\":\"1025\"}}]}}},{\"notUsed\":\"0\",\"frameType\":\"advisory\",\"msgId\":{\"roadSignID\":{\"position\":{\"latitude\":38.8311689,\"longitude\":-104.8408366},\"viewAngle\":\"1111111111111111\",\"mutcdCode\":\"warning\"}},\"startYear\":\"2024\",\"startTime\":\"428312\",\"durationTime\":\"30\",\"priority\":\"5\",\"notUsed1\":\"0\",\"regions\":[{\"name\":\"I_US-24_RSU_10.16.28.6\",\"id\":{\"region\":0,\"id\":0},\"anchor\":{\"latitude\":38.8311689,\"longitude\":-104.8408366},\"laneWidth\":5000,\"directionality\":\"both\",\"closedPath\":false,\"description\":{\"path\":{\"scale\":0,\"offset\":{\"ll\":{\"nodes\":[{\"delta\":{\"nodeLL1\":{\"lon\":1726,\"lat\":111}}},{\"delta\":{\"nodeLL5\":{\"lon\":1208360,\"lat\":77510}}}]}}}}}],\"notUsed2\":\"0\",\"notUsed3\":\"0\",\"content\":{\"advisory\":{\"SEQUENCE\":[{\"item\":{\"itis\":\"770\"}}]}}}]}}}", SCHEMA_VERSION, ASN1_STRING);

    //
    // Note that OdeTimData does not have annotations to support deserialization, so serialization/deserialization is not tested here.
    //

    @Test
    public void shouldValidateJson() throws Exception {
        // Load json schema from resource
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        final JsonSchema schema = factory.getSchema(getClass().getClassLoader().getResource("schemas/schema-tim.json").toURI());
        final JsonNode node = (JsonNode)JsonUtils.fromJson(json, JsonNode.class);
        Set<ValidationMessage> validationMessages = schema.validate(node);
        assertEquals(String.format("Json validation errors: %s", validationMessages), 0, validationMessages.size());
    }
}
