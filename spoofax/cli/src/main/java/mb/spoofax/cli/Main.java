package mb.spoofax.cli;

import mb.log.slf4j.SLF4JLogger;
import mb.spoofax.api.SpoofaxApi;
import mb.spoofax.runtime.SpoofaxRuntime;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) {
        final SLF4JLogger slf4JLogger = new SLF4JLogger(LoggerFactory.getLogger("root"));
        final SpoofaxApi spoofaxApi = new SpoofaxRuntime();
        spoofaxApi.doSpoofaxStuff(slf4JLogger);
    }
}
