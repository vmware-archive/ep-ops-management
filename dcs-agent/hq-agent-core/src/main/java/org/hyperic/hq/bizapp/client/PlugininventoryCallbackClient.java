package org.hyperic.hq.bizapp.client;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.PluginReport_args;
import org.hyperic.hq.product.PluginInfo;

public class PlugininventoryCallbackClient extends AgentCallbackClient {

    private final static Log logger = LogFactory.getLog(PlugininventoryCallbackClient.class);
    private final PluginReport_args args;

    public PlugininventoryCallbackClient(ProviderFetcher fetcher,
                                         Collection<PluginInfo> plugins,
                                         boolean resyncAgentPlugins,
                                         AgentConfig bootConfig) {
        super(fetcher, bootConfig);
        args = new PluginReport_args(plugins, resyncAgentPlugins);
        logger.debug("Sending plugin status to server: \n" + Arrays.toString(plugins.toArray()));
    }

    public void sendPluginReportToServer()
        throws AgentCallbackClientException {
        ProviderInfo provider = getProvider();
        invokeLatherCall(provider, CommandInfo.CMD_PLUGIN_SEND_REPORT, args);
    }

}
