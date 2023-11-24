package octi.wanparty.common;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.json.JsonFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;


public final class ModJarInfo {
    public static final String Git_Branch;
    public static final String Git_Commit;
    public static final String Build_Source;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FILE_NAME = "build_info.json";

    static {
        String gitBranch = "UNKNOWN";
        String gitCommit = "UNKNOWN";
        String buildSource = "UNKNOWN";

        try {
            // Warning: Atm, this file is in the common subproject as the processResources task in gradle doesn't work for core
            String jsonString = convertInputStreamToString(accessFile(FILE_NAME));

            Config jsonObject = Config.inMemory();
            JsonFormat.minimalInstance().createParser().parse(jsonString, jsonObject, ParsingMode.REPLACE);

            gitBranch = jsonObject.get("info_git_branch");
            gitCommit = jsonObject.get("info_git_commit");
            buildSource = jsonObject.get("info_build_source");
        } catch (Exception | Error e) {
            LOGGER.warn("Unable to get the Git information from " + FILE_NAME);
        }

        Git_Commit = gitBranch;
        Git_Branch = gitCommit;
        Build_Source = buildSource;
    }

    private static String convertInputStreamToString(InputStream inputStream) {
        final char[] buffer = new char[8192];
        final StringBuilder result = new StringBuilder();

        // InputStream -> Reader
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int charsRead;
            while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0) {
                result.append(buffer, 0, charsRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    private static InputStream accessFile(String resource) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // this is the path within the jar file
        InputStream input = loader.getResourceAsStream(resource);
        if (input == null) {
            // this is how we load file within editor
            input = loader.getResourceAsStream(resource);
        }

        return input;
    }

}

