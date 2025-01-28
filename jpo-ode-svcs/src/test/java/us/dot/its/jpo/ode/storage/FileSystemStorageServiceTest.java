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
package us.dot.its.jpo.ode.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import us.dot.its.jpo.ode.coder.stream.FileImporterProperties;
import us.dot.its.jpo.ode.eventlog.EventLogger;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = FileImporterProperties.class)
@EnableConfigurationProperties
public class FileSystemStorageServiceTest {

    @Autowired
    private FileImporterProperties fileImporterProperties;

    @Test @Disabled
    public void storeShouldThrowExceptionUnknownType(@Mocked MultipartFile mockMultipartFile) {

        try {
            new FileSystemStorageService(fileImporterProperties).store(mockMultipartFile, LogFileType.UNKNOWN);
            fail("Expected StorageException");
        } catch (Exception e) {
            assertEquals("Incorrect exception thrown", StorageException.class, e.getClass());
            assertTrue("Incorrect message received", e.getMessage().startsWith("File type unknown:"));
        }

        new Verifications() {
            {
                EventLogger.logger.info(anyString, any, any);
            }
        };

    }

    @Test @Disabled
    public void storeShouldTryToResolveBsmFilename(@Mocked MultipartFile mockMultipartFile) {
        new Expectations() {
            {
                mockMultipartFile.getOriginalFilename();
                result = anyString;
                mockMultipartFile.isEmpty();
                result = true;
            }
        };

        try {
            new FileSystemStorageService(fileImporterProperties).store(mockMultipartFile, LogFileType.OBU);
            fail("Expected StorageException");
        } catch (Exception e) {
            assertEquals("Incorrect exception thrown", StorageException.class, e.getClass());
            assertTrue("Incorrect message received", e.getMessage().startsWith("File is empty:"));
        }

        new Verifications() {
            {
                EventLogger.logger.info(anyString);
            }
        };
    }

    @Test @Disabled
    public void storeShouldThrowAnErrorEmptyFile(@Mocked MultipartFile mockMultipartFile) {
        new Expectations() {
            {
                mockMultipartFile.getOriginalFilename();
                result = anyString;
                mockMultipartFile.isEmpty();
                result = true;
            }
        };

        try {
            new FileSystemStorageService(fileImporterProperties).store(mockMultipartFile, LogFileType.OBU);
            fail("Expected StorageException");
        } catch (Exception e) {
            assertEquals("Incorrect exception thrown", StorageException.class, e.getClass());
            assertTrue("Incorrect message received", e.getMessage().startsWith("File is empty:"));
        }

        new Verifications() {
            {
                EventLogger.logger.info(anyString);
            }
        };
    }

    @Test @Disabled
    public void storeShouldRethrowDeleteException(@Mocked MultipartFile mockMultipartFile, @Mocked Files unused) {
        new Expectations() {
            {
                mockMultipartFile.getOriginalFilename();
                result = anyString;
                mockMultipartFile.isEmpty();
                result = false;
            }
        };

        try {
            new Expectations() {
                {
                    Files.deleteIfExists((Path) any);
                    result = new IOException("testException123");
                }
            };
        } catch (IOException e1) {
            fail("Unexpected exception on Files.deleteIfExists() expectation creation");
        }

        try {
            new FileSystemStorageService(fileImporterProperties).store(mockMultipartFile, LogFileType.OBU);
            fail("Expected StorageException");
        } catch (Exception e) {
            assertEquals("Incorrect exception thrown", StorageException.class, e.getClass());
            assertTrue("Incorrect message received", e.getMessage().startsWith("Failed to delete existing file:"));
        }

        new Verifications() {
            {
                EventLogger.logger.info("Deleting existing file: {}", any);
                EventLogger.logger.info("Failed to delete existing file: {} ", any);
            }
        };
    }

    @Test @Disabled
    public void storeShouldRethrowCopyException(@Mocked MultipartFile mockMultipartFile, @Mocked Files unusedFiles,
            @Mocked final Logger mockLogger, @Mocked LoggerFactory unusedLogger, @Mocked InputStream mockInputStream) {
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
            assertEquals("Incorrect exception thrown", StorageException.class, e.getClass());
            assertTrue("Incorrect message received",
                    e.getMessage().startsWith("Failed to store file in shared directory"));
        }

        new Verifications() {
            {
                EventLogger.logger.info("Copying file {} to {}", anyString, (Path) any);
                EventLogger.logger.info("Failed to store file in shared directory {}", (Path) any);
            }
        };
    }

    @Test @Disabled
    public void deleteAllShouldDeleteRecursivelyAndLog(@Mocked final FileSystemUtils unused) {

        new FileSystemStorageService(fileImporterProperties).deleteAll();

        new Verifications() {
            {
                FileSystemUtils.deleteRecursively((File) any);
                EventLogger.logger.info("Deleting {}", (Path) any);
            }
        };

    }
}
