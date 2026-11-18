package us.dot.its.jpo.ode.traveler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.Common.DYear;
import us.dot.its.jpo.asn.j2735.r2024.Common.DescriptiveName;
import us.dot.its.jpo.asn.j2735.r2024.Common.Elevation;
import us.dot.its.jpo.asn.j2735.r2024.Common.Extent;
import us.dot.its.jpo.asn.j2735.r2024.Common.FurtherInfoID;
import us.dot.its.jpo.asn.j2735.r2024.Common.HeadingSlice;
import us.dot.its.jpo.asn.j2735.r2024.Common.LaneWidth;
import us.dot.its.jpo.asn.j2735.r2024.Common.Latitude;
import us.dot.its.jpo.asn.j2735.r2024.Common.Longitude;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;
import us.dot.its.jpo.asn.j2735.r2024.Common.MsgCount;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeAttributeSetXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeListXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeOffsetPointXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeSetXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_LLmD_64b;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_XY_20b;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_XY_22b;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_XY_24b;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_XY_26b;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_XY_28b;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_XY_32b;
import us.dot.its.jpo.asn.j2735.r2024.Common.OffsetLL_B18;
import us.dot.its.jpo.asn.j2735.r2024.Common.Offset_B10;
import us.dot.its.jpo.asn.j2735.r2024.Common.Offset_B11;
import us.dot.its.jpo.asn.j2735.r2024.Common.Offset_B12;
import us.dot.its.jpo.asn.j2735.r2024.Common.Offset_B13;
import us.dot.its.jpo.asn.j2735.r2024.Common.Offset_B14;
import us.dot.its.jpo.asn.j2735.r2024.Common.Offset_B16;
import us.dot.its.jpo.asn.j2735.r2024.Common.Position3D;
import us.dot.its.jpo.asn.j2735.r2024.Common.RoadRegulatorID;
import us.dot.its.jpo.asn.j2735.r2024.Common.RoadSegmentID;
import us.dot.its.jpo.asn.j2735.r2024.Common.RoadSegmentReferenceID;
import us.dot.its.jpo.asn.j2735.r2024.Common.SSPindex;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.ITIScodes;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.ITIScodesAndText;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.ITIScodesAndTextSequence;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.ITIStext;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Circle;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DirectionOfUse;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DistanceUnits;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ExitService;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ExitServiceSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GenericSignage;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GenericSignageSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeometricProjection;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ITIStextPhrase;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.MUTCDCode;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.MinutesDuration;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.MsgCRC;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.NodeLL;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.NodeListLL;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.NodeOffsetPointLL;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.NodeSetLL;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Node_LL_24B;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Node_LL_28B;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Node_LL_32B;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Node_LL_36B;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Node_LL_44B;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Node_LL_48B;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.OffsetLL_B12;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.OffsetLL_B14;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.OffsetLL_B16;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.OffsetLL_B22;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.OffsetLL_B24;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.OffsetSystem;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Radius_B12;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.RoadSignID;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.SignPriority;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.SpeedLimit;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.SpeedLimitSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrameList;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInfoType;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.URL_Base;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.URL_Short;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.UniqueMSGID;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ValidRegion;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.WorkZone;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.WorkZoneSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Zoom;
import us.dot.its.jpo.asn.runtime.types.Asn1Boolean;
import us.dot.its.jpo.asn.runtime.types.Asn1Enumerated;
import us.dot.its.jpo.ode.plugin.j2735.OdePosition3D;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DirectionOfUseEnum;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DistanceUnitsEnum;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.ExtentEnum;
import us.dot.its.jpo.ode.util.DateTimeUtils;

