/*=============================================================================
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

package us.dot.its.jpo.ode.importer.parser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import us.dot.its.jpo.ode.model.OdeBsmMetadata;
import us.dot.its.jpo.ode.model.OdeBsmMetadata.BsmSource;
import us.dot.its.jpo.ode.model.OdeLogMetadata;
import us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType;
import us.dot.its.jpo.ode.model.OdeLogMsgMetadataLocation;
import us.dot.its.jpo.ode.model.OdeSpatMetadata;
import us.dot.its.jpo.ode.model.OdeSpatMetadata.SpatSource;
import us.dot.its.jpo.ode.model.ReceivedMessageDetails;
import us.dot.its.jpo.ode.model.RxSource;
import us.dot.its.jpo.ode.plugin.j2735.builders.ElevationBuilder;
import us.dot.its.jpo.ode.plugin.j2735.builders.HeadingBuilder;
import us.dot.its.jpo.ode.plugin.j2735.builders.LatitudeBuilder;
import us.dot.its.jpo.ode.plugin.j2735.builders.LongitudeBuilder;
import us.dot.its.jpo.ode.plugin.j2735.builders.SpeedOrVelocityBuilder;
import us.dot.its.jpo.ode.util.DateTimeUtils;

/**
 * Abstract class LogFileParser providing common functionalities and structure
 * for parsing log files. It defines the methods and state required to parse
 * different types of log files and update the corresponding metadata. Implementing
 * classes should provide specific parsing logic based on the log file format.
 */
@Setter
@Getter
public abstract class LogFileParser implements FileParser {

  public static final int BUFFER_SIZE = 4096;

  protected byte[] readBuffer = new byte[BUFFER_SIZE];
  protected int step = 0;

  protected String filename;
  protected RecordType recordType;

  protected IntersectionParser intersectionParser;
  protected LocationParser locationParser;
  protected TimeParser timeParser;
  protected SecurityResultCodeParser secResCodeParser;
  protected PayloadParser payloadParser;

  /**
   * Parses a file provided as a BufferedInputStream and updates the parser's state accordingly.
   *
   * @param bis      the input stream containing the file data to parse
   * @param fileName the name of the file being parsed
   *
   * @return the status of the parsing operation, represented as a {@link ParserStatus} enum
   *
   * @throws FileParserException if an error occurs during the parsing process
   */
  public ParserStatus parseFile(BufferedInputStream bis, String fileName) throws FileParserException {

    ParserStatus status = ParserStatus.INIT;
    if (getStep() == 0) {
      setFilename(fileName);
      setStep(getStep() + 1);
    }
    status = ParserStatus.COMPLETE;

    return status;
  }

  protected ParserStatus parseStep(BufferedInputStream bis, int length) throws FileParserException {
    if (length > BUFFER_SIZE) {
      throw new FileParserException(
          "Data size of " + length + " is larger than allocated buffer size of " + BUFFER_SIZE);
    }

    try {
      int numBytes;
      if (bis.markSupported()) {
        bis.mark(length);
      }
      numBytes = bis.read(readBuffer, 0, length);
      if (numBytes < 0) {
        return ParserStatus.EOF;
      } else if (numBytes < length) {
        if (bis.markSupported()) {
          try {
            bis.reset();
          } catch (IOException ioe) {
            throw new FileParserException("Error resetting Input Stream to marked position", ioe);
          }
        }
        return ParserStatus.PARTIAL;
      } else {
        step++;
        return ParserStatus.COMPLETE;
      }
    } catch (Exception e) {
      throw new FileParserException("Error parsing step " + step, e);
    }
  }

  protected int resetStep() {
    setStep(0);
    return getStep();
  }

  protected ParserStatus nextStep(BufferedInputStream bis, String fileName, LogFileParser parser)
      throws FileParserException {

    ParserStatus status = parser.parseFile(bis, fileName);
    if (status == ParserStatus.COMPLETE) {
      step++;
    }
    return status;
  }

