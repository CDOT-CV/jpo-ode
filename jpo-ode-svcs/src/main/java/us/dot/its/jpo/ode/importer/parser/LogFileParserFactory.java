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

  public static LogFileParser getLogFileParser(String fileName) throws LogFileParserFactoryException {
    LogFileParser fileParser;
    if (fileName.startsWith(OdeLogMetadata.RecordType.bsmTx.name())) {
      fileParser = new BsmLogFileParser(OdeLogMetadata.RecordType.bsmTx);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.bsmLogDuringEvent.name())) {
      fileParser = new BsmLogFileParser(OdeLogMetadata.RecordType.bsmLogDuringEvent);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.rxMsg.name())) {
      fileParser = new RxMsgFileParser(OdeLogMetadata.RecordType.rxMsg);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.dnMsg.name())) {
      fileParser = new DistressMsgFileParser(OdeLogMetadata.RecordType.dnMsg);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.driverAlert.name())) {
      fileParser = new DriverAlertFileParser(OdeLogMetadata.RecordType.driverAlert);
    } else if (fileName.startsWith(OdeLogMetadata.RecordType.spatTx.name())) {
      fileParser = new SpatLogFileParser(OdeLogMetadata.RecordType.spatTx);
    } else {
      throw new LogFileParserFactoryException("Unknown log file prefix: " + fileName);
    }
    log.debug("Returning parser for file type: {}", fileParser.getRecordType());
    return fileParser;
  }
}