/**
 * Converts the TIM in a TIM deposit request into a J2735 r2024 {@link TravelerInformation}.
 *
 * <h2>Purpose</h2>
 * <p>{@code POST}/{@code PUT /tim} ({@link TimDepositController}) accepts a TIM written for
 * people: an {@link OdeTravelerInformationMessage}. It uses decimal degrees and metres, ISO 8601
 * timestamps, enum values given by name or by number, and a flat list of content items. This class
 * turns it into the ASN.1 form. The depositor then wraps the result in a MessageFrame
 * ({@link TimTransmogrifier#convertToXml}) and publishes it as encoder XML and OdeTimJson.</p>
 *
 * <p>It replaces the legacy {@code TravelerMessageFromHumanToAsnConverter}. That class rewrote
 * the request JSON in place, then reordered it through the {@code timstorage} classes. The output
 * is meant to be identical to the legacy output, except for the corrections listed in
 * {@code TimDepositControllerRegressionTest}, which checks it. A few helpers below exist only to
 * keep that output the same; their comments say so.</p>
 *
 * <h2>How it works</h2>
 * <p>{@link #convert} walks the request top-down and builds the matching r2024 object at each
 * level:</p>
 * <pre>
 * OdeTravelerInformationMessage          -&gt; TravelerInformation
 *   DataFrame (dataframes[])             -&gt; TravelerDataFrame
 *     MsgId                              -&gt; MsgIdChoice (roadSignID or furtherInfoID)
 *     startDateTime                      -&gt; startYear + startTime
 *     Region (regions[])                 -&gt; GeographicalPath
 *       description "path"               -&gt;   OffsetSystem with node-LL or node-XY nodes
 *       description "geometry"           -&gt;   GeometricProjection (circle)
 *       description "oldRegion"          -&gt;   ValidRegion (circle area only)
 *     content + items[]                  -&gt; ContentChoice (advisory, workZone, ...)
 * </pre>
 * <p>Conversion rules used throughout:</p>
 * <ul>
 *   <li><b>Units.</b> Latitude, longitude and node-LL offsets in degrees become units of
 *       10^-7 degree. Elevation in metres becomes decimetres. Lane width and node-XY offsets in
 *       metres become centimetres. Values are rounded half up.</li>
 *   <li><b>Times.</b> An ISO 8601 timestamp becomes the minute of its year. One that cannot be
 *       parsed gives {@value #UNKNOWN_MINUTE_OF_THE_YEAR}, the ASN.1 "unknown" value, and is
 *       logged.</li>
 *   <li><b>Enumerations.</b> Where the request may give a number (directionality, oldRegion
 *       extent, circle units), the number is an index into the request enum, e.g. directionality
 *       {@code "3"} is {@code both}. The name is then looked up in the r2024 enum.</li>
 *   <li><b>Node offsets.</b> For a {@code node-LL} or {@code node-XY} delta without a size digit,
 *       the smallest size that fits the offset is chosen.</li>
 * </ul>
 * <p>Requests that cannot be converted throw {@link InvalidNodeLatLonOffsetException} or
 * {@link UnsupportedRegionException}. The depositor turns either one into a 400 response.</p>
 */
@Slf4j
public class TravelerInformationConverter {

  private static final String NODE_LL = "node-LL";
  private static final String NODE_XY = "node-XY";
  private static final String NODE_LAT_LON = "node-LatLon";
  /** MinuteOfTheYear value meaning "unknown". */
  private static final long UNKNOWN_MINUTE_OF_THE_YEAR = 527040;

  /** A path node's lat/long offset is too large for any node-LL size (more than ±0.8388607°). */
  public static class InvalidNodeLatLonOffsetException extends Exception {
    public InvalidNodeLatLonOffsetException(String message) {
      super(message);
    }
  }

  /**
   * A region uses a description this converter does not support: anything other than path,
   * geometry or oldRegion, or an oldRegion area other than a circle.
   */
  public static class UnsupportedRegionException extends Exception {
    public UnsupportedRegionException(String message) {
      super(message);
    }
  }

  private TravelerInformationConverter() {
    throw new UnsupportedOperationException();
  }

  /**
   * Converts a deposit-request TIM into a J2735 r2024 TravelerInformation.
   *
   * @param tim the {@code tim} part of a TIM deposit request
   * @return the TravelerInformation; it has an empty data frame list if the request has none
   * @throws InvalidNodeLatLonOffsetException if a node-LL offset is too large
   * @throws UnsupportedRegionException if a region description is not supported
   */
  public static TravelerInformation convert(OdeTravelerInformationMessage tim)
      throws InvalidNodeLatLonOffsetException, UnsupportedRegionException {
    TravelerInformation travelerInformation = new TravelerInformation();
    travelerInformation.setMsgCnt(new MsgCount(tim.getMsgCnt()));
    if (tim.getTimeStamp() != null) {
      travelerInformation.setTimeStamp(
          new MinuteOfTheYear(translateIsoTimeStampToMinuteOfYear(tim.getTimeStamp())));
    }
    if (tim.getPacketID() != null) {
      travelerInformation.setPacketID(new UniqueMSGID(tim.getPacketID()));
    }
    if (tim.getUrlB() != null) {
      travelerInformation.setUrlB(new URL_Base(tim.getUrlB()));
    }
    TravelerDataFrameList dataFrames = new TravelerDataFrameList();
    if (tim.getDataframes() != null) {
      for (DataFrame dataFrame : tim.getDataframes()) {
        dataFrames.add(convertDataFrame(dataFrame));
      }
    }
    travelerInformation.setDataFrames(dataFrames);
    return travelerInformation;
  }