  /**
   * Updates the given metadata object with various attributes derived from the current
   * state of the parser.
   *
   * @param metadata the metadata object to be updated with information such as log filename,
   *                 record type, generated time, security result code, and received message details.
   *                 Specific handling is applied if the metadata is an instance of
   *                 {@code OdeBsmMetadata} or {@code OdeSpatMetadata}, involving additional
   *                 attributes like BSM source or SPAT source.
   */
  public void updateMetadata(OdeLogMetadata metadata) {
    metadata.setLogFileName(getFilename());
    metadata.setRecordType(getRecordType());
    metadata.setRecordGeneratedAt(DateTimeUtils.isoDateTime(getTimeParser().getGeneratedAt()));

    if (getSecResCodeParser() != null) {
      metadata.setSecurityResultCode(getSecResCodeParser().getSecurityResultCode());
    }

    metadata.setReceivedMessageDetails(buildReceivedMessageDetails(this));

    if (metadata instanceof OdeBsmMetadata) {
      OdeBsmMetadata odeBsmMetadata = (OdeBsmMetadata) metadata;
      BsmSource bsmSource = BsmSource.unknown;
      if (this instanceof BsmLogFileParser) {
        BsmLogFileParser bsmLogFileParser = (BsmLogFileParser) this;
        bsmSource = bsmLogFileParser.getBsmSource();
      } else if (this instanceof RxMsgFileParser) {
        RxMsgFileParser rxMsgFileParser = (RxMsgFileParser) this;
        if (rxMsgFileParser.getRxSource() == RxSource.RV) {
          bsmSource = BsmSource.RV;
        }
      }
      odeBsmMetadata.setBsmSource(bsmSource);
    }
    if (metadata instanceof OdeSpatMetadata) {
      OdeSpatMetadata odeSpatMetadata = (OdeSpatMetadata) metadata;
      SpatSource spatSource = SpatSource.unknown;
      boolean isCertPresent = true; /*ieee 1609 (acceptable values 0 = no,1 =yes by default the Cert shall be present)*/
      if (this instanceof SpatLogFileParser) {
        SpatLogFileParser spatLogFileParser = (SpatLogFileParser) this;
        spatSource = spatLogFileParser.getSpatSource();
        isCertPresent = spatLogFileParser.isCertPresent(); //update
      }
      odeSpatMetadata.setSpatSource(spatSource);
      odeSpatMetadata.setIsCertPresent(isCertPresent);
    }

    metadata.calculateGeneratedBy();
  }

  private static ReceivedMessageDetails buildReceivedMessageDetails(LogFileParser parser) {
    LocationParser locationParser = parser.getLocationParser();
    ReceivedMessageDetails rxMsgDetails = null;
    if (locationParser != null) {
      LogLocation locationDetails = locationParser.getLocation();
      BigDecimal genericLatitude = LatitudeBuilder.genericLatitude(locationDetails.getLatitude());
      BigDecimal genericLongitude = LongitudeBuilder.genericLongitude(locationDetails.getLongitude());
      BigDecimal genericElevation = ElevationBuilder.genericElevation(locationDetails.getElevation());
      BigDecimal genericSpeedOrVelocity = SpeedOrVelocityBuilder
          .genericSpeedOrVelocity(locationDetails.getSpeed());
      BigDecimal genericHeading = HeadingBuilder.genericHeading(locationDetails.getHeading());
      rxMsgDetails = new ReceivedMessageDetails(new OdeLogMsgMetadataLocation(
          genericLatitude == null ? null : genericLatitude.stripTrailingZeros().toPlainString(),
          genericLongitude == null ? null : genericLongitude.stripTrailingZeros().toPlainString(),
          genericElevation == null ? null : genericElevation.stripTrailingZeros().toPlainString(),
          genericSpeedOrVelocity == null ? null : genericSpeedOrVelocity.stripTrailingZeros().toPlainString(),
          genericHeading == null ? null : genericHeading.stripTrailingZeros().toPlainString()), null);
    }

    if (rxMsgDetails != null) {
      if (parser instanceof RxMsgFileParser) {
        RxMsgFileParser rxMsgFileParser = (RxMsgFileParser) parser;
        rxMsgDetails.setRxSource(rxMsgFileParser.getRxSource());
      } else {
        rxMsgDetails.setRxSource(RxSource.NA);
      }
    }

    return rxMsgDetails;
  }

  /**
   * This method sequentially delegates the writing operation to several
   * internal parsers, namely `locationParser`, `timeParser`,
   * `secResCodeParser`, and `payloadParser`, each contributing specific
   * portions of data to the output stream.
   *
   * @param os the output stream to which the parsed data will be written.
   * @throws IOException if an I/O error occurs during the writing process.
   */
  public void writeTo(OutputStream os) throws IOException {
    locationParser.writeTo(os);
    timeParser.writeTo(os);
    secResCodeParser.writeTo(os);
    payloadParser.writeTo(os);
  }
}
