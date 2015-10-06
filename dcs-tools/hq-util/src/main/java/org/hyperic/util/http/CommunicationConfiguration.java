package org.hyperic.util.http;

public class CommunicationConfiguration {

    final boolean isSupportRRDNS;
    final int failPeriodInMin;
    final int downPeriodIntervalInMin;

    public CommunicationConfiguration(boolean isSupportRRDNS,
                                      int failPeriodInMin,
                                      int downPeriodIntervalInMin) {
        this.isSupportRRDNS = isSupportRRDNS;
        this.failPeriodInMin = failPeriodInMin;
        this.downPeriodIntervalInMin = downPeriodIntervalInMin;
    }

    public boolean isSupportRRDNS() {
        return isSupportRRDNS;
    }

    public int getFailPeriodInMin() {
        return failPeriodInMin;
    }

    public int getDownPeriodIntervalInMin() {
        return downPeriodIntervalInMin;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Support RRDNS:").append(isSupportRRDNS).append("  ")
                    .append("Fail period:").append(failPeriodInMin).append("  ")
                    .append("Down period interval:")
                    .append(downPeriodIntervalInMin).append("  ");
        return sb.toString();

    }

}
