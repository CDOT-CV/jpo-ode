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

package us.dot.its.jpo.ode.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import mockit.Expectations;
import mockit.Verifications;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import us.dot.its.jpo.ode.coder.stream.FileImporterProperties;
import us.dot.its.jpo.ode.eventlog.EventLogger;

@ExtendWith(value = {SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = FileImporterProperties.class)
@EnableConfigurationProperties
class FileSystemStorageServiceTest {

  @Autowired
  private FileImporterProperties fileImporterProperties;

  @Test
  void storeShouldThrowExceptionUnknownType(@Mock MultipartFile mockMultipartFile) {
    var storageService = new FileSystemStorageService(fileImporterProperties);
    var storageException = assertThrows(StorageException.class, () -> storageService.store(mockMultipartFile, LogFileType.UNKNOWN));
    assertTrue(storageException.getMessage().startsWith("File type unknown:"), "Incorrect message received");

  }

  @Test
  void storeShouldThrowExceptionOnEmptyFile(@Mock MultipartFile mockMultipartFile) {

    Mockito.doReturn("filename").when(mockMultipartFile).getOriginalFilename();
    Mockito.doReturn(true).when(mockMultipartFile).isEmpty();
    var storageService = new FileSystemStorageService(fileImporterProperties);
    var storageException = assertThrows(StorageException.class, () -> storageService.store(mockMultipartFile, LogFileType.OBU));
    assertTrue(storageException.getMessage().startsWith("File is empty:"), "Incorrect message received");
  }

  @Test
  void storeShouldRethrowDeleteException(@Mock MultipartFile mockMultipartFile) {
    Mockito.doReturn("filename").when(mockMultipartFile).getOriginalFilename();
    Mockito.doReturn(false).when(mockMultipartFile).isEmpty();

    var storageService = new FileSystemStorageService(fileImporterProperties);
    var storageException = assertThrows(StorageException.class, () -> storageService.store(mockMultipartFile, LogFileType.OBU));
    assertTrue(storageException.getMessage().startsWith("Failed to store file in shared directory"), "Incorrect message received");
  }

  @Test
  @Disabled
  void storeShouldRethrowCopyException(@Mock MultipartFile mockMultipartFile, @Mock Files unusedFiles,
                                       @Mock final Logger mockLogger, @Mock LoggerFactory unusedLogger, @Mock InputStream mockInputStream) {
    try {
      new Expectations() {
        {
          mockMultipartFile.getOriginalFilename();
          result = anyString;

          mockMultipartFile.isEmpty();
          result = false;

          mockMultipartFile.getInputStream();
          result = mockInputStream;

          Files.deleteIfExists((Path) any);

          Files.copy((InputStream) any, (Path) any);
          result = new IOException("testException123");
        }
      };
    } catch (IOException e1) {
      fail("Unexpected exception creating test Expectations: " + e1);
    }

    try {
      new FileSystemStorageService(fileImporterProperties).store(mockMultipartFile, LogFileType.OBU);
      fail("Expected StorageException");
    } catch (Exception e) {
      assertEquals(StorageException.class, e.getClass(), "Incorrect exception thrown");
      assertTrue(e.getMessage().startsWith("Failed to store file in shared directory"),
          "Incorrect message received");
    }

    new Verifications() {
      {
        EventLogger.logger.info("Copying file {} to {}", anyString, (Path) any);
        EventLogger.logger.info("Failed to store file in shared directory {}", (Path) any);
      }
    };
  }
}
