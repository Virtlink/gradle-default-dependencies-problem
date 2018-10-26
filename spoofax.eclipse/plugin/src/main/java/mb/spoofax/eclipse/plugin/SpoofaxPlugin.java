package mb.spoofax.eclipse.plugin;

import mb.spoofax.api.SpoofaxApi;
import mb.spoofax.runtime.SpoofaxRuntime;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class SpoofaxPlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "spoofax.eclipse.plugin"; //$NON-NLS-1$

    private static SpoofaxPlugin plugin;
    private static SpoofaxApi spoofaxApi;
    private static EclipseLogger logger;


    public static SpoofaxPlugin getPlugin() {
        return plugin;
    }

    public static SpoofaxApi getApi() {
        return spoofaxApi;
    }

    public static EclipseLogger getLogger() {
        return logger;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }


    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        spoofaxApi = new SpoofaxRuntime();
        logger = new EclipseLogger(this.getLog(), PLUGIN_ID);
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }
}
