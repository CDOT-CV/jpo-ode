/*==============================================================================
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
import us.dot.its.jpo.ode.model.OdeLogMetadata;

/**
 * This class is responsible for parsing driver alert log files by extending the
 * functionality of the {@link LogFileParser}. It operates in multiple steps where
 * it processes location, time, and payload data from the given file.
 */
public class DriverAlertFileParser extends LogFileParser {

  private String alert;

  /**
   * Constructs a new DriverAlertFileParser instance to parse driver alert log files.
   * This parser processes location, time, and payload data from files, using the
   * specified record type to determine the parsing behavior.
   *
   * @param recordType the type of records expected in the log file. It is used to
   *                   specify the type of driver alert data being parsed.
   */
  public DriverAlertFileParser(OdeLogMetadata.RecordType recordType) {
    super();
    setLocationParser(new LocationParser());
    setTimeParser(new TimeParser());
    setPayloadParser(new PayloadParser());
    setRecordType(recordType);
  }

  @Override
  public ParserStatus parseFile(BufferedInputStream bis, String fileName) throws FileParserException {

    ParserStatus status;
    try {
      status = super.parseFile(bis, fileName);
      if (status != ParserStatus.COMPLETE) {
        return status;
      }

      if (getStep() == 1) {
        status = nextStep(bis, fileName, locationParser);
        if (status != ParserStatus.COMPLETE) {
          return status;
        }
      }

      if (getStep() == 2) {
        status = nextStep(bis, fileName, timeParser);
        if (status != ParserStatus.COMPLETE) {
          return status;
        }
      }

      if (getStep() == 3) {
        status = nextStep(bis, fileName, payloadParser);
        if (status != ParserStatus.COMPLETE) {
          return status;
        }
        setAlert(payloadParser.getPayload());
      }

      resetStep();
      status = ParserStatus.COMPLETE;

    } catch (Exception e) {
      throw new FileParserException(String.format("Error parsing %s on step %d", fileName, getStep()), e);
    }

    return status;
  }

  protected String getAlert() {
    return alert;
  }

  protected void setAlert(byte[] alert) {
    this.alert = new String(alert);
  }

  @Override
  public void writeTo(OutputStream os) throws IOException {
    locationParser.writeTo(os);
    timeParser.writeTo(os);
    payloadParser.writeTo(os);
  }
}
