package mb.spoofax.eclipse.plugin;

import mb.log.api.ALogger;
import mb.log.api.Logger;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.slf4j.helpers.MessageFormatter;

public class EclipseLogger extends ALogger {
    private final ILog log;
    private final String pluginId;

    public EclipseLogger(ILog log, String pluginId) {
        this.log = log;
        this.pluginId = pluginId;
    }


    @Override
    public void trace(String msg, Throwable cause) {
        log.log(new Status(IStatus.INFO, pluginId, msg, cause));
    }

    @Override
    public boolean traceEnabled() {
        return true;
    }


    @Override
    public void debug(String msg, Throwable cause) {
        log.log(new Status(IStatus.INFO, pluginId, msg, cause));
    }

    @Override
    public boolean debugEnabled() {
        return true;
    }


    @Override
    public void info(String msg, Throwable cause) {
        log.log(new Status(IStatus.INFO, pluginId, msg, cause));
    }

    @Override
    public boolean infoEnabled() {
        return true;
    }


    @Override
    public void warn(String msg, Throwable cause) {
        log.log(new Status(IStatus.WARNING, pluginId, msg, cause));
    }

    @Override
    public boolean warnEnabled() {
        return true;
    }


    @Override
    public void error(String msg, Throwable cause) {
        log.log(new Status(IStatus.ERROR, pluginId, msg, cause));
    }

    @Override
    public boolean errorEnabled() {
        return true;
    }


    @Override
    public Logger forContext(Class<?> clazz) {
        return this;
    }

    @Override
    public Logger forContext(String name) {
        return this;
    }


    @Override
    protected String format(String msg, Object... args) {
        return MessageFormatter.arrayFormat(msg, args).getMessage();
    }
}
