package octi.wanparty.common;

import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

public enum WANParty {
    ;

    public static final String MOD_ID = "wanparty";
    public static final String MOD_NAME = "WAN Party";

    private static final Logger LOGGER = WPLogger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public static void initClient() {
        logVersion();
    }

    public static void initServer(int port) {
        logVersion();
        LOGGER.info("Starting server on port " + port);
    }

    private static void logVersion() {
        LOGGER.info("WANParty Branch: " + ModJarInfo.Git_Branch);
        LOGGER.info("WANParty Commit: " + ModJarInfo.Git_Commit);
        LOGGER.info("WANParty Jar Build Source: " + ModJarInfo.Build_Source);
    }


}
