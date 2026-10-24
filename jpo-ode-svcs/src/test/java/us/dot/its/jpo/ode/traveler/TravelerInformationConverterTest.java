package us.dot.its.jpo.ode.traveler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import us.dot.its.jpo.asn.j2735.r2024.Common.Extent;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeOffsetPointXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.Position3D;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.ITIScodesAndTextSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DirectionOfUse;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DistanceUnits;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.MUTCDCode;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.NodeOffsetPointLL;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInfoType;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.traveler.TravelerInformationConverter.InvalidNodeLatLonOffsetException;
import us.dot.its.jpo.ode.traveler.TravelerInformationConverter.UnsupportedRegionException;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;

class TravelerInformationConverterTest {

  private static final String DEFAULT_REGION = "{\"name\":\"Testing TIM\",\"regulatorID\":\"0\",\"segmentID\":\"33\","
      + "\"anchorPosition\":{\"latitude\":\"41.2500807\",\"longitude\":\"-111.0093847\",\"elevation\":\"2020.6969900289998\"},"
      + "\"laneWidth\":\"7\",\"directionality\":\"3\",\"closedPath\":\"false\",\"direction\":\"0000000000001010\","
      + "\"description\":\"path\",\"path\":{\"scale\":\"0\",\"type\":\"ll\",\"nodes\":[]}}";

  private static OdeTravelerInformationMessage tim(String dataFrameFields, String region) throws JsonUtilsException {
    String json = "{\"msgCnt\":\"1\",\"timeStamp\":\"2017-08-03T22:25:36.297Z\",\"packetID\":\"EC9C236B0000000000\","
        + "\"urlB\":\"null\",\"dataframes\":[{\"startDateTime\":\"2017-08-02T22:25:00.000Z\",\"durationTime\":1,"
        + "\"doNotUse1\":0,\"doNotUse2\":0,\"doNotUse3\":0,\"doNotUse4\":0,\"frameType\":\"advisory\","
        + "\"msgId\":{\"roadSignID\":{\"position\":{\"latitude\":\"41.678473\",\"longitude\":\"-108.782775\","
        + "\"elevation\":\"917.1432\"},\"viewAngle\":\"1010101010101010\",\"mutcdCode\":\"warning\",\"crc\":\"0000\"}},"
        + "\"priority\":\"0\",\"regions\":[" + region + "],\"content\":\"advisory\",\"items\":[\"125\"],\"url\":\"null\""
        + dataFrameFields + "}]}";
    return (OdeTravelerInformationMessage) JsonUtils.jacksonFromJson(json, OdeTravelerInformationMessage.class, true);
  }

  private static TravelerDataFrame convertFrame(String dataFrameFields, String region) throws Exception {
    return TravelerInformationConverter.convert(tim(dataFrameFields, region)).getDataFrames().get(0);
  }

  private static GeographicalPath convertRegion(String region) throws Exception {
    return convertFrame("", region).getRegions().get(0);
  }

  private static String pathRegion(String type, String nodes) {
    return DEFAULT_REGION.replace("\"type\":\"ll\",\"nodes\":[]", "\"type\":\"" + type + "\",\"nodes\":[" + nodes + "]");
  }

  private static String llNode(BigDecimal lat, BigDecimal lon, String delta) {
    return "{\"nodeLat\":\"" + lat.toPlainString() + "\",\"nodeLong\":\"" + lon.toPlainString() + "\",\"delta\":\""
        + delta + "\"}";
  }

  private static String xyNode(String x, String y, String delta) {
    return "{\"x\":\"" + x + "\",\"y\":\"" + y + "\",\"delta\":\"" + delta + "\"}";
  }

  @Test
  void testTranslateIsoTimeStampToMinuteOfYear() {
    assertEquals(232800, TravelerInformationConverter.translateIsoTimeStampToMinuteOfYear("2018-06-11T16:00:00.000Z"));
    assertEquals(232800, TravelerInformationConverter.translateIsoTimeStampToMinuteOfYear("2018-06-11T10:00-06:00"));
    assertEquals(527040, TravelerInformationConverter.translateIsoTimeStampToMinuteOfYear("2018-15-44T25:66:77.999Z"));
  }

