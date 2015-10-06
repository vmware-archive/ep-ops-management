/*
        Copyright (C) 2010-2014 Pivotal Software, Inc.


        All rights reserved. This program and the accompanying materials
        are made available under the terms of the under the Apache License,
        Version 2.0 (the "License"); you may not use this file except in compliance
        with the License. You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
 */

package com.springsource.hq.plugin.tcserver.plugin.serverconfig;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.hyperic.hq.product.PluginException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Retrieves the properties of the first listener node matching the node attribute specified.
 * 
 * @author jasonkonicki
 * 
 */
public class ServerXmlPropertiesRetriever extends AbstractDocumentCreator implements XmlPropertiesFileRetriever {

    public Map<String, String> getPropertiesFromFile(String filePath, String nodeName, String nodeAttributeName, String nodeAttributeValue)
        throws PluginException {
        ServerXmlParser serverParser = new ServerXmlParser();
        Element serverElement;
        try {
            serverElement = serverParser.parse(createDocument(filePath));
        } catch (ParserConfigurationException e) {
            throw new PluginException("Parser exception: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new PluginException("Error parsing file: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PluginException("File error: " + e.getMessage(), e);
        }
        return getPropertiesFromServerElementByAttribute(serverElement, nodeName, nodeAttributeName, nodeAttributeValue);
    }

    private Map<String, String> getPropertiesFromServerElementByAttribute(final Element serverElement, final String nodeName,
        final String nodeAttributeName, final String nodeAttributeValue) {
        Map<String, String> listenerProperties = new LinkedHashMap<String, String>();
        final NodeList children = serverElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (nodeName.equals(children.item(i).getNodeName())) {
                Element listenerElement = (Element) children.item(i);
                if (listenerElement.getAttribute(nodeAttributeName).equals(nodeAttributeValue)) {
                    NamedNodeMap nodeMap = listenerElement.getAttributes();
                    for (int j = 0; j < nodeMap.getLength(); j++) {
                        listenerProperties.put(nodeMap.item(j).getNodeName(), nodeMap.item(j).getNodeValue());
                    }
                    break;
                }
            }
        }
        return listenerProperties;
    }
}
