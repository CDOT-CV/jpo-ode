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
package us.dot.its.jpo.ode.importer;

import mockit.*;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.coder.stream.FileImporterProperties;
import us.dot.its.jpo.ode.kafka.JsonTopics;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.RawEncodedJsonTopics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ImporterDirectoryWatcherTest {

    @Tested
    ImporterDirectoryWatcher testImporterDirectoryWatcher;

    @Injectable
    FileImporterProperties injectableFileImporterProperties;
    @Injectable
    OdeKafkaProperties odeKafkaProperties;
    @Injectable
    JsonTopics jsonTopics;
    @Injectable
    RawEncodedJsonTopics rawEncodedJsonTopics;
    @Injectable
    ImporterDirectoryWatcher.ImporterFileType importerFileType = ImporterDirectoryWatcher.ImporterFileType.LOG_FILE;

    @Capturing
    OdeFileUtils capturingOdeFileUtils;
    @Capturing
    ImporterProcessor capturingImporterProcessor;
    @Capturing
    Executors capturingExecutors;

    @Mocked
    ScheduledExecutorService mockScheduledExecutorService;


    @Test
    void testRun() throws InterruptedException, IOException {
        new Expectations() {
            {
                OdeFileUtils.createDirectoryRecursively((Path) any);
                times = 3;

                Executors.newScheduledThreadPool(1);
                result = mockScheduledExecutorService;

                mockScheduledExecutorService.scheduleWithFixedDelay((Runnable) any, anyLong, anyLong, TimeUnit.SECONDS);

                mockScheduledExecutorService.awaitTermination(anyLong, TimeUnit.SECONDS);
            }
        };
        testImporterDirectoryWatcher = new ImporterDirectoryWatcher(injectableFileImporterProperties,
                odeKafkaProperties,
                jsonTopics,
                ImporterDirectoryWatcher.ImporterFileType.LOG_FILE,
                rawEncodedJsonTopics);

        testImporterDirectoryWatcher.run();
    }

}
