package mb.spoofax.runtime;

import mb.log.api.Logger;
import mb.spoofax.api.SpoofaxApi;

public class SpoofaxRuntime implements SpoofaxApi {
    @Override
    public void doSpoofaxStuff(Logger logger) {
        logger.info("The Spoofax runtime does some stuff!");
    }
}
