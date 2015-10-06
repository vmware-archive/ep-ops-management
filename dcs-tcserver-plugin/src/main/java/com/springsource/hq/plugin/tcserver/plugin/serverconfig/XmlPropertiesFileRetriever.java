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

import java.util.Map;

import org.hyperic.hq.product.PluginException;

/**
 * Retrieves the properties from a file.
 * 
 * @author jasonkonicki
 * 
 */
public interface XmlPropertiesFileRetriever {

    /**
     * Retrieves the properties of a node from the specified file.
     * 
     * @param filePath The path to the file.
     * @param nodeName The name of the node to retrieve its properties.
     * @param nodeAttributeName The name of an attribute to identify the correct node.
     * @param nodeAttributeValue The value of an attribute to identify the correct node.
     * @return All of the properties of the specified node with the matching attribute name/value pair.
     * @throws PluginException
     */
    Map<String, String> getPropertiesFromFile(String filePath, String nodeName, String nodeAttributeName, String nodeAttributeValue)
        throws PluginException;
}