  /**
   * The minute of the year of an ISO 8601 timestamp, counted from January 1 00:00 UTC of the
   * timestamp's year. For example, {@code 2018-06-11T16:00:00.000Z} and
   * {@code 2018-06-11T10:00-06:00} are both minute 232800.
   *
   * @return the minute of the year, or {@value #UNKNOWN_MINUTE_OF_THE_YEAR} if it cannot be parsed
   */
  public static long translateIsoTimeStampToMinuteOfYear(String isoTime) {
    long minuteOfYear = UNKNOWN_MINUTE_OF_THE_YEAR;
    try {
      ZonedDateTime zonedDateTime = DateTimeUtils.isoDateTime(isoTime);
      minuteOfYear = (int) Duration.between(
          DateTimeUtils.isoDateTime(zonedDateTime.getYear(), 1, 1, 0, 0, 0, 0), zonedDateTime)
          .toMinutes();
    } catch (Exception e) {
      log.error("Failed to parse datetime {}, defaulting to unknown value {}", isoTime,
          minuteOfYear);
    }
    return minuteOfYear;
  }

  private static TravelerDataFrame convertDataFrame(DataFrame dataFrame)
      throws InvalidNodeLatLonOffsetException, UnsupportedRegionException {
    TravelerDataFrame travelerDataFrame = new TravelerDataFrame();
    travelerDataFrame.setDoNotUse1(new SSPindex(dataFrame.getDoNotUse1()));
    travelerDataFrame.setFrameType(byName(TravelerInfoType.class, dataFrame.getFrameType().name()));
    if (dataFrame.getMsgId() != null) {
      travelerDataFrame.setMsgId(convertMsgId(dataFrame.getMsgId()));
    }

    // startDateTime becomes the year plus the minute of that year. An unparseable value gives
    // year 0 and the "unknown" minute.
    int startYear = 0;
    long startMinute = UNKNOWN_MINUTE_OF_THE_YEAR;
    try {
      ZonedDateTime zonedDateTime = DateTimeUtils.isoDateTime(dataFrame.getStartDateTime());
      startYear = zonedDateTime.getYear();
      startMinute = (int) ChronoUnit.MINUTES.between(
          DateTimeUtils.isoDateTime(startYear, 1, 1, 0, 0, 0, 0), zonedDateTime);
    } catch (Exception e) {
      log.error("Failed to parse startDateTime {}, defaulting to unknown value {}.",
          dataFrame.getStartDateTime(), startMinute);
    }
    travelerDataFrame.setStartYear(new DYear(startYear));
    travelerDataFrame.setStartTime(new MinuteOfTheYear(startMinute));

    travelerDataFrame.setDurationTime(new MinutesDuration(dataFrame.getDurationTime()));
    travelerDataFrame.setPriority(new SignPriority(dataFrame.getPriority()));
    travelerDataFrame.setDoNotUse2(new SSPindex(dataFrame.getDoNotUse2()));
    TravelerDataFrame.SequenceOfRegions regions = new TravelerDataFrame.SequenceOfRegions();
    for (Region region : dataFrame.getRegions()) {
      regions.add(convertRegion(region));
    }
    travelerDataFrame.setRegions(regions);
    travelerDataFrame.setDoNotUse3(new SSPindex(dataFrame.getDoNotUse3()));
    travelerDataFrame.setDoNotUse4(new SSPindex(dataFrame.getDoNotUse4()));
    travelerDataFrame.setContent(convertContent(dataFrame.getContent(), dataFrame.getItems()));
    if (dataFrame.getUrl() != null) {
      travelerDataFrame.setUrl(new URL_Short(dataFrame.getUrl()));
    }
    return travelerDataFrame;
  }

  private static TravelerDataFrame.MsgIdChoice convertMsgId(DataFrame.MsgId msgId) {
    TravelerDataFrame.MsgIdChoice msgIdChoice = new TravelerDataFrame.MsgIdChoice();
    DataFrame.RoadSignID roadSignId = msgId.getRoadSignID();
    if (roadSignId != null) {
      RoadSignID roadSignID = new RoadSignID();
      roadSignID.setPosition(position3D(roadSignId.getPosition()));
      if (roadSignId.getViewAngle() != null) {
        roadSignID.setViewAngle(headingSlice(roadSignId.getViewAngle()));
      }
      if (roadSignId.getMutcdCode() != null) {
        roadSignID.setMutcdCode(byName(MUTCDCode.class, roadSignId.getMutcdCode().name()));
      }
      if (roadSignId.getCrc() != null) {
        roadSignID.setCrc(new MsgCRC(roadSignId.getCrc()));
      }
      msgIdChoice.setRoadSignID(roadSignID);
    }
    if (msgId.getFurtherInfoID() != null) {
      msgIdChoice.setFurtherInfoID(new FurtherInfoID(msgId.getFurtherInfoID()));
    }
    return msgIdChoice;
  }

