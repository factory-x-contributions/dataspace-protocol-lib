package org.factoryx.library.connector.embedded.tck;

import org.factoryx.library.connector.embedded.teststarter.SampleDataAssetManagementService;
import org.factoryx.library.connector.embedded.teststarter.TestStarter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@EnabledIf(expression = "#{environment.getProperty('testcontainer.tck.disable') != 'true'}",
        reason = "TCK Testcontainer Tests disabled via application.properties setting")
public class TckTestContainerTest {
    final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    final static Path configFilePath = Paths.get("src/test/resources/tck.config").toAbsolutePath();
    final static Path outputFolderPath = Paths.get("src/test/resources/tck-logs/").toAbsolutePath();

    @Autowired
    private SampleDataAssetManagementService sampleDataAssetManagementService;

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
        sampleDataAssetManagementService.addTckDataAsset();
        assertNotNull(sampleDataAssetManagementService.getById(UUID.fromString("207ed5a4-2eae-47af-bcb1-9202280d2700")));
        assertTrue(waitForProjectBooted());
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

            List<String> expectedSuccesses = List.of("CAT:01-01");
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

            assertThat(latch.await(8, TimeUnit.MINUTES)).isTrue();
            assertThat(foundSuccesses.containsAll(expectedSuccesses)).isTrue();

            String formattedDate = formatter.format(LocalDateTime.now());
            Files.writeString(outputFolderPath.resolve(formattedDate), logOutputBuffer.toString());

        }
    }

    static private boolean waitForProjectBooted() throws Exception {
        System.out.println("Waiting for project booted...");
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

                System.out.println("Attempt " + count + " success? " + success);
            } catch (Exception e) {
                System.out.println("Exception: " + e);
            }
        } while (count < retries && !success);
        if (success) {
            System.out.println("Project booted after " + count + " tries");
        } else {
            System.out.println("Project not booted after " + retries + " tries");
        }
        return success;

    }
}
