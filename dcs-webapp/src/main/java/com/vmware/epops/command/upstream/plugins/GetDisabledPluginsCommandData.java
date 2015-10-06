package com.vmware.epops.command.upstream.plugins;

import com.vmware.epops.command.upstream.AgentVerifiedCommandDataImpl;

public class GetDisabledPluginsCommandData extends AgentVerifiedCommandDataImpl {

    @Override
    public String getCommandName() {
        return "getDisabledPlugins";
    }

}
