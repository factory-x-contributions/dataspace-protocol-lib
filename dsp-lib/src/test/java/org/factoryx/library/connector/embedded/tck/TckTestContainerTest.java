/*
 * Copyright (c) 2025. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.factoryx.library.connector.embedded.tck;

import org.factoryx.library.connector.embedded.model.JpaNegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.factoryx.library.connector.embedded.provider.service.NegotiationRecordFactory;
import org.factoryx.library.connector.embedded.teststarter.SampleDataAsset;
import org.factoryx.library.connector.embedded.teststarter.SampleDataAssetManagementService;
import org.factoryx.library.connector.embedded.teststarter.TckNegotiationRecordService;
import org.factoryx.library.connector.embedded.teststarter.TestStarter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = TestStarter.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnabledIf(expression = "#{systemProperties['testcontainer.tck.disable'] == 'false'}")
public class TckTestContainerTest {

    final static Logger log = LoggerFactory.getLogger(TckTestContainerTest.class);
    final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    final static Path configFilePath = Paths.get("src/test/resources/tck.config").toAbsolutePath();
    final static Path outputFolderPath = Paths.get("src/test/resources/tck-logs/").toAbsolutePath();

    final static UUID TP_02_AND_03_MOCK_AGREEMENT_ID = UUID.fromString("ca38d696-e5b6-48b0-ac10-66edbb04ff4b");

    @Autowired
    private SampleDataAssetManagementService sampleDataAssetManagementService;

    @Autowired
    private TckNegotiationRecordService negotiationRecordService;

    @BeforeAll
    static void ensureExistingOutputFolder() throws IOException {
        if (!Files.isDirectory(outputFolderPath)) {
            boolean nonDirectoryFileExists = Files.exists(outputFolderPath);
            assertFalse(nonDirectoryFileExists);
            // create directory
            Files.createDirectory(outputFolderPath);
        }
    }

    @Test
    void containerTest() throws Exception {
        assertTrue(waitForProjectBooted());
        sampleDataAssetManagementService.addTckDataAsset(SampleDataAsset.CATALOG_ASSET_ID);
        sampleDataAssetManagementService.addTckDataAsset(SampleDataAsset.NEGOTIATION_ASSET_ID);
        assertNotNull(sampleDataAssetManagementService.getById(UUID.fromString(SampleDataAsset.CATALOG_ASSET_ID)));
        assertNotNull(sampleDataAssetManagementService.getById(UUID.fromString(SampleDataAsset.NEGOTIATION_ASSET_ID)));
        negotiationRecordService.injectMockData(createNegotiationRecord(TP_02_AND_03_MOCK_AGREEMENT_ID));

        NegotiationRecord record = negotiationRecordService.findByContractId(TP_02_AND_03_MOCK_AGREEMENT_ID);
        assertNotNull(record);

        assertThat(Files.exists(configFilePath)).isTrue();

        // Note: Since there is currently no proper release version of the published TCK docker image, you may occasionally
        // want to ensure you have the actual 'latest version' in your local docker repo by manually executing in your shell:
        //
        // docker pull eclipsedataspacetck/dsp-tck-runtime:latest

        try (GenericContainer<?> tckContainer = new GenericContainer<>("eclipsedataspacetck/dsp-tck-runtime:latest")) {
            tckContainer.addFileSystemBind(configFilePath.toString(), "/etc/tck/config.properties", BindMode.READ_ONLY, SelinuxContext.SINGLE);
            tckContainer.withExtraHost("host.docker.internal", "host-gateway");
            tckContainer.setPortBindings(List.of("8083:8083"));
            tckContainer.start();

            assertEquals(8083, tckContainer.getMappedPort(8083));

            List<String> expectedSuccesses = List.of("MET:01-01", "CAT:01-01", "CAT:01-02", "CAT:01-03", "TP:02-01",
                    "TP:02-02", "TP:02-03", "TP:03-03", "TP:03-04", "TP:03-05", "TP:03-06", "CN:01-04", "CN:02-02", "CN:02-03", "CN:03-01");
            List<String> foundSuccesses = new ArrayList<>();

            var latch = new CountDownLatch(1);
            StringBuilder logOutputBuffer = new StringBuilder();
            tckContainer.followOutput(outputFrame -> {
                String line = outputFrame.getUtf8String();
                logOutputBuffer.append(line);
                if (line.contains("SUCCESSFUL:")) {
                    String[] words = line.split(" ");
                    foundSuccesses.add(words[words.length - 1].strip());
                }
                if (line.toLowerCase().contains("test run complete")) {
                    latch.countDown();
                }
            });

            boolean latchResult = latch.await(8, TimeUnit.MINUTES);
            log.info("Dump container logs \n{}", logOutputBuffer);
            String formattedDate = formatter.format(LocalDateTime.now());
            Files.writeString(outputFolderPath.resolve(formattedDate), logOutputBuffer.toString());

            List<String> deltaList = new ArrayList<>(expectedSuccesses);
            deltaList.removeAll(foundSuccesses);
            assertThat(deltaList.isEmpty()).withFailMessage("Missing expected successes " + deltaList).isTrue();
            assertThat(latchResult).isTrue();

        }
    }

    static private boolean waitForProjectBooted() throws Exception {
        int retries = 20;
        int interval = 1000;
        int count = 0;
        boolean success = false;
        do {
            Thread.sleep(interval);
            try {
                count++;
                HttpURLConnection connection = (HttpURLConnection) URI.create("http://localhost:8080/dsp/test").toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                success = connection.getResponseCode() == 200;

                log.info("Attempt {} success? {}", count, success);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } while (count < retries && !success);
        if (success) {
            log.info("Project booted after {} tries", count);
        } else {
            log.info("Project not booted after {} tries", retries);
        }
        return success;

    }

    private JpaNegotiationRecord createNegotiationRecord(UUID contractId) {
        try {
            JpaNegotiationRecord negotiationRecord = new JpaNegotiationRecord();
            negotiationRecord.setConsumerPid(UUID.randomUUID().toString());
            negotiationRecord.setState(NegotiationState.FINALIZED);
            negotiationRecord.setTargetAssetId(SampleDataAsset.CATALOG_ASSET_ID);
            negotiationRecord.setPartnerId("consumer");
            negotiationRecord.setContractId(contractId);
            negotiationRecord.setPartnerDspUrl("http://localhost:8083");
            return negotiationRecord;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @TestConfiguration
    static class TckTestConfig {

        @Bean
        @Primary
        TckNegotiationRecordService negotiationRecordService(NegotiationRecordFactory recordFactory, NegotiationRecordRepository repository) {
            return new TckNegotiationRecordService(recordFactory, repository);
        }
    }

}
