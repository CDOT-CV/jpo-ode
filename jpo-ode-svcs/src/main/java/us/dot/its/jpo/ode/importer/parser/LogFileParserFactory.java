package us.dot.its.jpo.ode.importer.parser;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.ode.model.OdeLogMetadata;

@Slf4j
public class LogFileParserFactory {

  public static class LogFileParserFactoryException extends Exception {
    public LogFileParserFactoryException(String message) {
      super(message);
    }
  }

  private LogFileParserFactory() {
    throw new UnsupportedOperationException();
  }

  public static LogFileParser factory(String fileName) throws LogFileParserFactoryException {
    LogFileParser fileParser;
    if (fileName.startsWith(OdeLogMetadata.RecordType.bsmTx.name())) {
      log.debug("Parsing as \"Transmit BSM \" log file type.");
      fileParser = new BsmLogFileParser(OdeLogMetadata.RecordType.bsmTx);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.bsmLogDuringEvent.name())) {
      log.debug("Parsing as \"BSM For Event\" log file type.");
      fileParser = new BsmLogFileParser(OdeLogMetadata.RecordType.bsmLogDuringEvent);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.rxMsg.name())) {
      log.debug("Parsing as \"Received Messages\" log file type.");
      fileParser = new RxMsgFileParser(OdeLogMetadata.RecordType.rxMsg);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.dnMsg.name())) {
      log.debug("Parsing as \"Distress Notifications\" log file type.");
      fileParser = new DistressMsgFileParser(OdeLogMetadata.RecordType.dnMsg);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.driverAlert.name())) {
      log.debug("Parsing as \"Driver Alert\" log file type.");
      fileParser = new DriverAlertFileParser(OdeLogMetadata.RecordType.driverAlert);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.spatTx.name())) {
      log.debug("Parsing as \"Transmit SPAT\" log file type.");
      fileParser = new SpatLogFileParser(OdeLogMetadata.RecordType.spatTx);
    } else {
      throw new LogFileParserFactoryException("Unknown log file prefix: " + fileName);
    }
    return fileParser;
  }
}
