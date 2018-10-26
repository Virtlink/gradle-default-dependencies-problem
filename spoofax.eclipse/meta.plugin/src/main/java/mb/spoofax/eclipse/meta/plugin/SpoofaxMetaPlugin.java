package mb.spoofax.eclipse.meta.plugin;

import mb.spoofax.eclipse.plugin.EclipseLogger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class SpoofaxMetaPlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "spoofax.eclipse.meta.plugin"; //$NON-NLS-1$

    private static SpoofaxMetaPlugin plugin;
    private static EclipseLogger logger;


    public static SpoofaxMetaPlugin getPlugin() {
        return plugin;
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
        logger = new EclipseLogger(this.getLog(), PLUGIN_ID);
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }
}
