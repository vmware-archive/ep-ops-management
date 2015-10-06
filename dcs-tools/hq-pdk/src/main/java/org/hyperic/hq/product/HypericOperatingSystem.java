package org.hyperic.hq.product;

import org.hyperic.sigar.OperatingSystem;

/**
 * Please use PlatformDetector instead. For more specific uses you can use SupportedOperatingSystem (regarding supported
 * platforms) and OperatingSystem (regarding OS details) directly.
 */
@Deprecated
public class HypericOperatingSystem {
    public static final String NAME_HYPER_V_WIN32 = "";
    public static final String[] WIN32_NAMES = { OperatingSystem.NAME_WIN32 };
    private static HypericOperatingSystem instance = null;
    public static final String[] NAMES = PlatformDetector.PLATFORM_NAMES;
    public static final boolean IS_WIN32 = PlatformDetector.IS_WIN32;
    public static final boolean IS_HYPER_V = false;

    public static boolean isSupported(String name) {
        return PlatformDetector.isSupportedPlatform(name);
    }

    public static boolean isWin32(String name) {
        return PlatformDetector.isWin32(name);
    }

    private HypericOperatingSystem() {
    }

    public static synchronized HypericOperatingSystem getInstance() {
        if (instance == null) {
            instance = new HypericOperatingSystem();
        }
        return instance;
    }

    public String getName() {
        return OperatingSystem.getInstance().getName();
    }

    public String getDescription() {
        return OperatingSystem.getInstance().getDescription();
    }

    public String getArch() {
        return OperatingSystem.getInstance().getArch();
    }

    public String getVersion() {
        return OperatingSystem.getInstance().getVersion();
    }

    public String getVendor() {
        return OperatingSystem.getInstance().getVendor();
    }

    public String getVendorVersion() {
        return OperatingSystem.getInstance().getVendorVersion();
    }
}
