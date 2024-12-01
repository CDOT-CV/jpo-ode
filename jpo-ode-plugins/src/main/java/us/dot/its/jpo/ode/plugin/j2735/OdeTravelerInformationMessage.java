/*******************************************************************************
 * Copyright 2018 572682
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package us.dot.its.jpo.ode.plugin.j2735;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.plugin.asn1.Asn1Object;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region.Circle;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region.OldRegion.RegionPointSet;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region.OldRegion.ShapePointSet;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.DirectionOfUse.DirectionOfUseEnum;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.DistanceUnits.DistanceUnitsEnum;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.Extent.ExtentEnum;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.FrameType;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.MutcdCode;

public class OdeTravelerInformationMessage extends OdeObject {

   private static final long serialVersionUID = -200529140190872305L;

   private int msgCnt;
   private String timeStamp;
   private String packetID;
   private String urlB;
   private DataFrame[] dataframes;
   private transient JsonNode asnDataFrames;

   public int getMsgCnt() {
      return msgCnt;
   }

   public void setMsgCnt(int msgCnt) {
      this.msgCnt = msgCnt;
   }

   public String getTimeStamp() {
      return timeStamp;
   }

   public void setTimeStamp(String timeStamp) {
      this.timeStamp = timeStamp;
   }

   public String getPacketID() {
      return packetID;
   }

   public void setPacketID(String packetID) {
      this.packetID = packetID;
   }

   public DataFrame[] getDataframes() {
      return dataframes;
   }

   public void setDataframes(DataFrame[] dataframes) {
      this.dataframes = dataframes;
   }

   public String getUrlB() {
      return urlB;
   }

   public void setUrlB(String urlB) {
      this.urlB = urlB;
   }

   public JsonNode getAsnDataFrames() {
      return asnDataFrames;
   }

   public void setAsnDataFrames(JsonNode stringDataFrames) {
      this.asnDataFrames = stringDataFrames;
   }

   public boolean equals(final Object o) {
      if (o == this) {
         return true;
      }
      if (!(o instanceof OdeTravelerInformationMessage)) {
         return false;
      }
      final OdeTravelerInformationMessage other = (OdeTravelerInformationMessage) o;
      if (!other.canEqual((Object) this)) {
         return false;
      }
      if (this.getMsgCnt() != other.getMsgCnt()) {
         return false;
      }
      final Object this$timeStamp = this.getTimeStamp();
      final Object other$timeStamp = other.getTimeStamp();
      if (this$timeStamp == null ? other$timeStamp != null :
          !this$timeStamp.equals(other$timeStamp)) {
         return false;
      }
      final Object this$packetID = this.getPacketID();
      final Object other$packetID = other.getPacketID();
      if (this$packetID == null ? other$packetID != null : !this$packetID.equals(other$packetID)) {
         return false;
      }
      final Object this$urlB = this.getUrlB();
      final Object other$urlB = other.getUrlB();
      if (this$urlB == null ? other$urlB != null : !this$urlB.equals(other$urlB)) {
         return false;
      }
      if (!java.util.Arrays.deepEquals(this.getDataframes(), other.getDataframes())) {
         return false;
      }
      return true;
   }

   protected boolean canEqual(final Object other) {
      return other instanceof OdeTravelerInformationMessage;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.getMsgCnt();
      final Object $timeStamp = this.getTimeStamp();
      result = result * PRIME + ($timeStamp == null ? 43 : $timeStamp.hashCode());
      final Object $packetID = this.getPacketID();
      result = result * PRIME + ($packetID == null ? 43 : $packetID.hashCode());
      final Object $urlB = this.getUrlB();
      result = result * PRIME + ($urlB == null ? 43 : $urlB.hashCode());
      result = result * PRIME + java.util.Arrays.deepHashCode(this.getDataframes());
      return result;
   }

   public static class NodeListXY extends OdeObject {
      private static final long serialVersionUID = 1L;
      private ComputedLane computedLane;
      private NodeXY[] nodexy;

      public ComputedLane getComputedLane() {
         return computedLane;
      }

      public void setComputedLane(ComputedLane computedLane) {
         this.computedLane = computedLane;
      }

      public NodeXY[] getNodexy() {
         return nodexy;
      }

      public void setNodexy(NodeXY[] nodexy) {
         this.nodexy = nodexy;
      }
   }

   public static class Area extends OdeObject {
      private static final long serialVersionUID = 1L;

      private ShapePointSet shapepoint;
      private Circle circle;
      private RegionPointSet regionPoint;

      public ShapePointSet getShapepoint() {
         return shapepoint;
      }

      public void setShapepoint(ShapePointSet shapepoint) {
         this.shapepoint = shapepoint;
      }

      public Circle getCircle() {
         return circle;
      }

      public void setCircle(Circle circle) {
         this.circle = circle;
      }

      public RegionPointSet getRegionPoint() {
         return regionPoint;
      }

      public void setRegionPoint(RegionPointSet regionPoint) {
         this.regionPoint = regionPoint;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         Area area = (Area) o;
         return Objects.equals(shapepoint, area.shapepoint) &&
             Objects.equals(circle, area.circle) &&
             Objects.equals(regionPoint, area.regionPoint);
      }

      @Override
      public int hashCode() {
         return Objects.hash(shapepoint, circle, regionPoint);
      }
   }

   public static class ComputedLane extends OdeObject {

      private static final long serialVersionUID = 7337344402648755924L;
      private int referenceLaneId;
      private BigDecimal offsetXaxis;
      private BigDecimal offsetYaxis;
      private BigDecimal rotateXY;
      private BigDecimal scaleXaxis;
      private BigDecimal scaleYaxis;

      public int getReferenceLaneId() {
         return referenceLaneId;
      }

      public void setReferenceLaneId(int referenceLaneId) {
         this.referenceLaneId = referenceLaneId;
      }

      public BigDecimal getOffsetXaxis() {
         return offsetXaxis;
      }

      public void setOffsetXaxis(BigDecimal offsetXaxis) {
         this.offsetXaxis = offsetXaxis;
      }

      public BigDecimal getOffsetYaxis() {
         return offsetYaxis;
      }

      public void setOffsetYaxis(BigDecimal offsetYaxis) {
         this.offsetYaxis = offsetYaxis;
      }

      public BigDecimal getRotateXY() {
         return rotateXY;
      }

      public void setRotateXY(BigDecimal rotateXY) {
         this.rotateXY = rotateXY;
      }

      public BigDecimal getScaleXaxis() {
         return scaleXaxis;
      }

      public void setScaleXaxis(BigDecimal scaleXaxis) {
         this.scaleXaxis = scaleXaxis;
      }

      public BigDecimal getScaleYaxis() {
         return scaleYaxis;
      }

      public void setScaleYaxis(BigDecimal scaleYaxis) {
         this.scaleYaxis = scaleYaxis;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         ComputedLane that = (ComputedLane) o;
         return referenceLaneId == that.referenceLaneId &&
             Objects.equals(offsetXaxis, that.offsetXaxis) &&
             Objects.equals(offsetYaxis, that.offsetYaxis) &&
             Objects.equals(rotateXY, that.rotateXY) &&
             Objects.equals(scaleXaxis, that.scaleXaxis) &&
             Objects.equals(scaleYaxis, that.scaleYaxis);
      }

      @Override
      public int hashCode() {
         return Objects.hash(referenceLaneId, offsetXaxis, offsetYaxis, rotateXY, scaleXaxis,
             scaleYaxis);
      }
   }

   public static class NodeXY extends OdeObject {

      private static final long serialVersionUID = -3250256624514759524L;
      private String delta;
      private BigDecimal nodeLat;
      private BigDecimal nodeLong;
      private BigDecimal x;
      private BigDecimal y;
      private Attributes attributes;

      public String getDelta() {
         return delta;
      }

      public void setDelta(String delta) {
         this.delta = delta;
      }

      public BigDecimal getNodeLat() {
         return nodeLat;
      }

      public void setNodeLat(BigDecimal nodeLat) {
         this.nodeLat = nodeLat;
      }

      public BigDecimal getNodeLong() {
         return nodeLong;
      }

      public void setNodeLong(BigDecimal nodeLong) {
         this.nodeLong = nodeLong;
      }

      public BigDecimal getX() {
         return x;
      }

      public void setX(BigDecimal x) {
         this.x = x;
      }

      public BigDecimal getY() {
         return y;
      }

      public void setY(BigDecimal y) {
         this.y = y;
      }

      public Attributes getAttributes() {
         return attributes;
      }

      public void setAttributes(Attributes attributes) {
         this.attributes = attributes;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         NodeXY nodeXY = (NodeXY) o;
         return Objects.equals(delta, nodeXY.delta) &&
             Objects.equals(nodeLat, nodeXY.nodeLat) &&
             Objects.equals(nodeLong, nodeXY.nodeLong) &&
             Objects.equals(x, nodeXY.x) && Objects.equals(y, nodeXY.y) &&
             Objects.equals(attributes, nodeXY.attributes);
      }

      @Override
      public int hashCode() {
         return Objects.hash(delta, nodeLat, nodeLong, x, y, attributes);
      }
   }

   public static class LocalNode extends OdeObject {

      private static final long serialVersionUID = 3872400520330034244L;
      private long type;

      public long getType() {
         return type;
      }

      public void setType(long type) {
         this.type = type;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         LocalNode localNode = (LocalNode) o;
         return type == localNode.type;
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(type);
      }
   }

   public static class DisabledList extends OdeObject {

      private static final long serialVersionUID = 1009869811306803991L;
      private long type;

      public long getType() {
         return type;
      }

      public void setType(long type) {
         this.type = type;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         DisabledList that = (DisabledList) o;
         return type == that.type;
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(type);
      }
   }

   public static class EnabledList extends OdeObject {

      private static final long serialVersionUID = 5797889223766230223L;
      private long type;

      public long getType() {
         return type;
      }

      public void setType(long type) {
         this.type = type;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         EnabledList that = (EnabledList) o;
         return type == that.type;
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(type);
      }
   }

   public static class SpeedLimits extends OdeObject {

      private static final long serialVersionUID = -8729406522600137038L;
      private long type;
      private BigDecimal velocity;

      public long getType() {
         return type;
      }

      public void setType(long type) {
         this.type = type;
      }

      public BigDecimal getVelocity() {
         return velocity;
      }

      public void setVelocity(BigDecimal velocity) {
         this.velocity = velocity;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         SpeedLimits that = (SpeedLimits) o;
         return type == that.type && Objects.equals(velocity, that.velocity);
      }

      @Override
      public int hashCode() {
         return Objects.hash(type, velocity);
      }
   }

   public static class DataList extends OdeObject {

      private static final long serialVersionUID = -1391200532738540024L;
      private int pathEndpointAngle;
      private BigDecimal laneCrownCenter;
      private BigDecimal laneCrownLeft;
      private BigDecimal laneCrownRight;
      private BigDecimal laneAngle;
      private SpeedLimits[] speedLimits;

      public int getPathEndpointAngle() {
         return pathEndpointAngle;
      }

      public void setPathEndpointAngle(int pathEndpointAngle) {
         this.pathEndpointAngle = pathEndpointAngle;
      }

      public BigDecimal getLaneCrownCenter() {
         return laneCrownCenter;
      }

      public void setLaneCrownCenter(BigDecimal laneCrownCenter) {
         this.laneCrownCenter = laneCrownCenter;
      }

      public BigDecimal getLaneCrownLeft() {
         return laneCrownLeft;
      }

      public void setLaneCrownLeft(BigDecimal laneCrownLeft) {
         this.laneCrownLeft = laneCrownLeft;
      }

      public BigDecimal getLaneCrownRight() {
         return laneCrownRight;
      }

      public void setLaneCrownRight(BigDecimal laneCrownRight) {
         this.laneCrownRight = laneCrownRight;
      }

      public BigDecimal getLaneAngle() {
         return laneAngle;
      }

      public void setLaneAngle(BigDecimal laneAngle) {
         this.laneAngle = laneAngle;
      }

      public SpeedLimits[] getSpeedLimits() {
         return speedLimits;
      }

      public void setSpeedLimits(SpeedLimits[] speedLimits) {
         this.speedLimits = speedLimits;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         DataList dataList = (DataList) o;
         return pathEndpointAngle == dataList.pathEndpointAngle &&
             Objects.equals(laneCrownCenter, dataList.laneCrownCenter) &&
             Objects.equals(laneCrownLeft, dataList.laneCrownLeft) &&
             Objects.equals(laneCrownRight, dataList.laneCrownRight) &&
             Objects.equals(laneAngle, dataList.laneAngle) &&
             Objects.deepEquals(speedLimits, dataList.speedLimits);
      }

      @Override
      public int hashCode() {
         return Objects.hash(pathEndpointAngle, laneCrownCenter, laneCrownLeft, laneCrownRight,
             laneAngle,
             Arrays.hashCode(speedLimits));
      }
   }

   public static class Attributes extends OdeObject {

      private static final long serialVersionUID = -6476758554962944513L;
      private LocalNode[] localNodes;
      private DisabledList[] disabledLists;
      private EnabledList[] enabledLists;
      private DataList[] dataLists;
      private BigDecimal dWidth;
      private BigDecimal dElevation;

      public LocalNode[] getLocalNodes() {
         return localNodes;
      }

      public void setLocalNodes(LocalNode[] localNodes) {
         this.localNodes = localNodes;
      }

      public DisabledList[] getDisabledLists() {
         return disabledLists;
      }

      public void setDisabledLists(DisabledList[] disabledLists) {
         this.disabledLists = disabledLists;
      }

      public EnabledList[] getEnabledLists() {
         return enabledLists;
      }

      public void setEnabledLists(EnabledList[] enabledLists) {
         this.enabledLists = enabledLists;
      }

      public DataList[] getDataLists() {
         return dataLists;
      }

      public void setDataLists(DataList[] dataLists) {
         this.dataLists = dataLists;
      }

      public BigDecimal getdWidth() {
         return dWidth;
      }

      public void setdWidth(BigDecimal dWidth) {
         this.dWidth = dWidth;
      }

      public BigDecimal getdElevation() {
         return dElevation;
      }

      public void setdElevation(BigDecimal dElevation) {
         this.dElevation = dElevation;
      }

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         Attributes that = (Attributes) o;
         return Objects.deepEquals(localNodes, that.localNodes) &&
             Objects.deepEquals(disabledLists, that.disabledLists) &&
             Objects.deepEquals(enabledLists, that.enabledLists) &&
             Objects.deepEquals(dataLists, that.dataLists) &&
             Objects.equals(dWidth, that.dWidth) &&
             Objects.equals(dElevation, that.dElevation);
      }

      @Override
      public int hashCode() {
         return Objects.hash(Arrays.hashCode(localNodes), Arrays.hashCode(disabledLists),
             Arrays.hashCode(enabledLists), Arrays.hashCode(dataLists), dWidth, dElevation);
      }
   }

   public static class DataFrame extends OdeObject {

      private static final long serialVersionUID = 537503046055742396L;
      @JsonAlias({"sspTimRights", "notUsed"})
      private short doNotUse1;// Start Header Information
      private FrameType.TravelerInfoType frameType;
      private MsgId msgId;
      private String startDateTime;
      @JsonAlias("duratonTime")
      private int durationTime;
      private int priority;// End header Information
      @JsonAlias({"sspLocationRights", "notUsed1"})
      private short doNotUse2;// Start Region Information
      private Region[] regions;
      @JsonAlias({"sspMsgContent", "sspMsgRights1", "notUsed2"})
      private short doNotUse3;// Start content Information
      @JsonAlias({"sspMsgTypes", "sspMsgRights2", "notUsed3"})
      private short doNotUse4;
      private String content;
      private String[] items;
      private String url;// End content Information

      @Override
      public boolean equals(Object o) {
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         DataFrame dataFrame = (DataFrame) o;
         return doNotUse1 == dataFrame.doNotUse1 && durationTime == dataFrame.durationTime &&
             priority == dataFrame.priority && doNotUse2 == dataFrame.doNotUse2 &&
             doNotUse3 == dataFrame.doNotUse3 && doNotUse4 == dataFrame.doNotUse4 &&
             frameType == dataFrame.frameType && Objects.equals(msgId, dataFrame.msgId) &&
             Objects.equals(startDateTime, dataFrame.startDateTime) &&
             Objects.deepEquals(regions, dataFrame.regions) &&
             Objects.equals(content, dataFrame.content) &&
             Objects.deepEquals(items, dataFrame.items) &&
             Objects.equals(url, dataFrame.url);
      }

      @Override
      public int hashCode() {
         return Objects.hash(doNotUse1, frameType, msgId, startDateTime, durationTime, priority,
             doNotUse2,
             Arrays.hashCode(regions), doNotUse3, doNotUse4, content, Arrays.hashCode(items), url);
      }

      public static class Region extends OdeObject {

         private static final long serialVersionUID = 8011973280114768008L;
         private String name;
         private int regulatorID;
         private int segmentID;
         private OdePosition3D anchorPosition;
         private BigDecimal laneWidth;
         private String directionality;
         private boolean closedPath;
         private String direction;
         private String description;
         private Path path;
         private Geometry geometry;
         private OldRegion oldRegion;

         @Override
         public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
               return false;
            }
            Region region = (Region) o;
            return regulatorID == region.regulatorID && segmentID == region.segmentID &&
                closedPath == region.closedPath && Objects.equals(name, region.name) &&
                Objects.equals(anchorPosition, region.anchorPosition) &&
                Objects.equals(laneWidth, region.laneWidth) &&
                Objects.equals(directionality, region.directionality) &&
                Objects.equals(direction, region.direction) &&
                Objects.equals(description, region.description) &&
                Objects.equals(path, region.path) &&
                Objects.equals(geometry, region.geometry) &&
                Objects.equals(oldRegion, region.oldRegion);
         }

         @Override
         public int hashCode() {
            return Objects.hash(name, regulatorID, segmentID, anchorPosition, laneWidth,
                directionality,
                closedPath, direction, description, path, geometry, oldRegion);
         }

         public static class OldRegion extends OdeObject {

            private static final long serialVersionUID = 1L;
            private String direction;
            private String extent;
            private Area area;

            @Override
            public boolean equals(Object o) {
               if (o == null || getClass() != o.getClass()) {
                  return false;
               }
               OldRegion oldRegion = (OldRegion) o;
               return Objects.equals(direction, oldRegion.direction) &&
                   Objects.equals(extent, oldRegion.extent) &&
                   Objects.equals(area, oldRegion.area);
            }

            @Override
            public int hashCode() {
               return Objects.hash(direction, extent, area);
            }

            public static class ShapePointSet extends OdeObject {

               private static final long serialVersionUID = 1L;
               private OdePosition3D anchor;
               private BigDecimal laneWidth;
               private int directionality;
               private NodeListXY nodeList;

               public OdePosition3D getAnchor() {
                  return anchor;
               }

               public void setAnchor(OdePosition3D anchor) {
                  this.anchor = anchor;
               }

               public BigDecimal getLaneWidth() {
                  return laneWidth;
               }

               public void setLaneWidth(BigDecimal laneWidth) {
                  this.laneWidth = laneWidth;
               }

               public int getDirectionality() {
                  return directionality;
               }

               public void setDirectionality(int directionality) {
                  this.directionality = directionality;
               }

               public void setDirectionalityEnum(DirectionOfUseEnum directionalityEnum) {
                  this.directionality = directionalityEnum.ordinal();
               }

               public NodeListXY getNodeList() {
                  return nodeList;
               }

               public void setNodeList(NodeListXY nodeList) {
                  this.nodeList = nodeList;
               }

               @Override
               public boolean equals(Object o) {
                  if (o == null || getClass() != o.getClass()) {
                     return false;
                  }
                  ShapePointSet that = (ShapePointSet) o;
                  return directionality == that.directionality &&
                      Objects.equals(anchor, that.anchor) &&
                      Objects.equals(laneWidth, that.laneWidth) &&
                      Objects.equals(nodeList, that.nodeList);
               }

               @Override
               public int hashCode() {
                  return Objects.hash(anchor, laneWidth, directionality, nodeList);
               }
            }

            public static class RegionPointSet extends OdeObject {

               private static final long serialVersionUID = 1L;
               private OdePosition3D position;
               private int scale;
               private RegionList[] regionList;

               @Override
               public boolean equals(Object o) {
                  if (o == null || getClass() != o.getClass()) {
                     return false;
                  }
                  RegionPointSet that = (RegionPointSet) o;
                  return scale == that.scale && Objects.equals(position, that.position) &&
                      Objects.deepEquals(regionList, that.regionList);
               }

               @Override
               public int hashCode() {
                  return Objects.hash(position, scale, Arrays.hashCode(regionList));
               }

               public static class RegionList extends OdeObject {

                  private static final long serialVersionUID = -5307620155601900634L;
                  private BigDecimal xOffset;
                  private BigDecimal yOffset;
                  private BigDecimal zOffset;

                  public BigDecimal getzOffset() {
                     return zOffset;
                  }

                  public void setzOffset(BigDecimal zOffset) {
                     this.zOffset = zOffset;
                  }

                  public BigDecimal getyOffset() {
                     return yOffset;
                  }

                  public void setyOffset(BigDecimal yOffset) {
                     this.yOffset = yOffset;
                  }

                  public BigDecimal getxOffset() {
                     return xOffset;
                  }

                  public void setxOffset(BigDecimal xOffset) {
                     this.xOffset = xOffset;
                  }

                  @Override
                  public boolean equals(Object o) {
                     if (o == null || getClass() != o.getClass()) {
                        return false;
                     }
                     RegionList that = (RegionList) o;
                     return Objects.equals(xOffset, that.xOffset) &&
                         Objects.equals(yOffset, that.yOffset) &&
                         Objects.equals(zOffset, that.zOffset);
                  }

                  @Override
                  public int hashCode() {
                     return Objects.hash(xOffset, yOffset, zOffset);
                  }
               }

               public RegionList[] getRegionList() {
                  return regionList;
               }

               public void setRegionList(RegionList[] regionList) {
                  this.regionList = regionList;
               }

               public int getScale() {
                  return scale;
               }

               public void setScale(int scale) {
                  this.scale = scale;
               }

               public OdePosition3D getPosition() {
                  return position;
               }

               public void setPosition(OdePosition3D position) {
                  this.position = position;
               }
            }

            public Area getArea() {
               return area;
            }

            public void setArea(Area area) {
               this.area = area;
            }

            public String getExtent() {
               return extent;
            }

            public void setExtent(String extent) {
               this.extent = extent;
            }

            public void setExtent(ExtentEnum extent) {
               this.extent = extent.name();
            }

            public String getDirection() {
               return direction;
            }

            public void setDirection(String direction) {
               this.direction = direction;
            }
         }

         public static class Geometry extends OdeObject {

            private static final long serialVersionUID = -7664796173893464468L;
            private String direction;
            private int extent;
            private BigDecimal laneWidth;
            private Circle circle;

            public Circle getCircle() {
               return circle;
            }

            public void setCircle(Circle circle) {
               this.circle = circle;
            }

            public BigDecimal getLaneWidth() {
               return laneWidth;
            }

            public void setLaneWidth(BigDecimal laneWidth) {
               this.laneWidth = laneWidth;
            }

            public int getExtent() {
               return extent;
            }

            public void setExtent(int extent) {
               this.extent = extent;
            }

            public String getDirection() {
               return direction;
            }

            public void setDirection(String direction) {
               this.direction = direction;
            }
         }

         public static class Circle extends OdeObject {

            private static final long serialVersionUID = -8156052898034497978L;
            private OdePosition3D position;
            private OdePosition3D center;
            private int radius;
            private String units;

            public String getUnits() {
               return units;
            }

            public void setUnits(String units) {
               this.units = units;
            }

            public void setUnits(DistanceUnitsEnum units) {
               this.units = units.name();
            }

            public int getRadius() {
               return radius;
            }

            public void setRadius(int radius) {
               this.radius = radius;
            }

            public OdePosition3D getPosition() {
               return position;
            }

            public void setPosition(OdePosition3D position) {
               this.position = position;
            }

            public OdePosition3D getCenter() {
               return center;
            }

            public void setCenter(OdePosition3D center) {
               this.center = center;
            }

            @Override
            public boolean equals(Object o) {
               if (o == null || getClass() != o.getClass()) {
                  return false;
               }
               Circle circle = (Circle) o;
               return radius == circle.radius && Objects.equals(position, circle.position) &&
                   Objects.equals(center, circle.center) &&
                   Objects.equals(units, circle.units);
            }

            @Override
            public int hashCode() {
               return Objects.hash(position, center, radius, units);
            }
         }

         public static class Path extends OdeObject {

            private static final long serialVersionUID = 3293758823626661508L;
            private int scale;
            private String type;
            private NodeXY[] nodes;
            private ComputedLane computedLane;

            public ComputedLane getComputedLane() {
               return computedLane;
            }

            public void setComputedLane(ComputedLane computedLane) {
               this.computedLane = computedLane;
            }

            public NodeXY[] getNodes() {
               return nodes;
            }

            public void setNodes(NodeXY[] nodes) {
               this.nodes = nodes;
            }

            public String getType() {
               return type;
            }

            public void setType(String type) {
               this.type = type;
            }

            public int getScale() {
               return scale;
            }

            public void setScale(int scale) {
               this.scale = scale;
            }

            @Override
            public boolean equals(Object o) {
               if (o == null || getClass() != o.getClass()) {
                  return false;
               }
               Path path = (Path) o;
               return scale == path.scale && Objects.equals(type, path.type) &&
                   Objects.deepEquals(nodes, path.nodes) &&
                   Objects.equals(computedLane, path.computedLane);
            }

            @Override
            public int hashCode() {
               return Objects.hash(scale, type, Arrays.hashCode(nodes), computedLane);
            }
         }

         public OldRegion getOldRegion() {
            return oldRegion;
         }

         public void setOldRegion(OldRegion oldRegion) {
            this.oldRegion = oldRegion;
         }

         public Geometry getGeometry() {
            return geometry;
         }

         public void setGeometry(Geometry geometry) {
            this.geometry = geometry;
         }

         public Path getPath() {
            return path;
         }

         public void setPath(Path path) {
            this.path = path;
         }

         public String getDescription() {
            return description;
         }

         public void setDescription(String description) {
            this.description = description;
         }

         public String getDirection() {
            return direction;
         }

         public void setDirection(String direction) {
            this.direction = direction;
         }

         public boolean isClosedPath() {
            return closedPath;
         }

         public void setClosedPath(boolean closedPath) {
            this.closedPath = closedPath;
         }

         public String getDirectionality() {
            return directionality;
         }

         public void setDirectionality(String directionality) {
            this.directionality = directionality;
         }

         public BigDecimal getLaneWidth() {
            return laneWidth;
         }

         public void setLaneWidth(BigDecimal laneWidth) {
            this.laneWidth = laneWidth;
         }

         public OdePosition3D getAnchorPosition() {
            return anchorPosition;
         }

         public void setAnchorPosition(OdePosition3D anchorPosition) {
            this.anchorPosition = anchorPosition;
         }

         public int getSegmentID() {
            return segmentID;
         }

         public void setSegmentID(int segmentID) {
            this.segmentID = segmentID;
         }

         public int getRegulatorID() {
            return regulatorID;
         }

         public void setRegulatorID(int regulatorID) {
            this.regulatorID = regulatorID;
         }

         public String getName() {
            return name;
         }

         public void setName(String name) {
            this.name = name;
         }

      }

      public static class RoadSignID extends OdeObject {

         private static final long serialVersionUID = 1L;

         private OdePosition3D position;
         private String viewAngle;
         private MutcdCode.MutcdCodeEnum mutcdCode;
         private String crc;

         public OdePosition3D getPosition() {
            return position;
         }

         public void setPosition(OdePosition3D position) {
            this.position = position;
         }

         public String getViewAngle() {
            return viewAngle;
         }

         public void setViewAngle(String viewAngle) {
            this.viewAngle = viewAngle;
         }

         public MutcdCode.MutcdCodeEnum getMutcdCode() {
            return mutcdCode;
         }

         public void setMutcdCode(MutcdCode.MutcdCodeEnum mutcdCode) {
            this.mutcdCode = mutcdCode;
         }

         public String getCrc() {
            return crc;
         }

         public void setCrc(String crc) {
            this.crc = crc;
         }

         @Override
         public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
               return false;
            }
            RoadSignID that = (RoadSignID) o;
            return Objects.equals(position, that.position) &&
                Objects.equals(viewAngle, that.viewAngle) && mutcdCode == that.mutcdCode &&
                Objects.equals(crc, that.crc);
         }

         @Override
         public int hashCode() {
            return Objects.hash(position, viewAngle, mutcdCode, crc);
         }
      }

      public static class MsgId extends Asn1Object {
         private static final long serialVersionUID = 1L;

         private RoadSignID roadSignID;
         private String furtherInfoID;

         public RoadSignID getRoadSignID() {
            return roadSignID;
         }

         public void setRoadSignID(RoadSignID roadSignID) {
            this.roadSignID = roadSignID;
         }

         public String getFurtherInfoID() {
            return furtherInfoID;
         }

         public void setFurtherInfoID(String furtherInfoID) {
            this.furtherInfoID = furtherInfoID;
         }

         @Override
         public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
               return false;
            }
            MsgId msgId = (MsgId) o;
            return Objects.equals(roadSignID, msgId.roadSignID) &&
                Objects.equals(furtherInfoID, msgId.furtherInfoID);
         }

         @Override
         public int hashCode() {
            return Objects.hash(roadSignID, furtherInfoID);
         }
      }

      public MsgId getMsgId() {
         return msgId;
      }

      public void setMsgId(MsgId msgId) {
         this.msgId = msgId;
      }

      public String getUrl() {
         return url;
      }

      public void setUrl(String url) {
         this.url = url;
      }

      public String[] getItems() {
         return items;
      }

      public void setItems(String[] items) {
         this.items = items;
      }

      public String getContent() {
         return content;
      }

      public void setContent(String content) {
         this.content = content;
      }

      public Region[] getRegions() {
         return regions;
      }

      public void setRegions(Region[] regions) {
         this.regions = regions;
      }

      public int getPriority() {
         return priority;
      }

      public void setPriority(int priority) {
         this.priority = priority;
      }

      public int getDurationTime() {
         return durationTime;
      }

      public void setDurationTime(int durationTime) {
         this.durationTime = durationTime;
      }

      public String getStartDateTime() {
         return startDateTime;
      }

      public void setStartDateTime(String startDateTime) {
         this.startDateTime = startDateTime;
      }

      public FrameType.TravelerInfoType getFrameType() {
         return frameType;
      }

      public void setFrameType(FrameType.TravelerInfoType frameType) {
         this.frameType = frameType;
      }

      public short getDoNotUse1() {
         return doNotUse1;
      }

      public void setDoNotUse1(short doNotUse1) {
         this.doNotUse1 = doNotUse1;
      }

      public short getDoNotUse2() {
         return doNotUse2;
      }

      public void setDoNotUse2(short doNotUse2) {
         this.doNotUse2 = doNotUse2;
      }

      public short getDoNotUse3() {
         return doNotUse3;
      }

      public void setDoNotUse3(short doNotUse3) {
         this.doNotUse3 = doNotUse3;
      }

      public short getDoNotUse4() {
         return doNotUse4;
      }

      public void setDoNotUse4(short doNotUse4) {
         this.doNotUse4 = doNotUse4;
      }

   }

}
