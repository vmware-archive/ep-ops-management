package com.vmware.epops.command.upstream.plugins;

import java.util.List;

import com.vmware.epops.command.upstream.AgentCommandResponseBase;

public class GetDisabledPluginsCommandResponse extends AgentCommandResponseBase {

    List<String> disabledPlugins;

    public GetDisabledPluginsCommandResponse() {
    }

    public GetDisabledPluginsCommandResponse(List<String> disabledPlugins) {
        this.disabledPlugins = disabledPlugins;
    }

    public List<String> getDisabledPlugins() {
        return disabledPlugins;
    }

}
