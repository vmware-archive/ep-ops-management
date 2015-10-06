/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.util.Relationship;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.util.InetAddressUtils;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.xmlparser.XmlParser;
import org.w3c.dom.Document;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author bharel
 */
public class RelationshipUtils {
    private static final Log log = LogFactory.getLog(RelationshipUtils.class);
    private static final String PROTOCOL_PREFIX = "\\w+://";
    private static final String BRACKETED_IP = "\\[.*\\]";
    private static final String PORT_SUFFIX_REGEX = ".*:\\d+";
    private static final String LOCALHOST = "localhost";
    private static final String LOCALHOST_PREFIX = "localhost.";
    private static final HashMap<String, Properties> propertiesMap = new HashMap<String, Properties>();
    private static String localFqdn;

    public static String executeXMLQuery(String xmlPath,
                                         String configFilePath) {
        File configFile = new File(configFilePath);
        return executeXMLQuery(xmlPath, configFile);
    }

    public static String executeXMLQuery(String xmlPath,
                                         File xmlFile) {
        String res = null;
        try {
            DocumentBuilderFactory factory = XmlParser.createDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            XPathFactory xFactory = XPathFactory.newInstance();
            XPath xpath = xFactory.newXPath();

            res = xpath.evaluate(xmlPath, doc);
        } catch (Exception ex) {
            log.debug("[executeXMLQuery] " + ex, ex);
        }
        return res;
    }

    protected static Properties configFile(String filePath) {
        if (propertiesMap.containsKey(filePath))
            return propertiesMap.get(filePath);

        Properties properties = new Properties();
        // TODO: German, to implement same for Windows OS
        File configFile = new File(filePath);
        if (configFile.exists()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(configFile);
                properties.load(in);
                propertiesMap.put(filePath, properties);
            } catch (FileNotFoundException ex) {
                log.debug(ex, ex);
            } catch (IOException ex) {
                log.debug(ex, ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        log.debug(ex, ex);
                    }
                }
            }
        }

        return properties;
    }

    public static String getFqdn(String containsAddress,
                                 AddressExtractor addressExtractor) {
        return getFqdn(addressExtractor.extractAddress(containsAddress));
    }

    public static Set<String> getFqdns(String address,
                                       String pattern) {
        Set<String> result = new HashSet<String>();
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(address);

        while (matcher.find()) {
            result.add(getFqdn(matcher.group(1)));
        }

        return result;
    }

    public static String getFqdn(String address) {
        if (StringUtils.isBlank(address)) {
            return getLocalFqdn();
        }
        String fqdn = getFqdnByAddress(address);
        if (isLocalhost(fqdn) && getLocalFqdn() != null) {
            return getLocalFqdn();
        }

        return fqdn;
    }

    private static String getFqdnByAddress(String address) {
        String sanitizedAddress = sanitizeAddress(address);
        if (isIp(sanitizedAddress)) {
            InetAddress inetAddr = getAddress(sanitizedAddress);
            if (inetAddr != null) {
                return inetAddr.getCanonicalHostName();
            }
        }

        return sanitizedAddress;
    }

    private static String sanitizeAddress(String address) {
        String sanitizedAddress = removePort(untilSlash(removeProtocol(removeBackslashes(address))));
        return removeEnclosingBrackets(sanitizedAddress);
    }

    private static String removeBackslashes(String address) {
        return address.replace("\\:", ":").replaceAll("\\\\", "/");
    }

    private static String removeProtocol(String address) {
        return address.replaceFirst(PROTOCOL_PREFIX, "");
    }

    private static String untilSlash(String address) {
        int slashIndex = address.indexOf("/");
        if (slashIndex == -1) {
            return address;
        }

        return address.substring(0, slashIndex);
    }

    private static String removeEnclosingBrackets(String address) {
        if (address.matches(BRACKETED_IP)) {
            return address.substring(1, address.length() - 1);
        }

        return address;
    }

    private static String removePort(String address) {
        if (!isIp(address) && address.matches(PORT_SUFFIX_REGEX)) {
            return address.substring(0, address.lastIndexOf(':'));
        }

        return address;
    }

    private static boolean isIp(String address) {
        return InetAddressUtils.isIPv4Address(address) || InetAddressUtils.isIPv6Address(address);
    }

    private static boolean isLocalhost(String fqdn) {
        if (fqdn.equals(LOCALHOST) || fqdn.startsWith(LOCALHOST_PREFIX)) {
            return true;
        }
        InetAddress inetAddress = getAddress(fqdn);
        return (inetAddress != null && inetAddress.isLoopbackAddress());
    }

    private static InetAddress getAddress(String address) {
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static Collection<String> getDnsNames(final String url) {
        Collection<String> dnsNames;
        try {
            DnsNameExtractor dnsExtractor = new DnsNameExtractor();
            dnsNames = dnsExtractor.getDnsNames(url);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dnsNames = new HashSet<String>();
        }
        return dnsNames;
    }

    public static String getWGet(String path) {
        String retValue = null;
        Reader reader = null;
        try {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs,
                                               String authType) {

                }

                public void checkServerTrusted(X509Certificate[] certs,
                                               String authType) {

                }
            } };
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname,
                                      SSLSession session) {
                    return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            URL url = new URL(path);
            URLConnection con;
            try {
                con = url.openConnection();
            } catch (Exception e) {
                log.debug("Couldn't connect to API");
                return StringUtils.EMPTY;
            }

            reader = new InputStreamReader(con.getInputStream());
            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    break;
                }
                retValue += (char) ch;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) { /*ignore*/
                }
            }
        }

        return retValue;
    }

    public static void setLocalFqdn(String localFqdn) {
        RelationshipUtils.localFqdn = localFqdn;
    }

    public static String getLocalFqdn() {
        if (StringUtils.isBlank(localFqdn)) {
            try {
                localFqdn = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (Exception e) {
                log.warn("Failed to get local FQDN", e);
            }
        }

        return localFqdn;
    }

    public static String readFile(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }

        Scanner scanner = null;
        FileInputStream fis = null;
        StringBuilder result = new StringBuilder();
        try {
            result = new StringBuilder();
            fis = new FileInputStream(filePath);
            scanner = new Scanner(fis);

            while (scanner.hasNextLine()) {
                result.append(String.format("%s%n", scanner.nextLine()));
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignore) { /*ignore*/
                }
            }
        }

        return result.toString();
    }

    public static String runCommand(String[] command) {
        return runCommand(command, null);
    }

    public static String runCommand(String[] command,
                                    String[] environmentVariables) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Execute exec = new Execute(new PumpStreamHandler(output));
        exec.setCommandline(command);
        if (environmentVariables != null) {
            exec.setEnvironment(environmentVariables);
        }

        try {
            exec.execute();
        } catch (Exception e) {
            log.debug("Failed to run command: " + exec.getCommandLineString(), e);
            return StringUtils.EMPTY;
        }
        return output.toString().trim();
    }
}
