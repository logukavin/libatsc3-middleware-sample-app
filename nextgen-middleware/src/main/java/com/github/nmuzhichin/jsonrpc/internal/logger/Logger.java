package com.github.nmuzhichin.jsonrpc.internal.logger;

import org.slf4j.LoggerFactory;

/**
 * Slf4j Delegate
 */
public final class Logger {
    private final org.slf4j.Logger logger;

    private Logger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public static Logger of(final Class<?> clazz) {
        return new Logger(LoggerFactory.getLogger(clazz));
    }

    public static Logger of(final org.slf4j.Logger logger) {
        return new Logger(logger);
    }

    public void trace(final String msg) {
        if (logger.isTraceEnabled()) {
            logger.trace(msg);
        }
    }

    public void trace(final String format, final Object arg) {
        if (logger.isTraceEnabled()) {
            logger.trace(format, arg);
        }
    }

    public void trace(final String format, final Object arg1, final Object arg2) {
        if (logger.isTraceEnabled()) {
            logger.trace(format, arg1, arg2);
        }
    }

    public void trace(final String format, final Object... arguments) {
        if (logger.isTraceEnabled()) {
            logger.trace(format, arguments);
        }
    }

    public void trace(final String msg, final Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(msg, t);
        }
    }

    public void debug(final String msg) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg);
        }
    }

    public void debug(final String format, final Object arg) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, arg);
        }
    }

    public void debug(final String format, final Object arg1, final Object arg2) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, arg1, arg2);
        }
    }

    public void debug(final String format, final Object... arguments) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, arguments);
        }
    }

    public void debug(final String msg, final Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg, t);
        }
    }

    public void info(final String msg) {
        if (logger.isInfoEnabled()) {
            logger.info(msg);
        }
    }

    public void info(final String format, final Object arg) {
        if (logger.isInfoEnabled()) {
            logger.info(format, arg);
        }
    }

    public void info(final String format, final Object arg1, final Object arg2) {
        if (logger.isInfoEnabled()) {
            logger.info(format, arg1, arg2);
        }
    }

    public void info(final String format, final Object... arguments) {
        if (logger.isInfoEnabled()) {
            logger.info(format, arguments);
        }
    }

    public void info(final String msg, final Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(msg, t);
        }
    }

    public void warn(final String msg) {
        if (logger.isWarnEnabled()) {
            logger.warn(msg);
        }
    }

    public void warn(final String format, final Object arg) {
        if (logger.isWarnEnabled()) {
            logger.warn(format, arg);
        }
    }

    public void warn(final String format, final Object... arguments) {
        if (logger.isWarnEnabled()) {
            logger.warn(format, arguments);
        }
    }

    public void warn(final String format, final Object arg1, final Object arg2) {
        if (logger.isWarnEnabled()) {
            logger.warn(format, arg1, arg2);
        }
    }

    public void warn(final String msg, final Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn(msg, t);
        }
    }

    public void error(final String msg) {
        if (logger.isErrorEnabled()) {
            logger.error(msg);
        }
    }

    public void error(final String format, final Object arg) {
        if (logger.isErrorEnabled()) {
            logger.error(format, arg);
        }
    }

    public void error(final String format, final Object arg1, final Object arg2) {
        if (logger.isErrorEnabled()) {
            logger.error(format, arg1, arg2);
        }
    }

    public void error(final String format, final Object... arguments) {
        if (logger.isErrorEnabled()) {
            logger.error(format, arguments);
        }
    }

    public void error(final String msg, final Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.error(msg, t);
        }
    }
}