  @Test
  void testTravelerInformationFields() throws Exception {
    TravelerInformation result = TravelerInformationConverter.convert(tim("", DEFAULT_REGION));
    assertEquals(1, result.getMsgCnt().getValue());
    assertEquals(309505, result.getTimeStamp().getValue());
    assertEquals("EC9C236B0000000000", result.getPacketID().getValue());
    assertEquals("null", result.getUrlB().getValue());
    assertEquals(1, result.getDataFrames().size());
  }

  @Test
  void testEmptyDataFramesProducesEmptyList() throws Exception {
    OdeTravelerInformationMessage tim = tim("", DEFAULT_REGION);
    tim.setDataframes(null);
    TravelerInformation result = TravelerInformationConverter.convert(tim);
    assertNotNull(result.getDataFrames());
    assertTrue(result.getDataFrames().isEmpty());
  }

  @Test
  void testStartDateTimeConvertsToStartYearAndStartTime() throws Exception {
    TravelerDataFrame frame = convertFrame(",\"startDateTime\":\"2018-06-11T16:00:00.000Z\"", DEFAULT_REGION);
    assertEquals(2018, frame.getStartYear().getValue());
    assertEquals(232800, frame.getStartTime().getValue());

    frame = convertFrame(",\"startDateTime\":\"2018-06-11T10:00-06:00\"", DEFAULT_REGION);
    assertEquals(2018, frame.getStartYear().getValue());
    assertEquals(232800, frame.getStartTime().getValue());

    frame = convertFrame(",\"startDateTime\":\"2018-15-44T25:66:77.999Z\"", DEFAULT_REGION);
    assertEquals(0, frame.getStartYear().getValue());
    assertEquals(527040, frame.getStartTime().getValue());
  }

  @Test
  void testDataFrameFields() throws Exception {
    TravelerDataFrame frame = convertFrame("", DEFAULT_REGION);
    assertEquals(TravelerInfoType.ADVISORY, frame.getFrameType());
    assertEquals(1, frame.getDurationTime().getValue());
    assertEquals(0, frame.getPriority().getValue());
    assertEquals(0, frame.getDoNotUse1().getValue());
    assertEquals(0, frame.getDoNotUse4().getValue());
    assertEquals("null", frame.getUrl().getValue());
    var roadSignID = frame.getMsgId().getRoadSignID();
    assertEquals(416784730, roadSignID.getPosition().getLat().getValue());
    assertEquals(-1087827750, roadSignID.getPosition().getLong_().getValue());
    assertEquals(9171, roadSignID.getPosition().getElevation().getValue());
    assertEquals("1010101010101010", roadSignID.getViewAngle().binaryString());
    assertEquals(MUTCDCode.WARNING, roadSignID.getMutcdCode());
    assertEquals("0000", roadSignID.getCrc().getValue());
  }

  @Test
  void testFurtherInfoId() throws Exception {
    String fields = ",\"msgId\":{\"furtherInfoID\":\"ABCD\"}";
    TravelerDataFrame frame = convertFrame(fields, DEFAULT_REGION);
    assertNull(frame.getMsgId().getRoadSignID());
    assertEquals("ABCD", frame.getMsgId().getFurtherInfoID().getValue());
  }

  @Test
  void testItemsConvertToItisCodesAndText() throws Exception {
    TravelerDataFrame frame = convertFrame(",\"items\":[\"123\",\"'123\",\"test 123\"]", DEFAULT_REGION);
    var advisory = frame.getContent().getAdvisory();
    assertEquals(3, advisory.size());
    ITIScodesAndTextSequence.ItemChoice itis = advisory.get(0).getItem();
    assertEquals(123, itis.getItis().getValue());
    assertNull(itis.getText());
    assertEquals("123", advisory.get(1).getItem().getText().getValue());
    assertEquals("test 123", advisory.get(2).getItem().getText().getValue());
  }

