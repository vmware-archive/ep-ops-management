package com.vmware.epops.webapp.translators.lather;

import java.util.List;

import org.hyperic.hq.bizapp.shared.lather.GetDisabledPlugins_result;
import org.hyperic.lather.LatherValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.epops.command.AgentCommandResponse;
import com.vmware.epops.command.AgentVerifiedCommandData;
import com.vmware.epops.command.upstream.plugins.GetDisabledPluginsCommandData;
import com.vmware.epops.command.upstream.plugins.GetDisabledPluginsCommandResponse;

public class GetDisabledPluginsCommandTranslator implements AgentVerifiedLatherCommandTranslator {

    private final static Logger log = LoggerFactory
                .getLogger(GetDisabledPluginsCommandTranslator.class);

    @Override
    public LatherValue translateResponse(AgentCommandResponse response) {
        if (!(response instanceof GetDisabledPluginsCommandResponse)) {
            log.error("response must be of type GetDisabledPluginsResponse");
            return null;
        }
        GetDisabledPluginsCommandResponse disabledPluginsResponse = (GetDisabledPluginsCommandResponse) response;
        List<String> plugins = disabledPluginsResponse.getDisabledPlugins();
        GetDisabledPlugins_result result = new GetDisabledPlugins_result();
        result.setDisabledPlugins(plugins);
        return result;

    }

    @Override
    public Class<? extends AgentCommandResponse> getResponseType() {
        return GetDisabledPluginsCommandResponse.class;
    }

    @Override
    public AgentVerifiedCommandData translateRequest(LatherValue latherValue,
                                                     String agentToken) {
        return new GetDisabledPluginsCommandData();
    }

}
