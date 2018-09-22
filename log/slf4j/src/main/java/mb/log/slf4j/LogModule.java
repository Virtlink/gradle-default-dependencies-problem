package mb.log.slf4j;

import com.google.inject.AbstractModule;
import mb.log.api.Logger;

public class LogModule extends AbstractModule {
    private final org.slf4j.Logger rootLogger;


    public LogModule(org.slf4j.Logger rootLogger) {
        this.rootLogger = rootLogger;
    }


    @Override protected void configure() {
        bind(Logger.class).toInstance(new SLF4JLogger(rootLogger));
    }
}
