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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.hyperic.hq.product.PluginException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractXmlParser extends AbstractDocumentCreator implements XmlParser {

    abstract public Element parse(Document document) throws PluginException;

    protected void writeDocument(final Document document, String fileName) throws IOException {
        final OutputFormat out = new OutputFormat(document);
        out.setIndenting(true);

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(new File(fileName));
            final XMLSerializer xmlSer = new XMLSerializer(fos, out);
            xmlSer.serialize(document);

            fos.flush();
            fos.getFD().sync();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }
}
