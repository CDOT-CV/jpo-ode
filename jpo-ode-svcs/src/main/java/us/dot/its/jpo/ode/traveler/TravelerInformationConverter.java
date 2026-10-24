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
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.NodeListLL;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.NodeLL;
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

@Slf4j
public class TravelerInformationConverter {

  private static final String NODE_LL = "node-LL";
  private static final String NODE_XY = "node-XY";
  private static final String NODE_LAT_LON = "node-LatLon";
  private static final long UNKNOWN_MINUTE_OF_THE_YEAR = 527040;

  public static class InvalidNodeLatLonOffsetException extends Exception {
    public InvalidNodeLatLonOffsetException(String message) {
      super(message);
    }
  }

  public static class UnsupportedRegionException extends Exception {
    public UnsupportedRegionException(String message) {
      super(message);
    }
  }

  private TravelerInformationConverter() {
    throw new UnsupportedOperationException();
  }

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

    int startYear = 0;
    long startMinute = UNKNOWN_MINUTE_OF_THE_YEAR;
    try {
      ZonedDateTime zonedDateTime = DateTimeUtils.isoDateTime(dataFrame.getStartDateTime());
      startYear = zonedDateTime.getYear();
      startMinute = (int) ChronoUnit.MINUTES.between(
          DateTimeUtils.isoDateTime(startYear, 1, 1, 0, 0, 0, 0), zonedDateTime);
    } catch (Exception e) {
      log.error("Failed to startDateTime {}, defaulting to unknown value {}.",
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

  private static TravelerDataFrame.ContentChoice convertContent(String content, String[] items) {
    TravelerDataFrame.ContentChoice contentChoice = new TravelerDataFrame.ContentChoice();
    switch (content) {
      case "advisory", "Advisory" -> {
        ITIScodesAndText advisory = new ITIScodesAndText();
        for (String item : items) {
          ITIScodesAndTextSequence.ItemChoice itemChoice = new ITIScodesAndTextSequence.ItemChoice();
          Integer itis = itisCode(item);
          if (itis != null) {
            itemChoice.setItis(new ITIScodes(itis));
          } else {
            itemChoice.setText(new ITIStext(itemText(item)));
          }
          ITIScodesAndTextSequence sequence = new ITIScodesAndTextSequence();
          sequence.setItem(itemChoice);
          advisory.add(sequence);
        }
        contentChoice.setAdvisory(advisory);
      }
      case "workZone" -> {
        WorkZone workZone = new WorkZone();
        for (String item : items) {
          WorkZoneSequence.ItemChoice itemChoice = new WorkZoneSequence.ItemChoice();
          Integer itis = itisCode(item);
          if (itis != null) {
            itemChoice.setItis(new ITIScodes(itis));
          } else {
            itemChoice.setText(new ITIStextPhrase(itemText(item)));
          }
          WorkZoneSequence sequence = new WorkZoneSequence();
          sequence.setItem(itemChoice);
          workZone.add(sequence);
        }
        contentChoice.setWorkZone(workZone);
      }
      case "genericSign" -> {
        GenericSignage genericSign = new GenericSignage();
        for (String item : items) {
          GenericSignageSequence.ItemChoice itemChoice = new GenericSignageSequence.ItemChoice();
          Integer itis = itisCode(item);
          if (itis != null) {
            itemChoice.setItis(new ITIScodes(itis));
          } else {
            itemChoice.setText(new ITIStextPhrase(itemText(item)));
          }
          GenericSignageSequence sequence = new GenericSignageSequence();
          sequence.setItem(itemChoice);
          genericSign.add(sequence);
        }
        contentChoice.setGenericSign(genericSign);
      }
      case "speedLimit" -> {
        SpeedLimit speedLimit = new SpeedLimit();
        for (String item : items) {
          SpeedLimitSequence.ItemChoice itemChoice = new SpeedLimitSequence.ItemChoice();
          Integer itis = itisCode(item);
          if (itis != null) {
            itemChoice.setItis(new ITIScodes(itis));
          } else {
            itemChoice.setText(new ITIStextPhrase(itemText(item)));
          }
          SpeedLimitSequence sequence = new SpeedLimitSequence();
          sequence.setItem(itemChoice);
          speedLimit.add(sequence);
        }
        contentChoice.setSpeedLimit(speedLimit);
      }
      case "exitService" -> {
        ExitService exitService = new ExitService();
        for (String item : items) {
          ExitServiceSequence.ItemChoice itemChoice = new ExitServiceSequence.ItemChoice();
          Integer itis = itisCode(item);
          if (itis != null) {
            itemChoice.setItis(new ITIScodes(itis));
          } else {
            itemChoice.setText(new ITIStextPhrase(itemText(item)));
          }
          ExitServiceSequence sequence = new ExitServiceSequence();
          sequence.setItem(itemChoice);
          exitService.add(sequence);
        }
        contentChoice.setExitService(exitService);
      }
      default -> log.warn("Unsupported TIM content type: {}", content);
    }
    return contentChoice;
  }

  private static Integer itisCode(String item) {
    try {
      return Integer.valueOf(String.valueOf(item));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String itemText(String item) {
    String text = String.valueOf(item);
    return text.startsWith("'") ? text.substring(1) : text;
  }

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

  private static LaneWidth laneWidth(BigDecimal laneWidth) {
    return new LaneWidth(scaleToInt(jsonDecimal(laneWidth), 2));
  }

  private static HeadingSlice headingSlice(String bits) {
    HeadingSlice headingSlice = new HeadingSlice();
    headingSlice.fromBinaryString(bits);
    return headingSlice;
  }

  private static BigDecimal jsonDecimal(BigDecimal value) {
    if (value.scale() > 0 || value.toString().contains("E")) {
      return BigDecimal.valueOf(value.doubleValue());
    }
    return value;
  }

  private static int scaleToInt(BigDecimal value, int power) {
    return value.scaleByPowerOfTen(power).setScale(0, RoundingMode.HALF_UP).intValue();
  }

  private static long scaleToLong(BigDecimal value, int power) {
    return value.scaleByPowerOfTen(power).setScale(0, RoundingMode.HALF_UP).longValue();
  }

  private static <E extends Enum<?>> String enumName(Class<E> clazz, String nameOrOrdinal) {
    try {
      return clazz.getEnumConstants()[Integer.parseInt(nameOrOrdinal)].toString();
    } catch (NumberFormatException e) {
      return nameOrOrdinal;
    }
  }

  private static <E extends Enum<E> & Asn1Enumerated> E byName(Class<E> clazz, String name) {
    for (E value : clazz.getEnumConstants()) {
      if (value.getName().equals(name)) {
        return value;
      }
    }
    return null;
  }

  private static <E extends Enum<E> & Asn1Enumerated> E byIndex(Class<E> clazz, int index) {
    for (E value : clazz.getEnumConstants()) {
      if (value.getIndex() == index) {
        return value;
      }
    }
    return null;
  }
}