  /**
   * Converts the frame's {@code content} name and {@code items} into the matching content choice.
   * Each content type has its own generated list class, but they share one shape: a list whose
   * entries hold an item that is either an ITIS code or text. An unknown content name is logged
   * and gives empty content, as in the legacy converter.
   */
  private static TravelerDataFrame.ContentChoice convertContent(String content, String[] items) {
    TravelerDataFrame.ContentChoice contentChoice = new TravelerDataFrame.ContentChoice();
    switch (content) {
      case "advisory", "Advisory" -> contentChoice.setAdvisory(advisory(items));
      case "workZone" -> contentChoice.setWorkZone(workZone(items));
      case "genericSign" -> contentChoice.setGenericSign(genericSign(items));
      case "speedLimit" -> contentChoice.setSpeedLimit(speedLimit(items));
      case "exitService" -> contentChoice.setExitService(exitService(items));
      default -> log.warn("Unsupported TIM content type: {}", content);
    }
    return contentChoice;
  }

  /**
   * One content item from the request. An item that parses as an integer is an ITIS code.
   * Anything else is text, and a leading {@code '} forces text, so {@code "'123"} is the text
   * {@code 123}.
   */
  private record ContentItem(Integer itisCode, String text) {

    static ContentItem parse(String item) {
      String value = String.valueOf(item);
      try {
        return new ContentItem(Integer.valueOf(value), null);
      } catch (NumberFormatException e) {
        return new ContentItem(null, value.startsWith("'") ? value.substring(1) : value);
      }
    }

    boolean isItisCode() {
      return itisCode != null;
    }
  }

  // The five methods below differ only in the generated classes they use. Advisory text is
  // ITIStext (up to 500 characters); the others use ITIStextPhrase (up to 16).

  private static ITIScodesAndText advisory(String[] items) {
    ITIScodesAndText advisory = new ITIScodesAndText();
    for (String item : items) {
      ContentItem contentItem = ContentItem.parse(item);
      ITIScodesAndTextSequence.ItemChoice choice = new ITIScodesAndTextSequence.ItemChoice();
      if (contentItem.isItisCode()) {
        choice.setItis(new ITIScodes(contentItem.itisCode()));
      } else {
        choice.setText(new ITIStext(contentItem.text()));
      }
      ITIScodesAndTextSequence entry = new ITIScodesAndTextSequence();
      entry.setItem(choice);
      advisory.add(entry);
    }
    return advisory;
  }

  private static WorkZone workZone(String[] items) {
    WorkZone workZone = new WorkZone();
    for (String item : items) {
      ContentItem contentItem = ContentItem.parse(item);
      WorkZoneSequence.ItemChoice choice = new WorkZoneSequence.ItemChoice();
      if (contentItem.isItisCode()) {
        choice.setItis(new ITIScodes(contentItem.itisCode()));
      } else {
        choice.setText(new ITIStextPhrase(contentItem.text()));
      }
      WorkZoneSequence entry = new WorkZoneSequence();
      entry.setItem(choice);
      workZone.add(entry);
    }
    return workZone;
  }

  private static GenericSignage genericSign(String[] items) {
    GenericSignage genericSign = new GenericSignage();
    for (String item : items) {
      ContentItem contentItem = ContentItem.parse(item);
      GenericSignageSequence.ItemChoice choice = new GenericSignageSequence.ItemChoice();
      if (contentItem.isItisCode()) {
        choice.setItis(new ITIScodes(contentItem.itisCode()));
      } else {
        choice.setText(new ITIStextPhrase(contentItem.text()));
      }
      GenericSignageSequence entry = new GenericSignageSequence();
      entry.setItem(choice);
      genericSign.add(entry);
    }
    return genericSign;
  }

  private static SpeedLimit speedLimit(String[] items) {
    SpeedLimit speedLimit = new SpeedLimit();
    for (String item : items) {
      ContentItem contentItem = ContentItem.parse(item);
      SpeedLimitSequence.ItemChoice choice = new SpeedLimitSequence.ItemChoice();
      if (contentItem.isItisCode()) {
        choice.setItis(new ITIScodes(contentItem.itisCode()));
      } else {
        choice.setText(new ITIStextPhrase(contentItem.text()));
      }
      SpeedLimitSequence entry = new SpeedLimitSequence();
      entry.setItem(choice);
      speedLimit.add(entry);
    }
    return speedLimit;
  }

