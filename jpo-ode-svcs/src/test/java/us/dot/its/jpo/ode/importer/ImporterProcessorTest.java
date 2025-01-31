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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType.bsmTx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import us.dot.its.jpo.ode.coder.stream.LogFileToAsn1CodecPublisher;

@ExtendWith(MockitoExtension.class)
class ImporterProcessorTest {

  Path dirToProcess;
  Path backupDir;
  Path failureDir;

  @BeforeEach
  void setup() throws IOException {
    dirToProcess = new File(System.getProperty("java.io.tmpdir") + "/filesToProcess").toPath();
    backupDir = new File(System.getProperty("java.io.tmpdir") + "/backup").toPath();
    failureDir = new File(System.getProperty("java.io.tmpdir") + "/failure").toPath();
    OdeFileUtils.createDirectoryRecursively(dirToProcess);
    OdeFileUtils.createDirectoryRecursively(backupDir);
    OdeFileUtils.createDirectoryRecursively(failureDir);
  }

  @Test
  void testProcessDirectoryGZIP(@Mock LogFileToAsn1CodecPublisher publisher) throws IOException {
    // Write some dummy data to the test gzip file so the input stream can be opened
    var fileForProcessing = new File("%s/%s.gz".formatted(dirToProcess, bsmTx.name()));
    assertTrue(fileForProcessing.createNewFile());
    fileForProcessing.deleteOnExit();
    GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(fileForProcessing));
    os.write("test".getBytes());
    os.finish();
    os.close();

    ImporterProcessor importerProcessor = new ImporterProcessor(publisher, ImporterFileType.LOG_FILE, 1024);
    int result = importerProcessor.processDirectory(dirToProcess, backupDir, failureDir);
    assertEquals(1, result);
  }

  @Test
  void testProcessDirectoryPlainText(@Mock LogFileToAsn1CodecPublisher publisher) throws IOException {
    var fileForProcessing = new File("%s/%s.txt".formatted(dirToProcess, bsmTx.name()));
    assertTrue(fileForProcessing.createNewFile());
    fileForProcessing.deleteOnExit();
    var os = new FileOutputStream(fileForProcessing);
    os.write("test".getBytes());
    os.flush();
    os.close();

    ImporterProcessor importerProcessor = new ImporterProcessor(publisher, ImporterFileType.LOG_FILE, 1024);
    int result = importerProcessor.processDirectory(dirToProcess, backupDir, failureDir);
    assertEquals(1, result);
  }

  @Test
  void testProcessDirectoryZip(@Mock LogFileToAsn1CodecPublisher publisher) throws IOException {
    var firstFileForProcessing = new File("%s/first-%s.uper".formatted(dirToProcess, bsmTx.name()));
    assertTrue(firstFileForProcessing.createNewFile());
    firstFileForProcessing.deleteOnExit();
    var os = new FileOutputStream(firstFileForProcessing);
    os.write("test".getBytes());
    os.flush();
    os.close();

    var secondFileForProcessing = new File("%s/second-%s.uper".formatted(dirToProcess, bsmTx.name()));
    assertTrue(secondFileForProcessing.createNewFile());
    secondFileForProcessing.deleteOnExit();
    os = new FileOutputStream(secondFileForProcessing);
    os.write("test2".getBytes());
    os.flush();
    os.close();

    var zipFileForProcessing = new File("%s/%s.zip".formatted(dirToProcess, bsmTx.name()));
    assertTrue(zipFileForProcessing.createNewFile());
    zipFileForProcessing.deleteOnExit();
    var zipOutputStream = new java.util.zip.ZipOutputStream(new FileOutputStream(zipFileForProcessing));
    zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("first.uper"));
    zipOutputStream.write("test".getBytes());
    zipOutputStream.closeEntry();
    zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("second.uper"));
    zipOutputStream.write("test2".getBytes());
    zipOutputStream.closeEntry();
    zipOutputStream.finish();
    zipOutputStream.close();

    ImporterProcessor importerProcessor = new ImporterProcessor(publisher, ImporterFileType.LOG_FILE, 1024);
    int result = importerProcessor.processDirectory(dirToProcess, backupDir, failureDir);
    assertEquals(1, result);
  }

  @Test
  void testProcessDirectoryWithNestedDirectories(@Mock LogFileToAsn1CodecPublisher publisher) throws IOException {
    var nestedDirectory = new File(dirToProcess + "/nestedDirectory");
    nestedDirectory.mkdirs();
    var fileForProcessing = new File("%s/%s.gz".formatted(nestedDirectory, bsmTx.name()));
    assertTrue(fileForProcessing.createNewFile());
    fileForProcessing.deleteOnExit();
    GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(fileForProcessing));
    os.write("test".getBytes());
    os.finish();
    os.close();

    ImporterProcessor importerProcessor = new ImporterProcessor(publisher, ImporterFileType.LOG_FILE, 1024);
    int result = importerProcessor.processDirectory(dirToProcess, backupDir, failureDir);
    assertEquals(1, result);
  }

  @Test
  void testProcessDirectoryWithEmptyDirectory() {
    var emptyDir = new File(dirToProcess + "/empty");
    emptyDir.mkdirs();
    ImporterProcessor importerProcessor = new ImporterProcessor(null, ImporterFileType.LOG_FILE, 1024);

    int result = importerProcessor.processDirectory(emptyDir.toPath(), backupDir, failureDir);

    assertEquals(0, result);
  }
}
