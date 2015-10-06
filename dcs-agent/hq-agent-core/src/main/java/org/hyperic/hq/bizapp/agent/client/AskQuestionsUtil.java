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

package org.hyperic.hq.bizapp.agent.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.sigar.Sigar;

public class AskQuestionsUtil {

    private final AgentConfig configs;
    private final static String OPTIONS_SUFFIX_YES_NO_MORE = " (yes/no/more)?";
    private final static String OPTIONS_SUFFIX_YES_NO = " (yes/no)?";

    public AskQuestionsUtil(AgentConfig agentConfig) {
        this.configs = agentConfig;
    }

    public boolean askYesNoQuestion(String question,
                                    boolean def,
                                    String questionProp)
        throws IOException,
        AutoQuestionException {
        boolean isAuto;
        final String fullQuestion = question + OPTIONS_SUFFIX_YES_NO;

        isAuto = configs.getBootProperty(questionProp) != null;

        while (true) {
            String res;

            res = this.askQuestion(fullQuestion, def ? "yes" : "no", questionProp);
            if (res.equalsIgnoreCase("yes") || res.equalsIgnoreCase("y")) {
                return true;
            } else if (res.equalsIgnoreCase("no") || res.equalsIgnoreCase("n")) {
                return false;
            }

            if (isAuto) {
                throw new AutoQuestionException("Property '" + questionProp
                            + "' must be 'yes' or " + "'no'.");
            }

            System.out.println("- Value must be 'yes' or 'no'");
        }
    }

    // Used to ask a yes/no question where "more"/"m" is a valid manual answer
    public Boolean askYesNoMoreQuestion(String question,
                                        boolean defaultVal,
                                        String questionProp)
        throws IOException, AutoQuestionException {
        boolean isAuto;
        String fullQuestion = question + OPTIONS_SUFFIX_YES_NO_MORE;

        isAuto = configs.getBootProperty(questionProp) != null;

        while (true) {
            String res;

            res = this.askQuestion(fullQuestion, defaultVal ? "yes" : "no",
                        questionProp);
            if (res.equalsIgnoreCase("yes") || res.equalsIgnoreCase("y")) {
                return true;
            } else if (res.equalsIgnoreCase("no") || res.equalsIgnoreCase("n")) {
                return false;
            } else if (res.equalsIgnoreCase("more") || res.equalsIgnoreCase("m")) {
                return null;
            }

            if (isAuto) {
                throw new AutoQuestionException("Property '" + questionProp
                            + "' must be 'yes' or " + "'no'.");
            }

            System.out.println("- Value must be 'yes', 'no' or 'more'");
        }
    }

    public int askIntQuestion(String question,
                              int def,
                              String questionProp)
        throws IOException, AutoQuestionException {

        boolean isAuto = configs.getBootProperty(questionProp) != null;
        while (true) {
            String res;
            int iVal;

            res = this.askQuestion(question, Integer.toString(def),
                        questionProp);
            try {
                iVal = Integer.parseInt(res);
                return iVal;
            } catch (NumberFormatException exc) {
                if (isAuto) {
                    throw new AutoQuestionException("Property '" + questionProp
                                + "' must be a valid integer.");
                }
                System.out.println("- Value must be an integer.");
            }
        }
    }

    /**
     * Asks a question but accepts empty answers
     * 
     * @param question
     * @param defaultAnswer
     * @param questionProp
     * @return null if an empty answer was entered and no default defined, defaultAnswer if default was provided and
     *         answer was empty. Property value if questionProp was defined, and user answer otherwise.
     * @throws IOException
     */
    public String askQuestionAcceptEmptyAnswer(String question,
                                               String defaultAnswer,
                                               String questionProp)
        throws IOException {
        return this.askQuestion(question, defaultAnswer, false, true, questionProp);
    }

    public String askQuestion(String question,
                              String def,
                              String questionProp)
        throws IOException {
        return this.askQuestion(question, def, false, false, questionProp);
    }

    public String askQuestion(String question,
                              String defaultAnswer,
                              boolean dontShowValueToUser,
                              boolean acceptEmptyInput,
                              String questionProp)
        throws IOException {
        BufferedReader in;
        String res = null;
        String bootProp = configs.getBootProperty(questionProp);

        while (true) {
            System.out.print(question);
            if (defaultAnswer != null) {
                System.out.print(" [default=" + defaultAnswer + "]");
            }

            System.out.print(": ");

            if (dontShowValueToUser) {
                if (bootProp != null) {
                    System.out.println("**Not echoing value**");
                    return bootProp;
                }
                return Sigar.getPassword("");
            } else {
                if (bootProp != null) {
                    if (bootProp.equals("*default*") && defaultAnswer != null) {
                        bootProp = defaultAnswer;
                    }

                    System.out.println(bootProp);
                    return bootProp;
                }

                in = new BufferedReader(new InputStreamReader(System.in));
                if ((res = in.readLine()) != null) {
                    res = res.trim();
                    if (res.length() == 0) {
                        res = null;
                    }
                }

                if (res == null) {
                    if (defaultAnswer != null) {
                        return defaultAnswer;
                    }
                    if (acceptEmptyInput) {
                        return res;
                    }
                } else {
                    return res;
                }
            }
        }
    }

    public static class AutoQuestionException extends Exception {
        private static final long serialVersionUID = 1L;

        public AutoQuestionException(String s) {
            super(s);
        }
    }

    public static class UserQuestionException extends Exception {
        private static final long serialVersionUID = 1L;

        public UserQuestionException(String s) {
            super(s);
        }
    }
}