  @Test
  void testContentTypes() throws Exception {
    assertNotNull(convertFrame(",\"content\":\"Advisory\"", DEFAULT_REGION).getContent().getAdvisory());
    assertEquals(125, convertFrame(",\"content\":\"workZone\"", DEFAULT_REGION)
        .getContent().getWorkZone().get(0).getItem().getItis().getValue());
    assertNotNull(convertFrame(",\"content\":\"genericSign\"", DEFAULT_REGION).getContent().getGenericSign());
    assertNotNull(convertFrame(",\"content\":\"speedLimit\"", DEFAULT_REGION).getContent().getSpeedLimit());
    assertEquals("text", convertFrame(",\"content\":\"exitService\",\"items\":[\"text\"]", DEFAULT_REGION)
        .getContent().getExitService().get(0).getItem().getText().getValue());
    TravelerDataFrame.ContentChoice unknown = convertFrame(",\"content\":\"unknown\"", DEFAULT_REGION).getContent();
    assertNull(unknown.getAdvisory());
    assertNull(unknown.getWorkZone());
  }

  @Test
  void testRegionFields() throws Exception {
    GeographicalPath region = convertRegion(DEFAULT_REGION);
    assertEquals("Testing TIM", region.getName().getValue());
    assertEquals(0, region.getId().getRegion().getValue());
    assertEquals(33, region.getId().getId().getValue());
    Position3D anchor = region.getAnchor();
    assertEquals(412500807, anchor.getLat().getValue());
    assertEquals(-1110093847, anchor.getLong_().getValue());
    assertEquals(20207, anchor.getElevation().getValue());
    assertEquals(700, region.getLaneWidth().getValue());
    assertEquals(DirectionOfUse.BOTH, region.getDirectionality());
    assertEquals(false, region.getClosedPath().getValue());
    assertEquals("0000000000001010", region.getDirection().binaryString());
  }

  @Test
  void testDirectionalityByOrdinalAndName() throws Exception {
    assertEquals(DirectionOfUse.FORWARD,
        convertRegion(DEFAULT_REGION.replace("\"directionality\":\"3\"", "\"directionality\":\"1\"")).getDirectionality());
    assertEquals(DirectionOfUse.REVERSE,
        convertRegion(DEFAULT_REGION.replace("\"directionality\":\"3\"", "\"directionality\":\"reverse\""))
            .getDirectionality());
  }

  @Test
  void testNodeLLWithSpecifiedType() throws Exception {
    GeographicalPath region = convertRegion(pathRegion("ll",
        "{\"nodeLong\":\"0.0031024\",\"nodeLat\":\"0.0014506\",\"delta\":\"node-LL3\"}"));
    NodeOffsetPointLL delta = region.getDescription().getPath().getOffset().getLl().getNodes().get(0).getDelta();
    assertEquals(14506, delta.getNode_LL3().getLat().getValue());
    assertEquals(31024, delta.getNode_LL3().getLon().getValue());
  }

  @Test
  void testNodeLLWithUnspecifiedType() throws Exception {
    GeographicalPath region = convertRegion(pathRegion("ll",
        "{\"nodeLong\":\"-0.0008192\",\"nodeLat\":\"-0.0013123\",\"delta\":\"node-LL\"}"));
    NodeOffsetPointLL delta = region.getDescription().getPath().getOffset().getLl().getNodes().get(0).getDelta();
    assertEquals(-13123, delta.getNode_LL3().getLat().getValue());
    assertEquals(-8192, delta.getNode_LL3().getLon().getValue());
  }

  @Test
  void testNodeLatLonInLLPath() throws Exception {
    GeographicalPath region = convertRegion(pathRegion("ll",
        "{\"nodeLong\":\"-111.0093847\",\"nodeLat\":\"41.2500807\",\"delta\":\"node-LatLon\"}"));
    NodeOffsetPointLL delta = region.getDescription().getPath().getOffset().getLl().getNodes().get(0).getDelta();
    assertEquals(412500807, delta.getNode_LatLon().getLat().getValue());
    assertEquals(-1110093847, delta.getNode_LatLon().getLon().getValue());
  }

