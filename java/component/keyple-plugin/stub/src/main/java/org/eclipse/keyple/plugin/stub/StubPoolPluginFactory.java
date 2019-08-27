package org.eclipse.keyple.plugin.stub;

import org.eclipse.keyple.core.seproxy.PluginFactory;
import org.eclipse.keyple.core.seproxy.ReaderPlugin;

public class StubPoolPluginFactory extends PluginFactory {

    @Override
    protected String getPluginName() {
        return StubPlugin.PLUGIN_NAME;
    }

    @Override
    protected ReaderPlugin getPluginInstance() {
        return new StubPoolPlugin(new StubPlugin());
    }
}
