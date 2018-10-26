package mb.spoofax.eclipse.plugin.handlers;

import mb.spoofax.eclipse.plugin.SpoofaxPlugin;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;

public class SampleHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) {
        SpoofaxPlugin.getApi().doSpoofaxStuff(SpoofaxPlugin.getLogger());
        return null;
    }
}