  static Stream<Arguments> llOffsets() {
    return Stream.of(
        Arguments.of(-1500L, -1500L, "node-LL1"),
        Arguments.of(-2048L, -2047L, "node-LL2"),
        Arguments.of(-2047L, -2048L, "node-LL2"),
        Arguments.of(-6000L, -6000L, "node-LL2"),
        Arguments.of(-8192L, -8191L, "node-LL3"),
        Arguments.of(-8191L, -8192L, "node-LL3"),
        Arguments.of(-30000L, -30000L, "node-LL3"),
        Arguments.of(-32768L, -32767L, "node-LL4"),
        Arguments.of(-32767L, -32768L, "node-LL4"),
        Arguments.of(-130000L, -130000L, "node-LL4"),
        Arguments.of(-131072L, -131071L, "node-LL5"),
        Arguments.of(-131071L, -131072L, "node-LL5"),
        Arguments.of(-2000000L, -2000000L, "node-LL5"),
        Arguments.of(-2097152L, -2097151L, "node-LL6"),
        Arguments.of(-2097151L, -2097152L, "node-LL6"),
        Arguments.of(-8388607L, -8388607L, "node-LL6"),
        Arguments.of(8388607L, 8388607L, "node-LL6"));
  }

  @ParameterizedTest
  @MethodSource("llOffsets")
  void testNodeLLTypeSelection(long lat, long lon, String expectedType) throws Exception {
    GeographicalPath region = convertRegion(pathRegion("ll",
        llNode(BigDecimal.valueOf(lat, 7), BigDecimal.valueOf(lon, 7), "node-LL")));
    NodeOffsetPointLL delta = region.getDescription().getPath().getOffset().getLl().getNodes().get(0).getDelta();
    Object point = switch (expectedType) {
      case "node-LL1" -> delta.getNode_LL1();
      case "node-LL2" -> delta.getNode_LL2();
      case "node-LL3" -> delta.getNode_LL3();
      case "node-LL4" -> delta.getNode_LL4();
      case "node-LL5" -> delta.getNode_LL5();
      default -> delta.getNode_LL6();
    };
    assertNotNull(point, "Expected " + expectedType + " for " + lat + "/" + lon);
  }

  @Test
  void testNodeLLOutOfRangeThrows() {
    InvalidNodeLatLonOffsetException e = assertThrows(InvalidNodeLatLonOffsetException.class,
        () -> convertRegion(pathRegion("ll",
            llNode(BigDecimal.valueOf(-8388608L, 7), BigDecimal.valueOf(-8388607L, 7), "node-LL"))));
    assertEquals("Invalid node lat/long offset: -8388608/-8388607. Values must be within a range of "
        + "-0.8388607/+0.8388607 degrees.", e.getMessage());
    assertThrows(InvalidNodeLatLonOffsetException.class, () -> convertRegion(pathRegion("ll",
        llNode(BigDecimal.valueOf(-8388607L, 7), BigDecimal.valueOf(-8388608L, 7), "node-LL"))));
  }

  static Stream<Arguments> xyOffsets() {
    return Stream.of(
        Arguments.of("5.11", "0", "node-XY1", 511L),
        Arguments.of("10.23", "-10.23", "node-XY1", 1023L),
        Arguments.of("10.24", "0", "node-XY2", 1024L),
        Arguments.of("20.48", "0", "node-XY3", 2048L),
        Arguments.of("40.96", "0", "node-XY4", 4096L),
        Arguments.of("81.92", "0", "node-XY5", 8192L),
        Arguments.of("163.84", "0", "node-XY6", 16384L),
        Arguments.of("327.67", "-327.67", "node-XY6", 32767L));
  }

  @ParameterizedTest
  @MethodSource("xyOffsets")
  void testNodeXYTypeSelection(String x, String y, String expectedType, long expectedX) throws Exception {
    GeographicalPath region = convertRegion(pathRegion("xy", xyNode(x, y, "node-XY")));
    NodeOffsetPointXY delta = region.getDescription().getPath().getOffset().getXy().getNodes().get(0).getDelta();
    long actualX = switch (expectedType) {
      case "node-XY1" -> delta.getNode_XY1().getX().getValue();
      case "node-XY2" -> delta.getNode_XY2().getX().getValue();
      case "node-XY3" -> delta.getNode_XY3().getX().getValue();
      case "node-XY4" -> delta.getNode_XY4().getX().getValue();
      case "node-XY5" -> delta.getNode_XY5().getX().getValue();
      default -> delta.getNode_XY6().getX().getValue();
    };
    assertEquals(expectedX, actualX);
  }

