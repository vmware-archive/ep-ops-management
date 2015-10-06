/*
 * Copyright (c) 2015 VMware, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vmware.epops.command.downstream.mail;

/**
 * 
 * Thrown to indicate that a command failed to be translated by one of the translators due to: (1) No Translator found
 * for the command. (2) The Translator doesn't support the translation of the given command type. (3) The command is not
 * implemented.
 * 
 */

public class IllegalCommandException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private CommandError commandError;
    private String commandData;
    private String translatorType;

    public enum CommandError {
        COMMAND_NOT_SUPPORTED("Command not supported by translator"),
        COMMAND_NOT_IMPLEMENTED("Command not implemented");

        private String errorMessage;

        private CommandError(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public IllegalCommandException(CommandError commandError) {
        super(commandError.errorMessage);
        this.commandError = commandError;
    }

    public IllegalCommandException(CommandError commandError,
                                   String commandData,
                                   String translatorType) {
        super(commandError.errorMessage);
        this.commandError = commandError;
        this.commandData = commandData;
        this.translatorType = translatorType;
    }

    @Override
    public String toString() {
        if (commandData == null || translatorType == null) {
            return new StringBuilder()
                        .append("[Error: ")
                        .append(commandError.errorMessage)
                        .append("]").toString();
        } else {
            return new StringBuilder()
                        .append("[Error: ")
                        .append(commandError.errorMessage)
                        .append(", Command Data: ")
                        .append(commandData)
                        .append(", Translator: ")
                        .append(translatorType)
                        .append("]")
                        .toString();
        }
    }
}