  private static ExitService exitService(String[] items) {
    ExitService exitService = new ExitService();
    for (String item : items) {
      ContentItem contentItem = ContentItem.parse(item);
      ExitServiceSequence.ItemChoice choice = new ExitServiceSequence.ItemChoice();
      if (contentItem.isItisCode()) {
        choice.setItis(new ITIScodes(contentItem.itisCode()));
      } else {
        choice.setText(new ITIStextPhrase(contentItem.text()));
      }
      ExitServiceSequence entry = new ExitServiceSequence();
      entry.setItem(choice);
      exitService.add(entry);
    }
    return exitService;
  }

  /**
   * Converts one region. Its description must be {@code path}, {@code geometry} or
   * {@code oldRegion}; a region without a description is converted without one.
   */
  private static GeographicalPath convertRegion(Region region)
      throws InvalidNodeLatLonOffsetException, UnsupportedRegionException {
    GeographicalPath geographicalPath = new GeographicalPath();
    if (region.getName() != null) {
      geographicalPath.setName(new DescriptiveName(region.getName()));
    }
    RoadSegmentReferenceID id = new RoadSegmentReferenceID();
    id.setRegion(new RoadRegulatorID(region.getRegulatorID()));
    id.setId(new RoadSegmentID(region.getSegmentID()));
    geographicalPath.setId(id);
    if (region.getAnchorPosition() != null) {
      geographicalPath.setAnchor(position3D(region.getAnchorPosition()));
    }
    if (region.getLaneWidth() != null) {
      geographicalPath.setLaneWidth(laneWidth(region.getLaneWidth()));
    }
    if (region.getDirectionality() != null) {
      geographicalPath.setDirectionality(
          byName(DirectionOfUse.class, enumName(DirectionOfUseEnum.class, region.getDirectionality())));
    }
    geographicalPath.setClosedPath(new Asn1Boolean(region.isClosedPath()));
    if (region.getDirection() != null) {
      geographicalPath.setDirection(headingSlice(region.getDirection()));
    }
    if (region.getDescription() != null) {
      GeographicalPath.DescriptionChoice description = new GeographicalPath.DescriptionChoice();
      switch (region.getDescription()) {
        case "path" -> {
          description.setPath(convertPath(region.getPath()));
          geographicalPath.setDescription(description);
        }
        case "geometry" -> {
          description.setGeometry(convertGeometry(region.getGeometry()));
          geographicalPath.setDescription(description);
        }
        case "oldRegion" -> {
          description.setOldRegion(convertOldRegion(region.getOldRegion()));
          geographicalPath.setDescription(description);
        }
        default -> throw new UnsupportedRegionException(
            "Unsupported region description: " + region.getDescription());
      }
    }
    return geographicalPath;
  }

  /**
   * Converts a path. Path type {@code ll} gives lat/long offset nodes and {@code xy} gives x/y
   * offset nodes. Any other type gives an OffsetSystem with only the scale.
   */
  private static OffsetSystem convertPath(Region.Path path) throws InvalidNodeLatLonOffsetException {
    OffsetSystem offsetSystem = new OffsetSystem();
    offsetSystem.setScale(new Zoom(path.getScale()));
    OffsetSystem.OffsetChoice offset = new OffsetSystem.OffsetChoice();
    if ("ll".equals(path.getType())) {
      NodeSetLL nodes = new NodeSetLL();
      for (OdeTravelerInformationMessage.NodeXY node : path.getNodes()) {
        nodes.add(convertNodeLL(node));
      }
      NodeListLL nodeList = new NodeListLL();
      nodeList.setNodes(nodes);
      offset.setLl(nodeList);
      offsetSystem.setOffset(offset);
    } else if ("xy".equals(path.getType())) {
      NodeSetXY nodes = new NodeSetXY();
      for (OdeTravelerInformationMessage.NodeXY node : path.getNodes()) {
        nodes.add(convertNodeXY(node));
      }
      NodeListXY nodeList = new NodeListXY();
      nodeList.setNodes(nodes);
      offset.setXy(nodeList);
      offsetSystem.setOffset(offset);
    }
    return offsetSystem;
  }

