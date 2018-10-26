package mb.spoofax.pie;

import mb.log.api.Logger;
import mb.pie.api.PieApi;
import mb.spoofax.api.SpoofaxApi;

public class SpoofaxPie {
    private final SpoofaxApi spoofaxApi;
    private final PieApi pieApi;


    public SpoofaxPie(SpoofaxApi spoofaxApi, PieApi pieApi) {
        this.spoofaxApi = spoofaxApi;
        this.pieApi = pieApi;
    }


    public void doSpoofaxPieStuff(Logger logger) {
        logger.warn("Spoofax-PIE is combining Spoofax and PIE!");
        spoofaxApi.doSpoofaxStuff(logger);
        pieApi.doPieStuff(logger);
    }
}
