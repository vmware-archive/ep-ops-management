/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.autoinventory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.StringUtil;
import org.hyperic.util.StringifiedException;

public class ScanState {

    private static HashMap installdirExcludes = new HashMap();
    private static List installdirExcludesPrefixes = new ArrayList();
    private static final Log _log = LogFactory.getLog(ScanState.class);

    static {
        loadInstalldirExcludes();
    }

    private DateFormat dateFmt = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.MEDIUM);

    private ScanStateCore _core;

    /** ScanMethodClassName->ScanMethod */
    private Map _scanMethods = null;

    private boolean _isDefaultScan = false;
    private boolean _isSyncScan = false;

    public ScanState() {
        _core = new ScanStateCore();
    }

    public ScanState(ScanStateCore core) {
        _core = core;
    }

    public ScanStateCore getCore() {
        return _core;
    }

    public void setCore(ScanStateCore core) {
        _core = core;
    }

    public boolean getIsDefaultScan() {
        return _isDefaultScan;
    }

    public void setIsDefaultScan(boolean b) {
        _isDefaultScan = b;
    }

    public boolean isSyncScan() {
        return _isSyncScan;
    }

    public void setIsSyncScan(boolean b) {
        _isSyncScan = b;
    }

    public boolean getAreServersIncluded() {
        return _core.getAreServersIncluded();
    }

    public void setAreServersIncluded(boolean b) {
        _core.setAreServersIncluded(b);
    }

    public long getStartTime() {
        return _core.getStartTime();
    }

    /**
     * @return the formatted start time
     */
    public String getStartTimeStr() {
        if (getStartTime() == 0)
            return "00/00/00 00:00:00";

        return dateFmt.format(new Long(getStartTime()));
    }

    public void setStartTime(long startTime) {
        _core.setStartTime(startTime);
    }

    public long getEndTime() {
        return _core.getEndTime();
    }

    public String getEndTimeStr() {
        if (getEndTime() == 0)
            return "00/00/00 00:00:00";

        return dateFmt.format(new Long(getEndTime()));
    }

    public void setEndTime(long endTime) {
        _core.setEndTime(endTime);
    }

    /**
     * @return the formatted relapsed time
     */
    public String getElapsedTimeStr() {
        long end = 0;

        // if the start time is zero, return no elapsed time
        if (getStartTime() == 0)
            return StringUtil.formatDuration(0);

        if (getEndTime() == 0)
            end = (new Date()).getTime();
        else
            end = getEndTime();

        return StringUtil.formatDuration(end - getStartTime());
    }

    public boolean getIsDone() {
        return _core.getIsDone();
    }

    public void setIsDone() {
        _core.setIsDone(true);
    }

    public boolean getIsInterrupted() {
        return _core.getIsInterrupted();
    }

    public void setIsInterrupted() {
        _core.setIsInterrupted(true);
    }

    public StringifiedException getGlobalException() {
        return _core.getGlobalException();
    }

    public void setGlobalException(Throwable _globalException) {
        _core.setGlobalException(new StringifiedException(_globalException));
    }

    /**
     * Tell the scan state what scan methods will be run.
     * 
     * @param scanMethods An array of ScanMethod class names that represent the ScanMethods that will be run in this
     *            scan.
     */
    public void setScanMethods(String[] scanMethods)
        throws AutoinventoryException {

        // Walk thru the list and construct the ScanMethodState[] array
        ScanMethodState[] smStates = new ScanMethodState[scanMethods.length];
        for (int i = 0; i < scanMethods.length; i++) {
            smStates[i] = new ScanMethodState();
            smStates[i].setMethodClass(scanMethods[i]);
        }
        _core.setScanMethodStates(smStates);

        setupMethodHash();
    }

    /**
     * Setup our internal hash of ScanMethodClassName->ScanMethod
     */
    protected void setupMethodHash()
        throws AutoinventoryException {

        _scanMethods = new HashMap();

        ScanMethodState[] smStates = _core.getScanMethodStates();
        if (smStates == null)
            return;

        ScanMethod method;
        String methodClass;
        for (int i = 0; i < smStates.length; i++) {
            methodClass = smStates[i].getMethodClass();
            try {
                method = (ScanMethod) Class.forName(methodClass).newInstance();
            } catch (Exception e) {
                throw new AutoinventoryException(methodClass
                            + ": error instantiating: "
                            + e, e);
            }
            _scanMethods.put(methodClass, method);
        }
    }

    public boolean completedOK() {
        return _core.getIsDone()
                    && (!hasExceptions())
                    && (!_core.getIsInterrupted());
    }

    public boolean hasExceptions() {
        ScanMethodState[] smStates = _core.getScanMethodStates();
        StringifiedException[] exceptions;
        for (int i = 0; i < smStates.length; i++) {
            exceptions = smStates[i].getExceptions();
            if (exceptions != null && exceptions.length > 0) {
                return true;
            }
        }
        return false;
    }

    public void initStartTime() {
        _core.setStartTime(System.currentTimeMillis());
    }

    public void initEndTime() {
        _core.setEndTime(System.currentTimeMillis());
    }

    public long getScanDuration() {
        long startTime = _core.getStartTime();
        long endTime = _core.getEndTime();
        if (endTime == 0) {
            if (startTime == 0) {
                return 0;
            }
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    public AIPlatformValue getPlatform() {
        return _core.getPlatform();
    }

    public void setPlatform(AIPlatformValue platform) {
        _core.setPlatform(platform);
    }

    public String getCertDN() {
        return _core.getPlatform().getCertdn();
    }

    public void setCertDN(String certDN) {
        _core.setCertDN(certDN);
    }

    public void addScanException(ScanMethod scanMethod,
                                 Throwable t) {

        ScanMethodState smState = findSMState("addScanException", scanMethod);
        smState.addException(new StringifiedException(t));
    }

    public void addScanExceptions(ScanMethod scanMethod,
                                  Throwable[] t) {

        ScanMethodState smState = findSMState("addScanException", scanMethod);
        smState.addExceptions(t);
    }

    public void setScanStatus(ScanMethod scanMethod,
                              String status) {
        ScanMethodState smState = findSMState("addScanException", scanMethod);
        smState.setStatus(status);
    }

    public ScanMethodState[] getScanMethodStates() {
        ScanMethodState[] smStates = _core.getScanMethodStates();
        return smStates;
    }

    // XXX temporary during refactoring
    private AIServerValue getServerValue(Object o) {
        AIServerValue server;
        if (o instanceof AIServerValue) {
            server = (AIServerValue) o;
        }
        else {
            server = (AIServerValue) ((ServerResource) o).getResource();
        }

        if (!server.cTimeHasBeenSet()) {
            server.setCTime(new Long(System.currentTimeMillis()));
        }

        return server;
    }

    private List excludeServers(List servers) {
        if (installdirExcludes.size() == 0) {
            return servers;
        }

        ArrayList includes = new ArrayList();

        for (int i = 0; i < servers.size(); i++) {
            Object server = servers.get(i);
            String installpath =
                        getServerValue(server).getInstallPath();
            boolean exclude = false;
            if (installdirExcludes.get(installpath) != null) {
                continue;
            }
            for (int j = 0; j < installdirExcludesPrefixes.size(); j++) {
                String prefix = (String) installdirExcludesPrefixes.get(j);
                if (installpath.startsWith(prefix)) {
                    exclude = true;
                    break;
                }
            }
            if (!exclude) {
                includes.add(server);
            }
        }

        return includes;
    }

    /**
     * Add servers to the list of servers detected for a particular scan method.
     * 
     * @param scanMethod The scan method to add servers to.
     * @param servers A List of AIServer objects representing the servers (and their services) that were detected and
     *            should be added and associated with the given scan method.
     */
    public void addServers(ScanMethod scanMethod,
                           List servers) {
        servers = excludeServers(servers);
        if (servers.isEmpty()) {
            return;
        }
        ScanMethodState smState = findSMState("addServers", scanMethod);
        AIServerValue[] newServers;
        AIServerValue[] existingServers = smState.getServers();
        if (existingServers == null) {
            newServers = new AIServerValue[servers.size()];
            for (int i = 0; i < newServers.length; i++) {
                newServers[i] = getServerValue(servers.get(i));
            }

        } else {
            List<AIServerValue> allServers = new ArrayList<AIServerValue>(Arrays.asList(existingServers));
            // Get rid of any servers we have already discovered (by autoinventoryidentifier).
            // ServerDetectors are ordered, so first one should always win
            for (Iterator iterator = servers.iterator(); iterator.hasNext();) {
                AIServerValue server = getServerValue(iterator.next());
                String identifier = server.getAutoinventoryIdentifier();
                boolean newIdentifier = true;
                for (AIServerValue existingServer : existingServers) {
                    if (existingServer.getAutoinventoryIdentifier().equals(identifier)) {
                        newIdentifier = false;
                        break;
                    }
                }
                if (newIdentifier) {
                    allServers.add(server);
                }
            }
            newServers = allServers.toArray(new AIServerValue[allServers.size()]);
        }
        smState.setServers(newServers);
    }

    /**
     * For debugging purposes, print out the servers that were detected.
     */
    public void printServers() {

        ScanMethodState[] smStates = _core.getScanMethodStates();
        AIServerValue[] servers;
        for (int i = 0; i < smStates.length; i++) {
            System.err.println("Detected by: " + smStates[i].getMethodClass());
            servers = smStates[i].getServers();
            for (i = 0; i < servers.length; i++) {
                System.err.println("\t" + servers[i]);
            }
        }
    }

    /**
     * For debugging purposes, print stack traces for all exceptions
     */
    public void printStackTraces() {
        ScanMethodState[] smStates = _core.getScanMethodStates();
        StringifiedException[] exc;
        for (int i = 0; i < smStates.length; i++) {
            exc = smStates[i].getExceptions();
            if (exc != null && exc.length > 0) {
                System.err.println("Exceptions for method "
                            + smStates[i].getMethodClass() + ":");
                for (int j = 0; j < exc.length; j++) {
                    System.err.println("\n" + exc[j].getStackTrace());
                }
            }
        }
    }

    /**
     * For debugging and command-line use, pretty-print full status info.
     * 
     * @param out The stream to write to.
     */
    public void printFullStatus(PrintStream out)
        throws AutoinventoryException {

        StringifiedException globalEx = _core.getGlobalException();
        ScanMethodState[] smStates = _core.getScanMethodStates();

        if (globalEx != null) {
            out.println("Severe failure: " + globalEx);
            out.println(globalEx.getStackTrace());
        }

        if (smStates == null) {
            out.println("scan not yet started.");
            return;
        }
        printMainStatus(out);

        for (int i = 0; i < smStates.length; i++) {
            printMethodStatus(smStates[i], out);
        }
    }

    public void printMainStatus(PrintStream out) {

        out.print("\nOVERALL STATUS: ");

        String status = null;
        if (_core.getIsInterrupted()) {
            status = "interrupted before normal completion";

        } else if (_core.getIsDone()) {
            status = "completed";

        } else {
            status = "scan in progress";
        }

        if (_core.getGlobalException() != null) {
            status += ", however a general scanning error occurred";
        } else if (hasExceptions()) {
            status += " successfully, however one or more scan methods had errors";
        } else if (_core.getIsDone()) {
            status += " successfully with no errors";
        }

        out.println(status);

        String duration = StringUtil.formatDuration(getScanDuration());
        out.println("Run time: " + duration);

        AIPlatformValue platform = _core.getPlatform();
        if (platform != null) {
            out.println("\nPlatform Detected:");
            out.println("\t" + platform);
            out.println("\tIP addresses: "
                        + StringUtil.arrayToString(platform.getAIIpValues()));
        } else {
            out.println("\nNo Platform Detected!");
        }
    }

    public void printMethodStatus(ScanMethodState smState,
                                  PrintStream out)
        throws AutoinventoryException {

        ScanMethod method = findScanMethod(smState.getMethodClass());
        out.println("\n" + method.getName() + ":");

        // Print exception (if any)
        StringifiedException[] t = smState.getExceptions();
        String status = smState.getStatus();
        if (t != null && t.length > 0) {
            out.println("\t* SCAN FAILED: ");
            if (status != null) {
                out.println("\t* Last status before failure: " + status);
            } else {
                out.println("\t* No status message available.");
            }
            for (int i = 0; i < t.length; i++) {
                out.println("\t* " + t[i].toString());
                out.println("\t* Stack Trace:");
                out.println(t[i].getStackTrace());
            }

        } else {
            if (status == null) {
                out.println("\t* Status: unknown");
            } else {
                out.println("\t* Status: " + status);
            }
        }

        // PRINT SERVERS DETECTED
        AIServerValue[] servers = smState.getServers();
        if (servers == null || servers.length == 0) {
            out.println("\t* No Servers Detected");

        } else {
            out.println("\t* Detected Servers:");
            for (int i = 0; i < servers.length; i++) {
                out.println("\t" + servers[i]);
            }
        }
        out.println("\n");
    }

    public String toString() {
        if (_core == null)
            return "[ScanState]";
        return _core.toString();
    }

    /**
     * Get the set of all servers detected in this autoinventory scan. This is the method that reconciles the fact that
     * multiple scan methods may have discovered the same server. We assemble the list of all servers by iterating over
     * each scan method in order of authority level. The scan methods with the highest authority level have their
     * servers added first. Scan methods with lower authority levels will have their servers added as long as they have
     * a different autoinventory ID from ones discovered by methods with higher authority levels.
     * 
     * @return A Set of AIServerValue objects. The Set uniqueness is based on the server autoinventory identifier, which
     *         is usually the same as the install path.
     */
    public Set getAllServers()
        throws AutoinventoryException {

        // allServers will guarantee uniqueness on the AIID.
        Set allServers = new TreeSet(COMPARE_AIID);

        // Put all the scan methods in a list
        Map scanMethods = getScanMethodMap();
        Iterator iter = scanMethods.keySet().iterator();
        List smList = new ArrayList();
        while (iter.hasNext()) {
            smList.add(scanMethods.get(iter.next()));
        }

        // Sort the scan methods by authority level.
        Collections.sort(smList, COMPARE_AUTH);

        // Iterate over the scan methods
        AIServerValue[] servers;
        String methodClass;
        ScanMethod method;
        ScanMethodState smState;
        for (int i = 0; i < smList.size(); i++) {

            method = (ScanMethod) smList.get(i);
            smState = findSMState("getAllServers", method);
            methodClass = smState.getMethodClass();

            servers = smState.getServers();
            if (servers != null) {
                for (int j = 0; j < servers.length; j++) {
                    if (!allServers.add(servers[j])) {
                        if (_log != null) {
                            _log.info("Server not added because another scan "
                                        + "method already detected it:"
                                        + servers[j]);
                        }
                    }
                }
            }
        }

        Map mServers = new HashMap();
        // look for servers with the same Metric Connect HashCode
        // for example, two JBoss servers with different installpath
        // but the same config: jnp://localhost:1099
        for (Iterator allIter = allServers.iterator(); allIter.hasNext();)
        {
            Object o = allIter.next();

            if (!(o instanceof AIServerExtValue)) {
                continue;
            }

            AIServerExtValue server = (AIServerExtValue) o;

            if (!server.getAutoEnable()) {
                continue;
            }

            int hashCode = server.getMetricConnectHashCode();
            if (hashCode == 0) {
                continue;
            }

            Integer key = new Integer(hashCode);
            AIServerExtValue cServer =
                        (AIServerExtValue) mServers.get(key);

            if (cServer == null) {
                // based on ScanImpl authority, the first server
                // found is the most likely to be running.
                mServers.put(key, server);
            }
            else {
                // found a server with the same connect config
                // turn off AutoEnable
                server.setAutoEnable(false);
                // disable metric collection
                server.unsetMeasurementConfig();

                if (_log != null) {
                    _log.info("Turning off AutoEnable for server " +
                                server.getName() +
                                " [" + server.getInstallPath() + "]" +
                                ", has the same metric connect config as " +
                                cServer.getName() +
                                " [" + cServer.getInstallPath() + "]");
                }
            }
        }

        return allServers;
    }

    protected ScanMethodState findSMState(String caller,
                                          ScanMethod scanMethod) {

        ScanMethodState[] smStates = _core.getScanMethodStates();
        String smClassName = scanMethod.getClass().getName();
        for (int i = 0; i < smStates.length; i++) {
            if (smStates[i].getMethodClass().equals(smClassName)) {
                return smStates[i];
            }
        }

        throw new IllegalArgumentException("Error finding smState: "
                    + smClassName + ", caller="
                    + caller);
    }

    protected ScanMethod findScanMethod(String methodClass)
        throws AutoinventoryException {

        Map scanMethods = getScanMethodMap();

        ScanMethod m = (ScanMethod) scanMethods.get(methodClass);
        if (m != null)
            return m;

        throw new IllegalArgumentException("ScanMethod not found: "
                    + methodClass);
    }

    private Map getScanMethodMap()
        throws AutoinventoryException {
        if (_scanMethods == null)
            setupMethodHash();
        return _scanMethods;
    }

    public boolean isSameState(ScanState other)
        throws AutoinventoryException {

        // Compare platform attributes
        AIPlatformValue p1, p2;
        p1 = getPlatform();
        p2 = other.getPlatform();

        if (!AICompare.compareAIPlatforms(p1, p2))
            return false;

        Set servers1, servers2;
        servers1 = getAllServers();
        servers2 = other.getAllServers();
        if (!AICompare.compareAIServers(servers1, servers2))
            return false;

        return true;
    }

    private static Comparator COMPARE_AIID = new ServerComparator_AIID();

    static class ServerComparator_AIID implements Comparator {
        public ServerComparator_AIID() {
        }

        public int compare(Object o1,
                           Object o2) {
            if (o1 instanceof AIServerValue &&
                        o2 instanceof AIServerValue) {
                return ((AIServerValue) o2).getAutoinventoryIdentifier()
                            .compareTo(((AIServerValue) o1).getAutoinventoryIdentifier());
            }
            return 0; // all other object are "equal"
        }

        public boolean equals(Object o) {
            return false;
        }
    }

    private static Comparator COMPARE_AUTH = new ServerComparator_AuthLevel();

    static class ServerComparator_AuthLevel implements Comparator {
        public ServerComparator_AuthLevel() {
        }

        public int compare(Object o1,
                           Object o2) {
            if (o1 instanceof ScanMethod &&
                        o2 instanceof ScanMethod) {
                return ((ScanMethod) o2).getAuthorityLevel()
                            - ((ScanMethod) o1).getAuthorityLevel();
            }
            return 0; // all other object are "equal"
        }

        public boolean equals(Object o) {
            return false;
        }
    }

    // any installpath found in $AGENT_CONF_DIR/installdir.excludes
    // will not be reported by AI
    private static void loadInstalldirExcludes() {
        File excludes = new File(AgentConfig.AGENT_CONF_DIR, "installdir.excludes");
        if (!excludes.exists()) {
            return;
        }

        FileReader is = null;
        try {
            is = new FileReader(excludes);
            BufferedReader in = new BufferedReader(is);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.charAt(0) == '#') {
                    continue;
                }
                if (line.endsWith("*")) {
                    line = line.substring(0, line.length() - 1);
                    installdirExcludesPrefixes.add(line);
                }
                installdirExcludes.put(line, Boolean.TRUE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
