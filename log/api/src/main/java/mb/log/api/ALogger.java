package mb.log.api;

import javax.annotation.Nullable;

/**
 * Abstract base class for {@link Logger} implementations.
 */
public abstract class ALogger implements Logger {
    @Override public void trace(final String msg) {
        trace(msg, (Throwable) null);
    }

    @Override public abstract void trace(final String msg, @Nullable final Throwable cause);

    @Override public void trace(final String format, final Object... args) {
        if(traceEnabled()) {
            final String msg = format(format, args);
            trace(msg);
        }
    }

    @Override public void trace(final String format, final Throwable cause, final Object... args) {
        if(traceEnabled()) {
            final String msg = format(format, args);
            trace(msg, cause);
        }
    }

    @Override public abstract boolean traceEnabled();

    @Override public void debug(final String msg) {
        debug(msg, (Throwable) null);
    }

    @Override public abstract void debug(final String msg, @Nullable final Throwable cause);

    @Override public void debug(final String format, final Object... args) {
        if(debugEnabled()) {
            final String msg = format(format, args);
            debug(msg);
        }
    }

    @Override public void debug(final String format, final Throwable cause, final Object... args) {
        if(debugEnabled()) {
            final String msg = format(format, args);
            debug(msg, cause);
        }
    }

    @Override public abstract boolean debugEnabled();

    @Override public void info(final String msg) {
        info(msg, (Throwable) null);
    }

    @Override public abstract void info(final String msg, @Nullable final Throwable cause);

    @Override public void info(final String format, final Object... args) {
        if(infoEnabled()) {
            final String msg = format(format, args);
            info(msg);
        }
    }

    @Override public void info(final String format, final Throwable cause, final Object... args) {
        if(infoEnabled()) {
            final String msg = format(format, args);
            info(msg, cause);
        }
    }

    @Override public abstract boolean infoEnabled();

    @Override public void warn(final String msg) {
        warn(msg, (Throwable) null);
    }

    @Override public abstract void warn(final String msg, @Nullable final Throwable cause);

    @Override public void warn(final String format, final Object... args) {
        if(warnEnabled()) {
            final String msg = format(format, args);
            warn(msg);
        }
    }

    @Override public void warn(final String format, final Throwable cause, final Object... args) {
        if(warnEnabled()) {
            final String msg = format(format, args);
            warn(msg, cause);
        }
    }

    @Override public abstract boolean warnEnabled();

    @Override public void error(final String msg) {
        error(msg, (Throwable) null);
    }

    @Override public abstract void error(final String msg, @Nullable final Throwable cause);

    @Override public void error(final String format, final Object... args) {
        if(errorEnabled()) {
            final String msg = format(format, args);
            error(msg);
        }
    }

    @Override public void error(final String format, final Throwable cause, final Object... args) {
        if(errorEnabled()) {
            final String msg = format(format, args);
            error(msg, cause);
        }
    }

    @Override public abstract boolean errorEnabled();

    @Override public void log(final Level level, final String msg) {
        log(level, msg, (Throwable) null);
    }

    @Override public void log(final Level level, final String msg, @Nullable final Throwable cause) {
        switch(level) {
            case Trace:
                trace(msg, cause);
                break;
            case Debug:
                debug(msg, cause);
                break;
            case Info:
                info(msg, cause);
                break;
            case Warn:
                warn(msg, cause);
                break;
            case Error:
                error(msg, cause);
                break;
            default:
                throw new IllegalStateException("Level " + level + " is not recognized.");
        }
    }

    @Override public void log(final Level level, final String format, final Object... args) {
        if(enabled(level)) {
            final String msg = format(format, args);
            log(level, msg);
        }
    }

    @Override public void log(final Level level, final String format, final Throwable cause, final Object... args) {
        if(enabled(level)) {
            final String msg = format(format, args);
            log(level, msg, cause);
        }
    }

    @Override public boolean enabled(final Level level) {
        switch(level) {
            case Trace:
                return traceEnabled();
            case Debug:
                return debugEnabled();
            case Info:
                return infoEnabled();
            case Warn:
                return warnEnabled();
            case Error:
                return errorEnabled();
            default:
                throw new IllegalStateException("Level " + level + " is not recognized.");
        }
    }


    protected abstract String format(final String msg, final Object... args);
}
