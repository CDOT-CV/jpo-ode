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
    GZIPOutputStream os =        new GZIPOutputStream(new FileOutputStream(fileForProcessing));
    os.write("test".getBytes());
    os.finish();
    os.close();

    ImporterProcessor importerProcessor = new ImporterProcessor(publisher, ImporterFileType.LOG_FILE, 1024);
    int result = importerProcessor.processDirectory(dirToProcess, backupDir, failureDir);
    assertEquals(1, result);
  }
//
//  @Test
//  void testProcessDirectoryWithNestedDirectories() throws IOException {
//    //
//  }
//
//  @Test
//  void testProcessDirectoryWithFailure()
//      throws IOException, LogFileParserFactory.LogFileParserFactoryException, LogFileToAsn1CodecPublisher.LogFileToAsn1CodecPublisherException {
//    Path mockDir = mock(Path.class);
//    DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
//    Path mockFile = mock(Path.class);
//
//    when(Files.newDirectoryStream(mockDir)).thenReturn(mockStream);
//    when(mockStream.iterator()).thenReturn(List.of(mockFile).iterator());
//    when(mockFile.toFile().isDirectory()).thenReturn(false);
//    //when(OdeFileUtils.moveFile(mockFile, mockFailureDir)).thenReturn(null);
//
//    ImporterProcessor importerProcessor = new ImporterProcessor(codecPublisher, ImporterFileType.LOG_FILE, 1024);
//    doThrow(IOException.class).when(codecPublisher).publish(any(), any(), any(), any());
//
//    int result = importerProcessor.processDirectory(mockDir, mockBackupDir, mockFailureDir);
//
//    assertEquals(0, result);
//    //verify(OdeFileUtils, times(1)).moveFile(mockFile, mockFailureDir);
//  }
//
//  @Test
//  void testProcessNestedDirectories(@Mock DirectoryStream<Path> mockStream) throws IOException {
//    Path mockDir = mock(Path.class);
//    Path mockSubDir = mock(Path.class);
//    Path mockFile = mock(Path.class);
//
//    when(Files.newDirectoryStream(mockDir)).thenReturn(mockStream);
//    when(mockStream.iterator()).thenReturn(List.of(mockSubDir).iterator());
//    when(mockSubDir.toFile().isDirectory()).thenReturn(true);
//    when(Files.newDirectoryStream(mockSubDir)).thenReturn(mockStream);
//    when(mockStream.iterator()).thenReturn(List.of(mockFile).iterator());
//    when(mockFile.toFile().isDirectory()).thenReturn(false);
//
//    ImporterProcessor importerProcessor = new ImporterProcessor(codecPublisher, ImporterFileType.LOG_FILE, 1024);
//    int result = importerProcessor.processDirectory(mockDir, mockBackupDir, mockFailureDir);
//
//    assertEquals(1, result);
//  }
}
