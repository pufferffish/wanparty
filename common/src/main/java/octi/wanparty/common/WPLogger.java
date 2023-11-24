package octi.wanparty.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static octi.wanparty.common.WANParty.*;

public enum WPLogger {
    ;

    public static Logger getLogger(String className) {
        return LogManager.getLogger(MOD_NAME + "-" + className);
    }

    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(MOD_NAME + "-" + clazz.getSimpleName());
    }

    public static Logger getLogger() {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        String callerClassName = "??";
        for (int i = 1; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (!ste.getClassName().equals(WANParty.class.getName())
                    && ste.getClassName().indexOf("java.lang.Thread") != 0) {
                callerClassName = ste.getClassName();
                break;
            }
        }
        return LogManager.getLogger(MOD_NAME + "-" + callerClassName);
    }

}
