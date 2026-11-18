package us.dot.its.jpo.ode.traveler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import us.dot.its.jpo.asn.j2735.r2024.Common.Extent;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeOffsetPointXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.Position3D;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.ITIScodesAndText;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DirectionOfUse;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DistanceUnits;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeometricProjection;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.MUTCDCode;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.NodeOffsetPointLL;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.RoadSignID;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInfoType;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ValidRegion;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.traveler.TravelerInformationConverter.InvalidNodeLatLonOffsetException;
import us.dot.its.jpo.ode.traveler.TravelerInformationConverter.UnsupportedRegionException;
import us.dot.its.jpo.ode.util.JsonUtils;

/**
 * Unit tests for {@link TravelerInformationConverter}. It converts the TIM in a deposit request
 * ({@link OdeTravelerInformationMessage}) into an r2024 {@link TravelerInformation}.
 *
 * <p>Each test starts from {@link #BASE_TIM}, which has one data frame and one region. The test
 * changes only the fields it is about, converts the TIM, and checks the result.</p>
 *
 * <p>The request uses human units; the result uses ASN.1 units:</p>
 * <ul>
 *   <li>latitude, longitude and node-LL offsets: degrees × 10^7;</li>
 *   <li>elevation: metres × 10;</li>
 *   <li>lane width and node-XY offsets: metres × 100;</li>
 *   <li>times: minute of the year, where 527040 means unknown.</li>
 * </ul>
 */
class TravelerInformationConverterTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** A valid deposit-request TIM: one advisory data frame with one path region. */
  private static final String BASE_TIM = """
      {
        "msgCnt": "1",
        "timeStamp": "2017-08-03T22:25:36.297Z",
        "packetID": "EC9C236B0000000000",
        "urlB": "null",
        "dataframes": [
          {
            "doNotUse1": 0,
            "frameType": "advisory",
            "msgId": {
              "roadSignID": {
                "position": {
                  "latitude": "41.678473",
                  "longitude": "-108.782775",
                  "elevation": "917.1432"
                },
                "viewAngle": "1010101010101010",
                "mutcdCode": "warning",
                "crc": "0000"
              }
            },
            "startDateTime": "2017-08-02T22:25:00.000Z",
            "durationTime": 1,
            "priority": "0",
            "doNotUse2": 0,
            "regions": [
              {
                "name": "Testing TIM",
                "regulatorID": "0",
                "segmentID": "33",
                "anchorPosition": {
                  "latitude": "41.2500807",
                  "longitude": "-111.0093847",
                  "elevation": "2020.6969900289998"
                },
                "laneWidth": "7",
                "directionality": "3",
                "closedPath": "false",
                "direction": "0000000000001010",
                "description": "path",
                "path": {
                  "scale": "0",
                  "type": "ll",
                  "nodes": []
                }
              }
            ],
            "doNotUse3": 0,
            "doNotUse4": 0,
            "content": "advisory",
            "items": ["125"],
            "url": "null"
          }
        ]
      }
      """;

  // ---- Building the request TIM ----

  /** A fresh, editable copy of {@link #BASE_TIM}. */
  private static ObjectNode baseTim() {
    return (ObjectNode) json(BASE_TIM);
  }

  private static ObjectNode dataFrame(ObjectNode tim) {
    return (ObjectNode) tim.get("dataframes").get(0);
  }

  private static ObjectNode region(ObjectNode tim) {
    return (ObjectNode) dataFrame(tim).get("regions").get(0);
  }

  /** Replaces the region's {@code path} description with another kind, e.g. a geometry. */
  private static void describeRegionAs(ObjectNode region, String description, String descriptionJson) {
    region.remove("path");
    region.put("description", description);
    region.set(description, json(descriptionJson));
  }

  /** Sets the region's path to one node of the given path type ({@code ll} or {@code xy}). */
  private static void setPathNode(ObjectNode region, String pathType, JsonNode node) {
    ObjectNode path = (ObjectNode) region.get("path");
    path.put("type", pathType);
    path.putArray("nodes").add(node);
  }

  /** A path node with a lat/long offset in degrees. */
  private static ObjectNode llNode(String delta, BigDecimal latOffset, BigDecimal longOffset) {
    return MAPPER.createObjectNode()
        .put("delta", delta)
        .put("nodeLat", latOffset.toPlainString())
        .put("nodeLong", longOffset.toPlainString());
  }

  /** A path node with an x/y offset in metres. */
  private static ObjectNode xyNode(String delta, String x, String y) {
    return MAPPER.createObjectNode()
        .put("delta", delta)
        .put("x", x)
        .put("y", y);
  }

  private static JsonNode json(String text) {
    try {
      return MAPPER.readTree(text);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Bad JSON in test: " + text, e);
    }
  }

  // ---- Converting ----

  /** Reads the TIM the way the depositor does, then converts it. */
  private static TravelerInformation convert(ObjectNode tim) throws Exception {
    OdeTravelerInformationMessage message = (OdeTravelerInformationMessage) JsonUtils.jacksonFromJson(
        tim.toString(), OdeTravelerInformationMessage.class, true);
    return TravelerInformationConverter.convert(message);
  }

  private static TravelerDataFrame convertedDataFrame(ObjectNode tim) throws Exception {
    return convert(tim).getDataFrames().get(0);
  }

  private static GeographicalPath convertedRegion(ObjectNode tim) throws Exception {
    return convertedDataFrame(tim).getRegions().get(0);
  }

  private static NodeOffsetPointLL convertedLLNodeOffset(ObjectNode tim) throws Exception {
    return convertedRegion(tim).getDescription().getPath().getOffset().getLl().getNodes().get(0)
        .getDelta();
  }

  private static NodeXY convertedXYNode(ObjectNode tim) throws Exception {
    return convertedRegion(tim).getDescription().getPath().getOffset().getXy().getNodes().get(0);
  }

  // ---- TravelerInformation ----

  @Test
  void isoTimestampConvertsToMinuteOfYear() {
    // June 11 is day 162: 161 days * 1440 + 16 h * 60 = 232800.
    assertEquals(232800, TravelerInformationConverter.translateIsoTimeStampToMinuteOfYear("2018-06-11T16:00:00.000Z"));
    assertEquals(232800, TravelerInformationConverter.translateIsoTimeStampToMinuteOfYear("2018-06-11T10:00-06:00"));
    assertEquals(527040, TravelerInformationConverter.translateIsoTimeStampToMinuteOfYear("2018-15-44T25:66:77.999Z"),
        "an invalid timestamp gives the 'unknown' minute");
  }

  @Test
  void convertsTopLevelFields() throws Exception {
    TravelerInformation result = convert(baseTim());

    assertEquals(1, result.getMsgCnt().getValue());
    assertEquals(309505, result.getTimeStamp().getValue(), "2017-08-03T22:25 as minute of the year");
    assertEquals("EC9C236B0000000000", result.getPacketID().getValue());
    assertEquals("null", result.getUrlB().getValue());
    assertEquals(1, result.getDataFrames().size());
  }

  @Test
  void missingDataFramesConvertToEmptyList() throws Exception {
    ObjectNode tim = baseTim();
    tim.remove("dataframes");

    TravelerInformation result = convert(tim);

    assertNotNull(result.getDataFrames());
    assertTrue(result.getDataFrames().isEmpty());
  }

  // ---- Data frame ----

  @Test
  void convertsDataFrameFieldsAndRoadSignId() throws Exception {
    TravelerDataFrame frame = convertedDataFrame(baseTim());

    assertEquals(TravelerInfoType.ADVISORY, frame.getFrameType());
    assertEquals(1, frame.getDurationTime().getValue());
    assertEquals(0, frame.getPriority().getValue());
    assertEquals(0, frame.getDoNotUse1().getValue());
    assertEquals(0, frame.getDoNotUse4().getValue());
    assertEquals("null", frame.getUrl().getValue());

    RoadSignID roadSignID = frame.getMsgId().getRoadSignID();
    assertEquals(416784730, roadSignID.getPosition().getLat().getValue());
    assertEquals(-1087827750, roadSignID.getPosition().getLong_().getValue());
    assertEquals(9171, roadSignID.getPosition().getElevation().getValue(), "917.1432 m, rounded");
    assertEquals("1010101010101010", roadSignID.getViewAngle().binaryString());
    assertEquals(MUTCDCode.WARNING, roadSignID.getMutcdCode());
    assertEquals("0000", roadSignID.getCrc().getValue());
  }

  @Test
  void startDateTimeConvertsToStartYearAndStartTime() throws Exception {
    ObjectNode tim = baseTim();

    dataFrame(tim).put("startDateTime", "2018-06-11T16:00:00.000Z");
    TravelerDataFrame frame = convertedDataFrame(tim);
    assertEquals(2018, frame.getStartYear().getValue());
    assertEquals(232800, frame.getStartTime().getValue());

    dataFrame(tim).put("startDateTime", "2018-06-11T10:00-06:00");
    frame = convertedDataFrame(tim);
    assertEquals(2018, frame.getStartYear().getValue());
    assertEquals(232800, frame.getStartTime().getValue(), "same instant as 16:00Z");

    dataFrame(tim).put("startDateTime", "2018-15-44T25:66:77.999Z");
    frame = convertedDataFrame(tim);
    assertEquals(0, frame.getStartYear().getValue(), "an invalid date gives year 0");
    assertEquals(527040, frame.getStartTime().getValue(), "and the 'unknown' minute");
  }

  @Test
  void furtherInfoIdConvertsWithoutRoadSignId() throws Exception {
    ObjectNode tim = baseTim();
    dataFrame(tim).set("msgId", json("""
        { "furtherInfoID": "ABCD" }
        """));

    TravelerDataFrame frame = convertedDataFrame(tim);

    assertNull(frame.getMsgId().getRoadSignID());
    assertEquals("ABCD", frame.getMsgId().getFurtherInfoID().getValue());
  }

  @Test
  void itemsConvertToItisCodesOrText() throws Exception {
    ObjectNode tim = baseTim();
    dataFrame(tim).set("items", json("""
        ["123", "'123", "test 123"]
        """));

    ITIScodesAndText advisory = convertedDataFrame(tim).getContent().getAdvisory();

    assertEquals(3, advisory.size());
    assertEquals(123, advisory.get(0).getItem().getItis().getValue(), "a number is an ITIS code");
    assertNull(advisory.get(0).getItem().getText());
    assertEquals("123", advisory.get(1).getItem().getText().getValue(), "a leading ' makes it text");
    assertEquals("test 123", advisory.get(2).getItem().getText().getValue());
  }

  @Test
  void contentNameSelectsContentChoice() throws Exception {
    ObjectNode tim = baseTim();

    dataFrame(tim).put("content", "Advisory");
    assertNotNull(convertedDataFrame(tim).getContent().getAdvisory(), "Advisory is accepted capitalized");

    dataFrame(tim).put("content", "workZone");
    assertEquals(125, convertedDataFrame(tim).getContent().getWorkZone().get(0).getItem().getItis().getValue());

    dataFrame(tim).put("content", "genericSign");
    assertNotNull(convertedDataFrame(tim).getContent().getGenericSign());

    dataFrame(tim).put("content", "speedLimit");
    assertNotNull(convertedDataFrame(tim).getContent().getSpeedLimit());

    dataFrame(tim).put("content", "exitService");
    dataFrame(tim).set("items", json("""
        ["text"]
        """));
    assertEquals("text", convertedDataFrame(tim).getContent().getExitService().get(0).getItem().getText().getValue());

    dataFrame(tim).put("content", "unknown");
    TravelerDataFrame.ContentChoice unknown = convertedDataFrame(tim).getContent();
    assertNull(unknown.getAdvisory(), "an unknown content name gives empty content");
    assertNull(unknown.getWorkZone());
  }

  // ---- Region ----

  @Test
  void convertsRegionFields() throws Exception {
    GeographicalPath region = convertedRegion(baseTim());

    assertEquals("Testing TIM", region.getName().getValue());
    assertEquals(0, region.getId().getRegion().getValue());
    assertEquals(33, region.getId().getId().getValue());
    Position3D anchor = region.getAnchor();
    assertEquals(412500807, anchor.getLat().getValue());
    assertEquals(-1110093847, anchor.getLong_().getValue());
    assertEquals(20207, anchor.getElevation().getValue(), "2020.697 m, rounded");
    assertEquals(700, region.getLaneWidth().getValue(), "7 m");
    assertEquals(DirectionOfUse.BOTH, region.getDirectionality(), "3 is 'both'");
    assertFalse(region.getClosedPath().getValue());
    assertEquals("0000000000001010", region.getDirection().binaryString());
  }

  @Test
  void directionalityAcceptsNumberOrName() throws Exception {
    ObjectNode tim = baseTim();

    region(tim).put("directionality", "1");
    assertEquals(DirectionOfUse.FORWARD, convertedRegion(tim).getDirectionality());

    region(tim).put("directionality", "reverse");
    assertEquals(DirectionOfUse.REVERSE, convertedRegion(tim).getDirectionality());
  }

  @Test
  void regionWithoutDescriptionHasNoDescription() throws Exception {
    ObjectNode tim = baseTim();
    region(tim).remove("description");
    region(tim).remove("path");

    assertNull(convertedRegion(tim).getDescription());
  }

  @Test
  void unknownDescriptionThrows() {
    ObjectNode tim = baseTim();
    region(tim).put("description", "foo");

    UnsupportedRegionException e = assertThrows(UnsupportedRegionException.class, () -> convertedRegion(tim));
    assertEquals("Unsupported region description: foo", e.getMessage());
  }

  // ---- Path nodes: lat/long offsets (path type "ll") ----

  @Test
  void llNodeWithExplicitOffsetType() throws Exception {
    ObjectNode tim = baseTim();
    setPathNode(region(tim), "ll", llNode("node-LL3", new BigDecimal("0.0014506"), new BigDecimal("0.0031024")));

    NodeOffsetPointLL offset = convertedLLNodeOffset(tim);

    assertEquals(14506, offset.getNode_LL3().getLat().getValue());
    assertEquals(31024, offset.getNode_LL3().getLon().getValue());
  }

  @Test
  void llNodeWithoutOffsetTypeGetsSmallestTypeThatFits() throws Exception {
    ObjectNode tim = baseTim();
    setPathNode(region(tim), "ll", llNode("node-LL", new BigDecimal("-0.0013123"), new BigDecimal("-0.0008192")));

    NodeOffsetPointLL offset = convertedLLNodeOffset(tim);

    // 13123 needs more than node-LL2's 8191.
    assertEquals(-13123, offset.getNode_LL3().getLat().getValue());
    assertEquals(-8192, offset.getNode_LL3().getLon().getValue());
  }

  @Test
  void llNodeWithAbsoluteLatLon() throws Exception {
    ObjectNode tim = baseTim();
    setPathNode(region(tim), "ll",
        llNode("node-LatLon", new BigDecimal("41.2500807"), new BigDecimal("-111.0093847")));

    NodeOffsetPointLL offset = convertedLLNodeOffset(tim);

    assertEquals(412500807, offset.getNode_LatLon().getLat().getValue());
    assertEquals(-1110093847, offset.getNode_LatLon().getLon().getValue());
  }

  /**
   * Lat and long offsets in degrees × 10^7, and the node-LL type expected for them. Each type
   * holds offsets up to: LL1 2047, LL2 8191, LL3 32767, LL4 131071, LL5 2097151, LL6 8388607. The
   * larger of the two offsets decides.
   */
  static Stream<Arguments> llOffsetsAndExpectedType() {
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

  @ParameterizedTest(name = "lat {0}, long {1} -> {2}")
  @MethodSource("llOffsetsAndExpectedType")
  void llNodeOffsetTypeIsSmallestThatFits(long lat, long lon, String expectedType) throws Exception {
    ObjectNode tim = baseTim();
    setPathNode(region(tim), "ll", llNode("node-LL", BigDecimal.valueOf(lat, 7), BigDecimal.valueOf(lon, 7)));

    NodeOffsetPointLL offset = convertedLLNodeOffset(tim);

    Object chosen = switch (expectedType) {
      case "node-LL1" -> offset.getNode_LL1();
      case "node-LL2" -> offset.getNode_LL2();
      case "node-LL3" -> offset.getNode_LL3();
      case "node-LL4" -> offset.getNode_LL4();
      case "node-LL5" -> offset.getNode_LL5();
      default -> offset.getNode_LL6();
    };
    assertNotNull(chosen, "expected " + expectedType);
  }

  @Test
  void llNodeOffsetTooLargeThrows() {
    ObjectNode latTooLarge = baseTim();
    setPathNode(region(latTooLarge), "ll",
        llNode("node-LL", BigDecimal.valueOf(-8388608L, 7), BigDecimal.valueOf(-8388607L, 7)));
    InvalidNodeLatLonOffsetException e =
        assertThrows(InvalidNodeLatLonOffsetException.class, () -> convertedRegion(latTooLarge));
    assertEquals("Invalid node lat/long offset: -8388608/-8388607. Values must be within a range of "
        + "-0.8388607/+0.8388607 degrees.", e.getMessage());

    ObjectNode longTooLarge = baseTim();
    setPathNode(region(longTooLarge), "ll",
        llNode("node-LL", BigDecimal.valueOf(-8388607L, 7), BigDecimal.valueOf(-8388608L, 7)));
    assertThrows(InvalidNodeLatLonOffsetException.class, () -> convertedRegion(longTooLarge));
  }

  // ---- Path nodes: x/y offsets (path type "xy") ----

  /**
   * x and y offsets in metres, the node-XY type expected for them, and the x offset in centimetres.
   *
   * <p>The type comes from the bit length of the larger offset in cm: under 2^10 is XY1, under
   * 2^11 XY2, and so on up to under 2^16 for XY6. This is the legacy rule, kept unchanged. It allows
   * twice the ASN.1 range of each type: XY1's Offset-B10 is -512..511 cm, yet 10.23 m becomes
   * node-XY1 with x = 1023.</p>
   */
  static Stream<Arguments> xyOffsetsAndExpectedType() {
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

  @ParameterizedTest(name = "x {0} m, y {1} m -> {2}")
  @MethodSource("xyOffsetsAndExpectedType")
  void xyNodeOffsetTypeIsSmallestThatFits(String x, String y, String expectedType, long expectedXcm)
      throws Exception {
    ObjectNode tim = baseTim();
    setPathNode(region(tim), "xy", xyNode("node-XY", x, y));

    NodeOffsetPointXY offset = convertedXYNode(tim).getDelta();

    long actualXcm = switch (expectedType) {
      case "node-XY1" -> offset.getNode_XY1().getX().getValue();
      case "node-XY2" -> offset.getNode_XY2().getX().getValue();
      case "node-XY3" -> offset.getNode_XY3().getX().getValue();
      case "node-XY4" -> offset.getNode_XY4().getX().getValue();
      case "node-XY5" -> offset.getNode_XY5().getX().getValue();
      default -> offset.getNode_XY6().getX().getValue();
    };
    assertEquals(expectedXcm, actualXcm);
  }

  @Test
  void xyNodeOffsetTooLargeThrows() {
    ObjectNode tim = baseTim();
    setPathNode(region(tim), "xy", xyNode("node-XY", "655.36", "0"));

    assertThrows(IllegalArgumentException.class, () -> convertedRegion(tim));
  }

  @Test
  void xyNodeWithExplicitOffsetTypeAndAttributes() throws Exception {
    ObjectNode tim = baseTim();
    ObjectNode node = xyNode("node-XY3", "1.5", "-2.5");
    node.set("attributes", json("""
        { "dWidth": "0.5" }
        """));
    setPathNode(region(tim), "xy", node);

    NodeXY converted = convertedXYNode(tim);

    assertEquals(150, converted.getDelta().getNode_XY3().getX().getValue());
    assertEquals(-250, converted.getDelta().getNode_XY3().getY().getValue());
    // Attribute values are not converted: any attributes give an empty NodeAttributeSetXY, as the
    // legacy converter did.
    assertNotNull(converted.getAttributes());
  }

  // ---- Geometry and oldRegion descriptions ----

  @Test
  void geometryWithCircle() throws Exception {
    ObjectNode tim = baseTim();
    describeRegionAs(region(tim), "geometry", """
        {
          "direction": "1111000011110000",
          "extent": "5",
          "laneWidth": "3.66",
          "circle": {
            "position": { "latitude": "41.153593", "longitude": "-104.655785", "elevation": "1824" },
            "radius": "50",
            "units": "1"
          }
        }
        """);

    GeometricProjection geometry = convertedRegion(tim).getDescription().getGeometry();

    assertEquals("1111000011110000", geometry.getDirection().binaryString());
    assertEquals(Extent.USEFOR500METERS, geometry.getExtent(), "extent 5 is useFor500meters");
    assertEquals(366, geometry.getLaneWidth().getValue(), "3.66 m");
    assertEquals(411535930, geometry.getCircle().getCenter().getLat().getValue());
    assertEquals(18240, geometry.getCircle().getCenter().getElevation().getValue(), "1824 m");
    assertEquals(50, geometry.getCircle().getRadius().getValue());
    assertEquals(DistanceUnits.CM2_5, geometry.getCircle().getUnits(), "units 1 is cm2-5");
  }

  @Test
  void oldRegionWithCircle() throws Exception {
    ObjectNode tim = baseTim();
    describeRegionAs(region(tim), "oldRegion", """
        {
          "direction": "1010101010101010",
          "extent": "useFor3meters",
          "area": {
            "circle": {
              "center": { "latitude": "41.678473", "longitude": "-108.782775", "elevation": "917.1432" },
              "radius": "2048",
              "units": "centimeter"
            }
          }
        }
        """);

    ValidRegion oldRegion = convertedRegion(tim).getDescription().getOldRegion();

    assertEquals(Extent.USEFOR3METERS, oldRegion.getExtent());
    assertEquals(416784730, oldRegion.getArea().getCircle().getCenter().getLat().getValue(),
        "the circle may give its center as 'center' instead of 'position'");
    assertEquals(DistanceUnits.CENTIMETER, oldRegion.getArea().getCircle().getUnits());
  }

  @Test
  void oldRegionWithRegionPointAreaThrows() {
    ObjectNode tim = baseTim();
    describeRegionAs(region(tim), "oldRegion", """
        {
          "direction": "1010101010101010",
          "extent": "1",
          "area": {
            "regionPoint": {
              "position": { "latitude": "41.678473", "longitude": "-108.782775" },
              "scale": 0,
              "regionList": []
            }
          }
        }
        """);

    assertThrows(UnsupportedRegionException.class, () -> convertedRegion(tim),
        "only a circle area is supported");
  }
}
