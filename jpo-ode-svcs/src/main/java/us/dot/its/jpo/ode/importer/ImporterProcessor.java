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

package us.dot.its.jpo.ode.importer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.ode.coder.stream.LogFileToAsn1CodecPublisher;
import us.dot.its.jpo.ode.importer.parser.LogFileParserFactory;

/**
 * The {@code ImporterProcessor} class is responsible for processing files within directories and subdirectories.
 * It detects the file format, handles compression if necessary, and publishes the contents of the file using
 * a configured {@link LogFileToAsn1CodecPublisher}. The processed files are then moved to the appropriate
 * directory based on the result of the processing.
 *
 * <p>The class can handle gzipped, zipped, and plain file formats. Within the handling of zip files, it is capable
 * of accessing and processing individual entries in the archive. Failed or successfully processed files are moved to
 * predefined failure or backup directories, respectively.
 *
 * <h2>Exceptions:</h2>
 * <ul>
 *   <li>{@link IOException}: Thrown during file I/O operations.</li>
 *   <li>{@link LogFileParserFactory.LogFileParserFactoryException}: Thrown if there is an issue creating a log file parser.</li>
 *   <li>{@link LogFileToAsn1CodecPublisher.LogFileToAsn1CodecPublisherException}: Thrown if file content cannot be published.</li>
 * </ul>
 */
@Slf4j
public class ImporterProcessor {

  private final int bufferSize;
  private final LogFileToAsn1CodecPublisher codecPublisher;
  private final ImporterFileType fileType;
  private static final Pattern gZipPattern = Pattern.compile("application/.*gzip");
  private static final Pattern zipPattern = Pattern.compile("application/.*zip.*");

  /**
   * Constructs an instance of ImporterProcessor for processing and encoding files
   * of a specific type. This class facilitates proper handling of log files and
   * their transformation into {@link us.dot.its.jpo.ode.model.OdeData} objects
   * based on the specified importer file type and buffer size.
   *
   * @param publisher  The publisher responsible for encoding and transferring log files
   *                   to their respective downstream systems or topics.
   * @param fileType   The type of file to be processed by this processor, which determines
   *                   the specific handling and encoding logic.
   * @param bufferSize The size of the buffer to be used during file processing operations,
   *                   impacting memory usage and performance.
   */
  public ImporterProcessor(LogFileToAsn1CodecPublisher publisher, ImporterFileType fileType, int bufferSize) {
    this.codecPublisher = publisher;
    this.bufferSize = bufferSize;
    this.fileType = fileType;
  }

  /**
   * Processes all the files in the given directory. If a subdirectory is encountered,
   * it recursively processes that subdirectory.
   *
   * <p>For each file, it attempts to parse the file into {@link us.dot.its.jpo.ode.model.OdeData} objects,
   * produce the parsed data to the message type's respective topics, and moves the processed file
   * to either the backup directory or the failure directory based on the processing result.</p>
   *
   * @param dir        The directory containing files to be processed. Must not be {@code null}.
   * @param backupDir  The directory where successfully processed files will be moved. Must not be {@code null}.
   * @param failureDir The directory where failed files will be moved. Must not be {@code null}.
   */
  public void processDirectory(Path dir, Path backupDir, Path failureDir) {
    int count = 0;
    // Process files already in the directory
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path entry : stream) {
        if (entry.toFile().isDirectory()) {
          processDirectory(entry, backupDir, failureDir);
        } else {
          log.debug("Found a file to process: {}", entry.getFileName());
          var success = processFile(entry);
          moveProcessedFile(entry, backupDir, failureDir, success);
          count++;
        }
      }
    } catch (IOException e) {
      log.error("Error processing files.", e);
    }
    log.debug("Processed {} files.", count);
  }

  private boolean processFile(Path filePath) {
    boolean success = true;
    FileFormat detectedFileFormat;
    try {
      detectedFileFormat = detectFileType(filePath);
      log.info("Treating as {} file", detectedFileFormat.toString());
    } catch (IOException e) {
      log.error("Failed to detect file type: {}", filePath, e);
      return false;
    }

    try (InputStream inputStream = getInputStream(filePath, detectedFileFormat)) {
      if (detectedFileFormat == FileFormat.ZIP) {
        publishZipFiles(filePath, (ZipInputStream) inputStream);
      } else {
        publishFile(filePath, inputStream);
      }
    } catch (Exception e) {
      success = false;
      log.error("Failed to open or process file: {}", filePath, e);
    }

    return success;
  }

  private void moveProcessedFile(Path filePath, Path backupDir, Path failureDir, boolean success) {
    try {
      if (success) {
        OdeFileUtils.backupFile(filePath, backupDir);
        log.info("File moved to backup: {}", backupDir);
      } else {
        OdeFileUtils.moveFile(filePath, failureDir);
        log.info("File moved to failure directory: {}", failureDir);
      }
    } catch (IOException e) {
      log.error("Unable to backup file: {}", filePath, e);
    }
  }

  private FileFormat detectFileType(Path filePath) throws IOException {
    String probeContentType = Files.probeContentType(filePath);
    if (probeContentType != null) {
      if (gZipPattern.matcher(probeContentType).matches() || filePath.toString().toLowerCase().endsWith("gz")) {
        return FileFormat.GZIP;
      } else if (zipPattern.matcher(probeContentType).matches() || filePath.toString().endsWith("zip")) {
        return FileFormat.ZIP;
      }
    }

    return FileFormat.UNKNOWN;
  }

  private InputStream getInputStream(Path filePath, FileFormat fileFormat) throws IOException {
    return switch (fileFormat) {
      case GZIP -> new GZIPInputStream(new FileInputStream(filePath.toFile()));
      case ZIP -> new ZipInputStream(new FileInputStream(filePath.toFile()));
      default -> new FileInputStream(filePath.toFile());
    };
  }

  private void publishFile(Path filePath, InputStream inputStream)
      throws LogFileParserFactory.LogFileParserFactoryException, LogFileToAsn1CodecPublisher.LogFileToAsn1CodecPublisherException, IOException {
    var fileName = filePath.getFileName().toString();
    var parser = LogFileParserFactory.getLogFileParser(fileName);
    try (BufferedInputStream bis = new BufferedInputStream(inputStream, this.bufferSize)) {
      codecPublisher.publish(bis, fileName, fileType, parser);
    }
  }

  private void publishZipFiles(Path filePath, ZipInputStream inputStream)
      throws IOException, LogFileParserFactory.LogFileParserFactoryException, LogFileToAsn1CodecPublisher.LogFileToAsn1CodecPublisherException {
    while (inputStream.getNextEntry() != null) {
      publishFile(filePath, inputStream);
    }
  }
}
