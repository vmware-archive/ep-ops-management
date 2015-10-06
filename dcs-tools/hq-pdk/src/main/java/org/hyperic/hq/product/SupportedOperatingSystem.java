package org.hyperic.hq.product;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hyperic.sigar.OperatingSystem;

public class SupportedOperatingSystem {
    private static final Set<String> supportedUnixPlatforms = new HashSet<String>();
    private static final Set<String> supportedWindowsPlatforms = new HashSet<String>();
    private static final Set<String> supportedPlatforms = new HashSet<String>();

    static {
        Collections.addAll(supportedUnixPlatforms, OperatingSystem.UNIX_NAMES);
        supportedUnixPlatforms.remove(OperatingSystem.NAME_MACOSX);
        supportedUnixPlatforms.remove(OperatingSystem.NAME_FREEBSD);
        supportedUnixPlatforms.remove(OperatingSystem.NAME_NETBSD);
        supportedUnixPlatforms.remove(OperatingSystem.NAME_OPENBSD);

        Collections.addAll(supportedWindowsPlatforms, OperatingSystem.WIN32_NAMES);

        supportedPlatforms.addAll(supportedUnixPlatforms);
        supportedPlatforms.addAll(supportedWindowsPlatforms);
    }

    public static boolean isSupported(String name) {
        return supportedPlatforms.contains(name);
    }

    public static Set<String> getSupportedPlatforms() {
        return supportedPlatforms;
    }

    public static Set<String> getSupportedUnixPlatforms() {
        return supportedUnixPlatforms;
    }

    public static Set<String> getSupportedWindowsPlatforms() {
        return supportedWindowsPlatforms;
    }
}
