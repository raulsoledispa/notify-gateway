package com.nova.infrastructure.logging;

import org.slf4j.Logger;
import java.util.regex.Pattern;

public class PrivacyMaskingLogger {
    private final Logger logger;
    
    private static final Pattern EMAIL_REGEX = Pattern.compile("([^@]{1,3})([^@]+)@(.+)");
    private static final Pattern PHONE_REGEX = Pattern.compile("(\\+\\d{1,3})\\d{4,8}(\\d{2})");
    private static final Pattern TOKEN_REGEX = Pattern.compile("([A-Za-z0-9_\\-]{4})[A-Za-z0-9_\\-]{15,}([A-Za-z0-9_\\-]{4})");

    public PrivacyMaskingLogger(Logger logger) {
        this.logger = logger;
    }

    public void info(String format, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(mask(format), applyMaskToArgs(args));
        }
    }

    public void error(String format, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(mask(format), applyMaskToArgs(args));
        }
    }

    private String mask(String input) {
        if (input == null) return null;
        String temp = EMAIL_REGEX.matcher(input).replaceAll("$1***@$3");
        temp = PHONE_REGEX.matcher(temp).replaceAll("$1******$2");
        temp = TOKEN_REGEX.matcher(temp).replaceAll("$1********$2");
        return temp;
    }

    private Object[] applyMaskToArgs(Object[] args) {
        Object[] maskedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String s) {
                maskedArgs[i] = mask(s);
            } else {
                maskedArgs[i] = args[i];
            }
        }
        return maskedArgs;
    }
}
