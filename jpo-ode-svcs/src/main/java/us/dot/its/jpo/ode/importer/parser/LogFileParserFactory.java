package us.dot.its.jpo.ode.importer.parser;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.ode.model.OdeLogMetadata;

/**
 * Factory class for creating instances of specific log file parsers based on the
 * file name prefix. This factory helps in determining and returning the appropriate
 * parser implementation for processing different types of log files.
 */
@Slf4j
public class LogFileParserFactory {

  /**
   * Exception class representing errors that occur during the creation of log file parsers
   * in the {@code LogFileParserFactory} class. This exception is thrown when a log file with
   * an unrecognized or unsupported prefix is encountered.
   */
  public static class LogFileParserFactoryException extends Exception {
    public LogFileParserFactoryException(String message) {
      super(message);
    }
  }

  private LogFileParserFactory() {
    throw new UnsupportedOperationException();
  }

  /**
   * Retrieves an appropriate instance of a log file parser based on the given file name prefix.
   * Determines the file type by matching the prefix of the file name with predefined record types and
   * creates an instance of the corresponding parser for that type.
   *
   * @param fileName the name of the log file, including its prefix, which will be used to
   *                 identify the appropriate parser type
   *
   * @return an instance of a subclass of {@link LogFileParser} that matches the file type
   *
   * @throws LogFileParserFactoryException if the file name prefix does not match any supported
   *                                       record type
   */
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
