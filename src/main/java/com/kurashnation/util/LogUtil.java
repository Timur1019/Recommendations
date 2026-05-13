package com.kurashnation.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public final class LogUtil {
    private static final Logger LOG = LoggerFactory.getLogger("KurashNation");

    private LogUtil() {
    }

    public static void info(String message, Object... args) {
        if (args.length == 0) {
            LOG.info(message);
        } else {
            LOG.info(String.format(Locale.ROOT, message, args));
        }
    }

    public static void warn(String message, Object... args) {
        if (args.length == 0) {
            LOG.warn(message);
        } else {
            LOG.warn(String.format(Locale.ROOT, message, args));
        }
    }

    public static void error(String message, Object... args) {
        if (args.length == 0) {
            LOG.error(message);
        } else {
            LOG.error(String.format(Locale.ROOT, message, args));
        }
    }

    public static void error(String message, Throwable throwable, Object... args) {
        LOG.error(String.format(Locale.ROOT, message, args), throwable);
    }
}

