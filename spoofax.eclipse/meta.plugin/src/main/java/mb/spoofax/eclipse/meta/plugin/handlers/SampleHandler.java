package mb.spoofax.eclipse.meta.plugin.handlers;

import mb.pie.api.PieApi;
import mb.pie.runtime.PieRuntime;
import mb.spoofax.eclipse.meta.plugin.SpoofaxMetaPlugin;
import mb.spoofax.eclipse.plugin.SpoofaxPlugin;
import mb.spoofax.pie.SpoofaxPie;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;

public class SampleHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) {
        final PieApi pieApi = new PieRuntime();
        final SpoofaxPie spoofaxPie = new SpoofaxPie(SpoofaxPlugin.getApi(), pieApi);
        spoofaxPie.doSpoofaxPieStuff(SpoofaxMetaPlugin.getLogger());
        return null;
    }
}
