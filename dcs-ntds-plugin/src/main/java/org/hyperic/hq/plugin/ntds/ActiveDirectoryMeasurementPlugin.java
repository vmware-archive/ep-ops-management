package org.hyperic.hq.plugin.ntds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class ActiveDirectoryMeasurementPlugin extends Win32MeasurementPlugin {

    private static final String VERSION_KEY = "version";
    private static final Map<String, String> placeHolderToValuesOldVersions;
    private static final Map<String, String> placeHolderToValuesNewVersions;
    private static final List<String> oldVersions = Arrays.asList(new String[]{"2003 RTM","2003 R2"});
    // private static final List<String> newVersions =
    // Arrays.asList(new String[]{"2008 RTM", "2008 R2", "2012", "2012 RTM", "2012 R2"});

    static {
        Map<String, String> placeHolderToValuesOldVersionsTemp = new HashMap<String, String>(5);
        placeHolderToValuesOldVersionsTemp.put("avail_domain", "NTDS");
        placeHolderToValuesOldVersionsTemp.put("avail_key_1", "Type");
        placeHolderToValuesOldVersionsTemp.put("avail_val_1", "Availability");
        placeHolderToValuesOldVersionsTemp.put("avail_arg_1", "NTLM Authentications");
        placeHolderToValuesOldVersionsTemp.put("NtlmDomain", "NTDS");
        placeHolderToValuesOldVersions = placeHolderToValuesOldVersionsTemp;

        Map<String, String> placeHolderToValuesNewVersionsTemp = new HashMap<String, String>(5);
        placeHolderToValuesNewVersionsTemp.put("avail_domain", "ServiceAvail");
        placeHolderToValuesNewVersionsTemp.put("avail_key_1", "Platform");
        placeHolderToValuesNewVersionsTemp.put("avail_val_1", "Win32");
        placeHolderToValuesNewVersionsTemp.put("avail_arg_1", "ntds");
        placeHolderToValuesNewVersionsTemp.put("NtlmDomain", "Security System-Wide Statistics");
        placeHolderToValuesNewVersions = placeHolderToValuesNewVersionsTemp;
    }

    @Override
    public String translate(String template,
                            ConfigResponse config) {
        String version = config.getValue(VERSION_KEY);
        if (oldVersions.contains(version)) {
            template = fillTemplate(template, placeHolderToValuesOldVersions);
        }
        else {
            template = fillTemplate(template, placeHolderToValuesNewVersions);
        }
        return super.translate(template, config);
    }

    private String fillTemplate(String template,
                                Map<String, String> keyValueMap) {
        for (Entry<String, String> entry : keyValueMap.entrySet()) {
            template = StringUtil.replace(template,
                        "%" + entry.getKey() + "%",
                        entry.getValue());
        }
        return template;
    }

}