  /**
   * Converts a node of an {@code ll} path. The delta picks the form:
   * <ul>
   *   <li>{@code node-LL1} .. {@code node-LL6}: an offset of that size;</li>
   *   <li>{@code node-LL}: an offset of the smallest size that fits;</li>
   *   <li>{@code node-LatLon}: an absolute position, not an offset.</li>
   * </ul>
   * Any other delta gives a node with an empty offset; an unknown node-LL size is also logged.
   */
  private static NodeLL convertNodeLL(OdeTravelerInformationMessage.NodeXY node)
      throws InvalidNodeLatLonOffsetException {
    BigDecimal latOffset = jsonDecimal(node.getNodeLat());
    BigDecimal longOffset = jsonDecimal(node.getNodeLong());
    String delta = node.getDelta();
    NodeOffsetPointLL offsetPoint = new NodeOffsetPointLL();
    if (delta.startsWith(NODE_LL)) {
      long lat = scaleToLong(latOffset, 7);
      long lon = scaleToLong(longOffset, 7);
      if (delta.equals(NODE_LL)) {
        delta = nodeOffsetPointLLType(lat, lon);
      }
      switch (delta) {
        case "node-LL1" -> {
          Node_LL_24B point = new Node_LL_24B();
          point.setLon(new OffsetLL_B12(lon));
          point.setLat(new OffsetLL_B12(lat));
          offsetPoint.setNode_LL1(point);
        }
        case "node-LL2" -> {
          Node_LL_28B point = new Node_LL_28B();
          point.setLon(new OffsetLL_B14(lon));
          point.setLat(new OffsetLL_B14(lat));
          offsetPoint.setNode_LL2(point);
        }
        case "node-LL3" -> {
          Node_LL_32B point = new Node_LL_32B();
          point.setLon(new OffsetLL_B16(lon));
          point.setLat(new OffsetLL_B16(lat));
          offsetPoint.setNode_LL3(point);
        }
        case "node-LL4" -> {
          Node_LL_36B point = new Node_LL_36B();
          point.setLon(new OffsetLL_B18(lon));
          point.setLat(new OffsetLL_B18(lat));
          offsetPoint.setNode_LL4(point);
        }
        case "node-LL5" -> {
          Node_LL_44B point = new Node_LL_44B();
          point.setLon(new OffsetLL_B22(lon));
          point.setLat(new OffsetLL_B22(lat));
          offsetPoint.setNode_LL5(point);
        }
        case "node-LL6" -> {
          Node_LL_48B point = new Node_LL_48B();
          point.setLon(new OffsetLL_B24(lon));
          point.setLat(new OffsetLL_B24(lat));
          offsetPoint.setNode_LL6(point);
        }
        default -> log.warn("Unsupported node-LL delta: {}", delta);
      }
    } else if (NODE_LAT_LON.equals(delta)) {
      offsetPoint.setNode_LatLon(nodeLatLon(latOffset, longOffset));
    }
    NodeLL nodeLL = new NodeLL();
    nodeLL.setDelta(offsetPoint);
    return nodeLL;
  }

  /**
   * The smallest node-LL size whose range holds both offsets (units of 10^-7 degree). LL1 holds
   * ±2047, LL2 ±8191, LL3 ±32767, LL4 ±131071, LL5 ±2097151 and LL6 ±8388607.
   */
  private static String nodeOffsetPointLLType(long latDelta, long lonDelta)
      throws InvalidNodeLatLonOffsetException {
    long absLatDelta = Math.abs(latDelta);
    long absLonDelta = Math.abs(lonDelta);
    if (absLatDelta <= 2047 && absLonDelta <= 2047) {
      return "node-LL1";
    } else if (absLatDelta <= 8191 && absLonDelta <= 8191) {
      return "node-LL2";
    } else if (absLatDelta <= 32767 && absLonDelta <= 32767) {
      return "node-LL3";
    } else if (absLatDelta <= 131071 && absLonDelta <= 131071) {
      return "node-LL4";
    } else if (absLatDelta <= 2097151 && absLonDelta <= 2097151) {
      return "node-LL5";
    } else if (absLatDelta <= 8388607 && absLonDelta <= 8388607) {
      return "node-LL6";
    } else {
      throw new InvalidNodeLatLonOffsetException("Invalid node lat/long offset: " + latDelta + "/"
          + lonDelta + ". Values must be within a range of -0.8388607/+0.8388607 degrees.");
    }
  }

