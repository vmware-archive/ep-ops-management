package org.hyperic.hq.product;

import java.util.Properties;

public class CollectorIdentifier {
    private final String identifier;

    public CollectorIdentifier(Properties props) {
        identifier = props.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CollectorIdentifier) {
            CollectorIdentifier other = (CollectorIdentifier) obj;
            return identifier.equals(other.identifier);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return "collectorIdentifier-" + identifier;
    }
}
