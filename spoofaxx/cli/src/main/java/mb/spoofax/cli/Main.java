package mb.spoofax.cli;

import mb.log.slf4j.SLF4JLogger;
import mb.pie.api.PieApi;
import mb.pie.runtime.PieRuntime;
import mb.spoofax.api.SpoofaxApi;
import mb.spoofax.pie.SpoofaxPie;
import mb.spoofax.runtime.SpoofaxRuntime;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) {
        final SpoofaxApi spoofaxApi = new SpoofaxRuntime();
        final PieApi pieApi = new PieRuntime();
        final SpoofaxPie spoofaxPie = new SpoofaxPie(spoofaxApi, pieApi);
        final SLF4JLogger slf4JLogger = new SLF4JLogger(LoggerFactory.getLogger("root"));
        spoofaxPie.doSpoofaxPieStuff(slf4JLogger);
    }
}
