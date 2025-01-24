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
import us.dot.its.jpo.ode.coder.FileAsn1CodecPublisher;
import us.dot.its.jpo.ode.coder.FileAsn1CodecPublisher.FileAsn1CodecPublisherException;
import us.dot.its.jpo.ode.eventlog.EventLogger;
import us.dot.its.jpo.ode.importer.ImporterDirectoryWatcher.ImporterFileType;

@Slf4j
public class ImporterProcessor {

  private final int bufferSize;
  private final FileAsn1CodecPublisher codecPublisher;
  private final ImporterFileType fileType;
  private final Pattern gZipPattern = Pattern.compile("application/.*gzip");
  private final Pattern zipPattern = Pattern.compile("application/.*zip.*");

  public ImporterProcessor(FileAsn1CodecPublisher publisher, ImporterFileType fileType, int bufferSize) {
    this.codecPublisher = publisher;
    this.bufferSize = bufferSize;
    this.fileType = fileType;
  }

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

  public boolean processFile(Path filePath) {
    boolean success = true;
    FileType detectedFileType;
    try {
      detectedFileType = detectFileType(filePath);
      log.info("Treating as {} file", detectedFileType.toString());
    } catch (IOException e) {
      log.error("Failed to detect file type: {}", filePath, e);
      return false;
    }

    try (InputStream inputStream = getInputStream(filePath, detectedFileType)) {
      if (detectedFileType == FileType.ZIP) {
        ZipInputStream zis = (ZipInputStream) inputStream;
        while (zis.getNextEntry() != null) {
          publishFile(filePath, inputStream);
        }
      } else {
        publishFile(filePath, inputStream);
      }
    } catch (Exception e) {
      success = false;
      log.error("Failed to open or process file: {}", filePath, e);
      EventLogger.logger.error("Failed to open or process file: {}", filePath, e);
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

  private FileType detectFileType(Path filePath) throws IOException {
    String probeContentType = Files.probeContentType(filePath);
    if (probeContentType != null) {
      if (gZipPattern.matcher(probeContentType).matches() || filePath.toString().toLowerCase().endsWith("gz")) {
        return FileType.GZIP;
      } else if (zipPattern.matcher(probeContentType).matches() || filePath.toString().endsWith("zip")) {
        return FileType.ZIP;
      }
    }

    return FileType.UNKNOWN;
  }

  private InputStream getInputStream(Path filePath, FileType fileType) throws IOException {
    return switch (fileType) {
      case GZIP -> new GZIPInputStream(new FileInputStream(filePath.toFile()));
      case ZIP -> new ZipInputStream(new FileInputStream(filePath.toFile()));
      default -> new FileInputStream(filePath.toFile());
    };
  }

  private void publishFile(Path filePath, InputStream inputStream) throws FileAsn1CodecPublisherException {
    BufferedInputStream bis= new BufferedInputStream(inputStream, this.bufferSize);
    codecPublisher.publishFile(filePath, bis, fileType);
  }
}
