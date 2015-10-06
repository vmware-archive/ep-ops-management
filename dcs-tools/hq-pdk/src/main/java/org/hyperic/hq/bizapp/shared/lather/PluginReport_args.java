package org.hyperic.hq.bizapp.shared.lather;

import java.util.Collection;

import org.hyperic.hq.product.PluginInfo;
import org.hyperic.lather.LatherValue;

public class PluginReport_args extends LatherValue {

    public static final String PLUGIN_NAME = "PLUGIN_NAME";
    public static final String PRODUCT_NAME = "PRODUCT_NAME";
    public static final String MD5 = "MD5";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String AGENT_TOKEN = "agentToken";
    public static final String RESYNC_AGENT_PLUGINS = "resyncAgentPlugins";

    public PluginReport_args() {
        super();
    }

    public PluginReport_args(Collection<PluginInfo> plugins,
                             boolean resyncAgentPlugins) {
        super();
        for (PluginInfo plugin : plugins) {
            addStringToList(PLUGIN_NAME, plugin.name);
            addStringToList(FILE_NAME, plugin.jar);
            addStringToList(MD5, plugin.md5);
            addStringToList(PRODUCT_NAME, plugin.product);
            setStringValue(RESYNC_AGENT_PLUGINS, Boolean.toString(resyncAgentPlugins));
        }
    }

}
