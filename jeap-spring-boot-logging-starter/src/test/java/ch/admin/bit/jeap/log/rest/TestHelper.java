package ch.admin.bit.jeap.log.rest;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
class TestHelper {
    @SneakyThrows
    void assertEqualsExceptTimeAndUnresolvedHostname(String expectFileName, String logFileName) {
        String actualLogLine = getLastLine(logFileName);
        actualLogLine = replaceTimeAndUnresolvedHostname(actualLogLine);

        URI expectFileUri = ClassLoader.getSystemResource(expectFileName).toURI();
        File expectedFile = new File(expectFileUri);
        String expectedLog = Files.readString(expectedFile.toPath());
        expectedLog = replaceTimeAndUnresolvedHostname(expectedLog);
        expectedLog = expectedLog.replace(System.lineSeparator(), "");

        Assertions.assertEquals(expectedLog, actualLogLine);
    }

    @SneakyThrows
    private String getLastLine(String logFileName) {
        String actualFileName = "target/testoutput/" + logFileName;
        BufferedReader actualBufferedReader = Files.newBufferedReader(Path.of(actualFileName), StandardCharsets.UTF_8);
        String actual = actualBufferedReader.readLine();
        String nextLine = actualBufferedReader.readLine();
        while (nextLine != null) {
            actual = nextLine;
            nextLine = actualBufferedReader.readLine();
        }
        return actual;
    }

    private String replaceTimeAndUnresolvedHostname(String input) {
        return input
                //JSON Format 2020-05-29T12:47:17.653+02:00 or 2022-11-17T17:47:11.576Z (time with an offset or Zulu time)
                //allowing millis without leading zeros and completely missing millis
                .replaceAll("\"....-..-..T..:..:...{0,4}(Z|(\\+..:..))\"", "TIME")
                //dt
                .replaceAll("\"dt\":[0-9]+,", "\"dt\":X,")
                .replaceAll("dt=[0-9]+", "dt=X")
                //Classic Format: 2020-05-29 13:02:56,126
                .replaceAll("^....-..-.. ..:..:..,...", "TIME")
                //Canonicalize "localhost/<unresolved>" (build with java 17+) / vs. "localhost" (java 11)
                .replace("/<unresolved>", "");
    }
}