  /**
   * Converts a node of an {@code xy} path. The delta works as for {@link #convertNodeLL}: a size
   * from {@code node-XY1} to {@code node-XY6}, {@code node-XY} for the smallest that fits, or
   * {@code node-LatLon}. Attribute values are not converted: if the node has any attributes, an
   * empty NodeAttributeSetXY is written, as the legacy converter did.
   */
  private static NodeXY convertNodeXY(OdeTravelerInformationMessage.NodeXY node) {
    String delta = node.getDelta();
    NodeOffsetPointXY offsetPoint = new NodeOffsetPointXY();
    if (delta.startsWith(NODE_XY)) {
      long x = scaleToLong(jsonDecimal(node.getXpos()), 2);
      long y = scaleToLong(jsonDecimal(node.getYpos()), 2);
      if (delta.equals(NODE_XY)) {
        delta = nodeOffsetPointXYType(x, y);
      }
      switch (delta) {
        case "node-XY1" -> {
          Node_XY_20b point = new Node_XY_20b();
          point.setX(new Offset_B10(x));
          point.setY(new Offset_B10(y));
          offsetPoint.setNode_XY1(point);
        }
        case "node-XY2" -> {
          Node_XY_22b point = new Node_XY_22b();
          point.setX(new Offset_B11(x));
          point.setY(new Offset_B11(y));
          offsetPoint.setNode_XY2(point);
        }
        case "node-XY3" -> {
          Node_XY_24b point = new Node_XY_24b();
          point.setX(new Offset_B12(x));
          point.setY(new Offset_B12(y));
          offsetPoint.setNode_XY3(point);
        }
        case "node-XY4" -> {
          Node_XY_26b point = new Node_XY_26b();
          point.setX(new Offset_B13(x));
          point.setY(new Offset_B13(y));
          offsetPoint.setNode_XY4(point);
        }
        case "node-XY5" -> {
          Node_XY_28b point = new Node_XY_28b();
          point.setX(new Offset_B14(x));
          point.setY(new Offset_B14(y));
          offsetPoint.setNode_XY5(point);
        }
        case "node-XY6" -> {
          Node_XY_32b point = new Node_XY_32b();
          point.setX(new Offset_B16(x));
          point.setY(new Offset_B16(y));
          offsetPoint.setNode_XY6(point);
        }
        default -> log.warn("Unsupported node-XY delta: {}", delta);
      }
    } else if (NODE_LAT_LON.equals(delta)) {
      offsetPoint.setNode_LatLon(
          nodeLatLon(jsonDecimal(node.getNodeLat()), jsonDecimal(node.getNodeLong())));
    }
    NodeXY nodeXY = new NodeXY();
    nodeXY.setDelta(offsetPoint);
    if (node.getAttributes() != null) {
      nodeXY.setAttributes(new NodeAttributeSetXY());
    }
    return nodeXY;
  }

  /**
   * The node-XY size for the offsets in centimetres, chosen by bit length: under 2^10 is XY1,
   * under 2^11 XY2, and so on up to under 2^16 for XY6.
   *
   * <p>This is the legacy rule, kept so the output does not change. It allows twice the ASN.1
   * range of each size. For example, XY1's Offset-B10 holds -512..511, yet offsets up to 1023 are
   * put in XY1.</p>
   */
  private static String nodeOffsetPointXYType(long x, long y) {
    long transformed = Math.abs(x) | Math.abs(y);
    if ((transformed & (-1 << 10)) == 0) {
      return "node-XY1";
    } else if ((transformed & (-1 << 11)) == 0) {
      return "node-XY2";
    } else if ((transformed & (-1 << 12)) == 0) {
      return "node-XY3";
    } else if ((transformed & (-1 << 13)) == 0) {
      return "node-XY4";
    } else if ((transformed & (-1 << 14)) == 0) {
      return "node-XY5";
    } else if ((transformed & (-1 << 16)) == 0) {
      return "node-XY6";
    } else {
      throw new IllegalArgumentException("Invalid node X/Y offset: " + x + "/" + y
          + ". Values must be between a range of -327.68/+327.67 meters.");
    }
  }

  private static Node_LLmD_64b nodeLatLon(BigDecimal latitude, BigDecimal longitude) {
    Node_LLmD_64b point = new Node_LLmD_64b();
    point.setLon(new Longitude(scaleToInt(longitude, 7)));
    point.setLat(new Latitude(scaleToInt(latitude, 7)));
    return point;
  }

  /** Converts a geometry. Its extent is a number, the index of the r2024 Extent value. */
  private static GeometricProjection convertGeometry(Region.Geometry geometry) {
    GeometricProjection geometricProjection = new GeometricProjection();
    if (geometry.getDirection() != null) {
      geometricProjection.setDirection(headingSlice(geometry.getDirection()));
    }
    geometricProjection.setExtent(byIndex(Extent.class, geometry.getExtent()));
    if (geometry.getLaneWidth() != null) {
      geometricProjection.setLaneWidth(laneWidth(geometry.getLaneWidth()));
    }
    geometricProjection.setCircle(convertCircle(geometry.getCircle()));
    return geometricProjection;
  }

