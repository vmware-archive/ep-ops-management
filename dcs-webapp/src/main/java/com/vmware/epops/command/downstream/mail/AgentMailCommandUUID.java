package com.vmware.epops.command.downstream.mail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;

public class AgentMailCommandUUID implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String DELIMITER = "\\|";
    private static final String PIPE_DELIMITER = "|";
    private String agentToken;
    private long deliveryTime = 0;
    private AgentMailCommandType commandType;
    private String uuid;

    public AgentMailCommandUUID() {
    }

    public AgentMailCommandUUID(String agentToken,
                                AgentMailCommandType commandType,
                                long deliveryTime) {
        this.agentToken = agentToken;
        this.commandType = commandType;
        this.deliveryTime = deliveryTime;
        this.uuid = UUID.randomUUID().toString();
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "REC_CATCH_EXCEPTION")
    public AgentMailCommandUUID(String commandUUIDstr) {
        String[] parts = commandUUIDstr.split(DELIMITER);
        this.agentToken = parts[0];
        this.uuid = parts[1];
        this.commandType = AgentMailCommandType.valueOf(parts[2]);
    }

    public String getAgentToken() {
        return agentToken;
    }

    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
    }

    public long getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(long deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public AgentMailCommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(AgentMailCommandType commandType) {
        this.commandType = commandType;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        AgentMailCommandUUID other = (AgentMailCommandUUID) obj;
        return new EqualsBuilder().append(this.agentToken, other.getAgentToken())
                    .append(this.uuid, other.getUuid())
                    .append(commandType, other.getCommandType())
                    .isEquals();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { agentToken, uuid, commandType });
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        return builder.append(agentToken).append(PIPE_DELIMITER).append(uuid).append(
                    PIPE_DELIMITER).append(commandType).append(PIPE_DELIMITER).append(deliveryTime).toString();
    }
}