  @Test
  void testNodeXYOutOfRangeThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> convertRegion(pathRegion("xy", xyNode("655.36", "0", "node-XY"))));
  }

  @Test
  void testNodeXYWithSpecifiedTypeAndAttributes() throws Exception {
    GeographicalPath region = convertRegion(pathRegion("xy",
        "{\"x\":\"1.5\",\"y\":\"-2.5\",\"delta\":\"node-XY3\",\"attributes\":{\"dWidth\":\"0.5\"}}"));
    var node = region.getDescription().getPath().getOffset().getXy().getNodes().get(0);
    assertEquals(150, node.getDelta().getNode_XY3().getX().getValue());
    assertEquals(-250, node.getDelta().getNode_XY3().getY().getValue());
    assertNotNull(node.getAttributes());
  }

  @Test
  void testGeometryWithCircle() throws Exception {
    String region = DEFAULT_REGION.replace("\"description\":\"path\",\"path\":{\"scale\":\"0\",\"type\":\"ll\",\"nodes\":[]}",
        "\"description\":\"geometry\",\"geometry\":{\"direction\":\"1111000011110000\",\"extent\":\"5\","
            + "\"laneWidth\":\"3.66\",\"circle\":{\"position\":{\"latitude\":\"41.153593\",\"longitude\":\"-104.655785\","
            + "\"elevation\":\"1824\"},\"radius\":\"50\",\"units\":\"1\"}}");
    var geometry = convertRegion(region).getDescription().getGeometry();
    assertEquals("1111000011110000", geometry.getDirection().binaryString());
    assertEquals(Extent.USEFOR500METERS, geometry.getExtent());
    assertEquals(366, geometry.getLaneWidth().getValue());
    assertEquals(411535930, geometry.getCircle().getCenter().getLat().getValue());
    assertEquals(18240, geometry.getCircle().getCenter().getElevation().getValue());
    assertEquals(50, geometry.getCircle().getRadius().getValue());
    assertEquals(DistanceUnits.CM2_5, geometry.getCircle().getUnits());
  }

  @Test
  void testOldRegionWithCircle() throws Exception {
    String region = DEFAULT_REGION.replace("\"description\":\"path\",\"path\":{\"scale\":\"0\",\"type\":\"ll\",\"nodes\":[]}",
        "\"description\":\"oldRegion\",\"oldRegion\":{\"direction\":\"1010101010101010\",\"extent\":\"useFor3meters\","
            + "\"area\":{\"circle\":{\"center\":{\"latitude\":\"41.678473\",\"longitude\":\"-108.782775\","
            + "\"elevation\":\"917.1432\"},\"radius\":\"2048\",\"units\":\"centimeter\"}}}");
    var oldRegion = convertRegion(region).getDescription().getOldRegion();
    assertEquals(Extent.USEFOR3METERS, oldRegion.getExtent());
    assertEquals(416784730, oldRegion.getArea().getCircle().getCenter().getLat().getValue());
    assertEquals(DistanceUnits.CENTIMETER, oldRegion.getArea().getCircle().getUnits());
  }

  @Test
  void testOldRegionWithUnsupportedAreaThrows() {
    String region = DEFAULT_REGION.replace("\"description\":\"path\",\"path\":{\"scale\":\"0\",\"type\":\"ll\",\"nodes\":[]}",
        "\"description\":\"oldRegion\",\"oldRegion\":{\"direction\":\"1010101010101010\",\"extent\":\"1\","
            + "\"area\":{\"regionPoint\":{\"position\":{\"latitude\":\"41.678473\",\"longitude\":\"-108.782775\"},"
            + "\"scale\":0,\"regionList\":[]}}}");
    assertThrows(UnsupportedRegionException.class, () -> convertRegion(region));
  }

  @Test
  void testUnsupportedDescriptionThrows() {
    UnsupportedRegionException e = assertThrows(UnsupportedRegionException.class,
        () -> convertRegion(DEFAULT_REGION.replace("\"description\":\"path\"", "\"description\":\"foo\"")));
    assertEquals("Unsupported region description: foo", e.getMessage());
  }

  @Test
  void testRegionWithoutDescription() throws Exception {
    GeographicalPath region = convertRegion(DEFAULT_REGION.replace(
        ",\"description\":\"path\",\"path\":{\"scale\":\"0\",\"type\":\"ll\",\"nodes\":[]}", ""));
    assertNull(region.getDescription());
  }
}