  /**
   * Converts an oldRegion. Its extent may be a name or an index. Only a circle area is supported;
   * a shapepoint or regionPoint area throws.
   */
  private static ValidRegion convertOldRegion(Region.OldRegion oldRegion)
      throws UnsupportedRegionException {
    ValidRegion validRegion = new ValidRegion();
    if (oldRegion.getDirection() != null) {
      validRegion.setDirection(headingSlice(oldRegion.getDirection()));
    }
    validRegion.setExtent(byName(Extent.class, enumName(ExtentEnum.class, oldRegion.getExtent())));
    if (oldRegion.getArea().getShapepoint() != null || oldRegion.getArea().getRegionPoint() != null) {
      throw new UnsupportedRegionException("Unsupported oldRegion area, only circle is supported");
    }
    ValidRegion.AreaChoice area = new ValidRegion.AreaChoice();
    if (oldRegion.getArea().getCircle() != null) {
      area.setCircle(convertCircle(oldRegion.getArea().getCircle()));
    }
    validRegion.setArea(area);
    return validRegion;
  }

  /**
   * Converts a circle. The request may give the center as {@code position} or {@code center}.
   * Units may be a name or an index: {@code "1"}, {@code "cm2_5"} and {@code "cm2-5"} all
   * become {@code cm2-5}.
   */
  private static Circle convertCircle(Region.Circle circle) {
    Circle result = new Circle();
    result.setCenter(position3D(circle.getPosition() != null ? circle.getPosition() : circle.getCenter()));
    result.setRadius(new Radius_B12(circle.getRadius()));
    if (circle.getUnits() != null) {
      String units = enumName(DistanceUnitsEnum.class, circle.getUnits());
      result.setUnits(byName(DistanceUnits.class, units == null ? null : units.replace('_', '-')));
    }
    return result;
  }

  /** Degrees become units of 10^-7 degree; metres of elevation become decimetres. */
  private static Position3D position3D(OdePosition3D position) {
    Position3D position3D = new Position3D();
    if (position.getLatitude() != null) {
      position3D.setLat(new Latitude(scaleToInt(jsonDecimal(position.getLatitude()), 7)));
    }
    if (position.getLongitude() != null) {
      position3D.setLong_(new Longitude(scaleToInt(jsonDecimal(position.getLongitude()), 7)));
    }
    if (position.getElevation() != null) {
      position3D.setElevation(new Elevation(scaleToInt(jsonDecimal(position.getElevation()), 1)));
    }
    return position3D;
  }

  /** Metres become centimetres. */
  private static LaneWidth laneWidth(BigDecimal laneWidth) {
    return new LaneWidth(scaleToInt(jsonDecimal(laneWidth), 2));
  }

  /** A 16-bit HeadingSlice from its bits as a string of 0s and 1s. */
  private static HeadingSlice headingSlice(String bits) {
    HeadingSlice headingSlice = new HeadingSlice();
    headingSlice.fromBinaryString(bits);
    return headingSlice;
  }

  /**
   * Kept so the output matches the legacy converter. In the legacy path, a decimal from the
   * request went through JSON and was read back as a double. For long decimals, such as
   * elevation {@code 2020.6969900289998}, that changes the last digits, which can change the
   * rounded result. This repeats that step for any value with a fraction or an exponent.
   */
  private static BigDecimal jsonDecimal(BigDecimal value) {
    if (value.scale() > 0 || value.toString().contains("E")) {
      return BigDecimal.valueOf(value.doubleValue());
    }
    return value;
  }

  /** value × 10^power, rounded half up. */
  private static int scaleToInt(BigDecimal value, int power) {
    return value.scaleByPowerOfTen(power).setScale(0, RoundingMode.HALF_UP).intValue();
  }

  /** value × 10^power, rounded half up. */
  private static long scaleToLong(BigDecimal value, int power) {
    return value.scaleByPowerOfTen(power).setScale(0, RoundingMode.HALF_UP).longValue();
  }

  /**
   * The constant name for a request enum value given as a name or as an index. For an index this
   * is the request enum's constant at that position; a name is returned unchanged.
   */
  private static <E extends Enum<?>> String enumName(Class<E> clazz, String nameOrOrdinal) {
    try {
      return clazz.getEnumConstants()[Integer.parseInt(nameOrOrdinal)].toString();
    } catch (NumberFormatException e) {
      return nameOrOrdinal;
    }
  }

  /** The r2024 enum value with this ASN.1 name, or null if there is none. */
  private static <E extends Enum<E> & Asn1Enumerated> E byName(Class<E> clazz, String name) {
    for (E value : clazz.getEnumConstants()) {
      if (value.getName().equals(name)) {
        return value;
      }
    }
    return null;
  }

  /** The r2024 enum value with this ASN.1 index, or null if there is none. */
  private static <E extends Enum<E> & Asn1Enumerated> E byIndex(Class<E> clazz, int index) {
    for (E value : clazz.getEnumConstants()) {
      if (value.getIndex() == index) {
        return value;
      }
    }
    return null;
  }
}
