package org.hyperic.hq.bizapp.shared.lather;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.lather.LatherValue;

public class GetDisabledPlugins_result extends LatherValue {

    private static final String DISABLED_PLUGINS = "disabledPlugins";

    @SuppressWarnings("unchecked")
    public List<String> getDisabledPlugins() {

        if (null != getObject(DISABLED_PLUGINS)) {
            return (List<String>) getObject(DISABLED_PLUGINS);
        }
        return new ArrayList<String>();
    }

    public void setDisabledPlugins(List<String> plugins) {
        addObject(DISABLED_PLUGINS, (Serializable) plugins);
    }
}
