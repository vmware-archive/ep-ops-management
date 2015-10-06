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

package org.hyperic.util.xmlparser;

import org.hyperic.util.StringUtil;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.xml.sax.EntityResolver;

/**
 * The main entry point && bulk of XmlParser. The parsing routine takes an entry-point tag, which provides information
 * about subtags, attributes it takes, etc. Tags can implement various interfaces to tell the parser to call back when
 * certain conditions are met. This class takes the role of both a minimal validator as well as a traversal mechanism
 * for building data objects out of XML.
 */

public class XmlParser {
    private XmlParser() {
    }

    private static void checkAttributes(Element elem,
                                        XmlTagHandler tag,
                                        XmlFilterHandler filter)
        throws XmlAttrException
    {
        boolean handlesAttrs = tag instanceof XmlAttrHandler;
        XmlAttr[] attrs;

        if (handlesAttrs)
            attrs = ((XmlAttrHandler) tag).getAttributes();
        else
            attrs = new XmlAttr[0];

        // Ensure out all the required && optional attributes
        for (int i = 0; i < attrs.length; i++) {
            Attribute a = null;
            boolean found = false;

            for (Iterator j = elem.getAttributes().iterator(); j.hasNext();) {
                a = (Attribute) j.next();

                if (a.getName().equalsIgnoreCase(attrs[i].getName())) {
                    found = true;
                    break;
                }
            }

            if (!found && attrs[i].getType() == XmlAttr.REQUIRED) {
                throw new XmlRequiredAttrException(elem,
                            attrs[i].getName());
            }

            if (found && handlesAttrs) {
                String val;

                val = filter.filterAttrValue(tag, a.getName(), a.getValue());

                ((XmlAttrHandler) tag).handleAttribute(i, val);
            }
        }

        // Second loop to handle unknown attributes
        for (Iterator i = elem.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            boolean found = false;

            for (int j = 0; j < attrs.length; j++) {
                if (a.getName().equalsIgnoreCase(attrs[j].getName())) {
                    found = true;
                    break;
                }
            }

            if (found)
                continue;

            if (tag instanceof XmlUnAttrHandler) {
                XmlUnAttrHandler handler;
                String val;

                val = filter.filterAttrValue(tag, a.getName(), a.getValue());

                handler = (XmlUnAttrHandler) tag;
                handler.handleUnknownAttribute(a.getName(), val);
            } else {
                throw new XmlUnknownAttrException(elem, a.getName());
            }
        }

        if (tag instanceof XmlEndAttrHandler) {
            ((XmlEndAttrHandler) tag).endAttributes();
        }
    }

    private static void checkSubNodes(Element elem,
                                      XmlTagHandler tag,
                                      XmlFilterHandler filter)
        throws XmlAttrException, XmlTagException
    {
        XmlTagInfo[] subTags = tag.getSubTags();
        Map hash;

        hash = new HashMap();

        // First, count how many times each sub-tag is referenced
        for (Iterator i = elem.getChildren().iterator(); i.hasNext();) {
            Element e = (Element) i.next();
            String name;
            Integer val;

            name = e.getName().toLowerCase();
            if ((val = (Integer) hash.get(name)) == null) {
                val = new Integer(0);
            }

            val = new Integer(val.intValue() + 1);
            hash.put(name, val);
        }

        for (int i = 0; i < subTags.length; i++) {
            String name = subTags[i].getTag().getName().toLowerCase();
            Integer iVal = (Integer) hash.get(name);
            int threshold = 0, val;

            val = iVal == null ? 0 : iVal.intValue();

            switch (subTags[i].getType()) {
                case XmlTagInfo.REQUIRED:
                    if (val == 0) {
                        throw new XmlMissingTagException(elem, name);
                    } else if (val != 1) {
                        throw new XmlTooManyTagException(elem, name);
                    }
                    break;
                case XmlTagInfo.OPTIONAL:
                    if (val > 1) {
                        throw new XmlTooManyTagException(elem, name);
                    }
                    break;
                case XmlTagInfo.ONE_OR_MORE:
                    threshold++;
                case XmlTagInfo.ZERO_OR_MORE:
                    if (val < threshold) {
                        throw new XmlMissingTagException(elem, name);
                    }
                    break;
            }

            hash.remove(name);
        }

        // Now check for excess sub-tags
        if (hash.size() != 0) {
            Set keys = hash.keySet();

            throw new XmlTooManyTagException(elem,
                        (String) keys.iterator().next());
        }

        // Recurse to all sub-tags
        for (Iterator i = elem.getChildren().iterator(); i.hasNext();) {
            Element child = (Element) i.next();

            for (int j = 0; j < subTags.length; j++) {
                XmlTagHandler subTag = subTags[j].getTag();
                String subName = subTag.getName();

                if (child.getName().equalsIgnoreCase(subName)) {
                    XmlParser.processNode(child, subTag, filter);
                    break;
                }
            }
        }
    }

    private static void processNode(Element elem,
                                    XmlTagHandler tag,
                                    XmlFilterHandler filter)
        throws XmlAttrException, XmlTagException
    {
        if (tag instanceof XmlTagEntryHandler) {
            ((XmlTagEntryHandler) tag).enter();
        }

        if (tag instanceof XmlFilterHandler) {
            filter = (XmlFilterHandler) tag;
        }

        XmlParser.checkAttributes(elem, tag, filter);
        if (tag instanceof XmlTextHandler) {
            ((XmlTextHandler) tag).handleText(elem.getText());
        }
        XmlParser.checkSubNodes(elem, tag, filter);

        if (tag instanceof XmlTagExitHandler) {
            ((XmlTagExitHandler) tag).exit();
        }
    }

    private static class DummyFilter
                implements XmlFilterHandler
    {
        public String filterAttrValue(XmlTagHandler tag,
                                      String attrName,
                                      String attrValue)
        {
            return attrValue;
        }
    }

    /**
     * Parse an input stream, otherwise the same as parsing a file
     */
    public static void parse(InputStream is,
                             XmlTagHandler tag)
        throws XmlParseException
    {
        parse(is, tag, null);
    }

    public static void parse(InputStream is,
                             XmlTagHandler tag,
                             EntityResolver resolver)
        throws XmlParseException
    {
        SAXBuilder builder;
        Document doc;

        builder = new SAXBuilder();

        if (resolver != null) {
            builder.setEntityResolver(resolver);
        }

        try {
            if (resolver != null) {
                // WTF? seems relative entity URIs are allowed
                // by certain xerces impls. but fully qualified
                // file://... URLs trigger a NullPointerException
                // in others. setting base here worksaround
                doc = builder.build(is, "");
            }
            else {
                doc = builder.build(is);
            }
        } catch (JDOMException exc) {
            XmlParseException toThrow = new XmlParseException(exc.getMessage());
            toThrow.initCause(exc);
            throw toThrow;
        } catch (IOException exc) {
            XmlParseException toThrow = new XmlParseException(exc.getMessage());
            toThrow.initCause(exc);
            throw toThrow;
        }

        generalParse(tag, doc);
    }

    /**
     * Parse a file, which should have a root which is the associated tag.
     * 
     * @param in File to parse
     * @param tag Root tag which the parsed file should contain
     */
    public static void parse(File in,
                             XmlTagHandler tag)
        throws XmlParseException
    {
        SAXBuilder builder;
        Document doc;

        builder = new SAXBuilder();
        InputStream is = null;

        // open the file ourselves. the builder(File)
        // method escapes " " -> "%20" and bombs
        try {
            is = new FileInputStream(in);
            doc = builder.build(is);
        } catch (IOException exc) {
            throw new XmlParseException(exc.getMessage());
        } catch (JDOMException exc) {
            throw new XmlParseException(exc.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        generalParse(tag, doc);
    }

    /** General parsing used by both parse methods above */
    private static void generalParse(XmlTagHandler tag,
                                     Document doc)
        throws XmlParseException
    {

        Element root = doc.getRootElement();
        if (!root.getName().equalsIgnoreCase(tag.getName())) {
            throw new XmlParseException("Incorrect root tag.  Expected <" +
                        tag.getName() + "> but got <" +
                        root.getName() + ">");
        }
        XmlParser.processNode(root, tag, new DummyFilter());
    }

    private static void dumpAttrs(XmlAttr[] attrs,
                                  String typeName,
                                  int type,
                                  PrintStream out,
                                  int indent)
    {
        String printMsg;
        boolean printed = false;
        int lineBase, lineLen;

        if (attrs.length == 0)
            return;

        lineLen = 0;
        printMsg = "- Has " + typeName + " attributes: ";
        lineBase = indent + printMsg.length();
        // Required attributes
        for (int i = 0; i < attrs.length; i++) {
            String toPrint;

            if (attrs[i].getType() != type)
                continue;

            if (!printed) {
                toPrint = StringUtil.repeatChars(' ', indent) +
                            "- Has " + typeName + " attributes: ";
                out.print(toPrint);
                lineLen = toPrint.length();
                printed = true;
            }

            toPrint = attrs[i].getName() + ", ";
            lineLen += toPrint.length();
            out.print(toPrint);
            if (lineLen > 70) {
                out.println();
                out.print(StringUtil.repeatChars(' ', lineBase));
                lineLen = lineBase;
            }
        }
        if (printed)
            out.println();
    }

    private static void dumpNode(XmlTagHandler tag,
                                 PrintStream out,
                                 int indent)
        throws XmlTagException
    {
        XmlTagInfo[] subTags = tag.getSubTags();

        out.println(StringUtil.repeatChars(' ', indent) +
                    "Tag <" + tag.getName() + ">:");
        if (tag instanceof XmlAttrHandler) {
            XmlAttr[] attrs;

            attrs = ((XmlAttrHandler) tag).getAttributes();
            if (attrs.length == 0)
                out.println(StringUtil.repeatChars(' ', indent) +
                            "- has no required or optional attributes");

            XmlParser.dumpAttrs(attrs, "REQUIRED", XmlAttr.REQUIRED,
                        out, indent);
            XmlParser.dumpAttrs(attrs, "OPTIONAL", XmlAttr.OPTIONAL,
                        out, indent);
        } else {
            out.println(StringUtil.repeatChars(' ', indent) +
                        "- has no required or optional attributes");
        }

        if (tag instanceof XmlUnAttrHandler)
            out.println(StringUtil.repeatChars(' ', indent) +
                        "- handles arbitrary attributes");

        subTags = tag.getSubTags();
        if (subTags.length == 0) {
            out.println(StringUtil.repeatChars(' ', indent) +
                        "- has no subtags");
        } else {
            for (int i = 0; i < subTags.length; i++) {
                String name = subTags[i].getTag().getName();
                int type = subTags[i].getType();

                out.print(StringUtil.repeatChars(' ', indent) +
                            "- has subtag <" + name + ">, which ");
                switch (type) {
                    case XmlTagInfo.REQUIRED:
                        out.println("is REQUIRED");
                        break;
                    case XmlTagInfo.OPTIONAL:
                        out.println("is OPTIONAL");
                        break;
                    case XmlTagInfo.ONE_OR_MORE:
                        out.println("is REQUIRED at least ONCE");
                        break;
                    case XmlTagInfo.ZERO_OR_MORE:
                        out.println("can be specified any # of times");
                        break;
                }

                XmlParser.dumpNode(subTags[i].getTag(), out, indent + 4);
            }
        }
    }

    public static void dump(XmlTagHandler root,
                            PrintStream out) {
        try {
            XmlParser.dumpNode(root, out, 0);
        } catch (XmlTagException exc) {
            out.println("Error traversing tags: " + exc.getMessage());
        }
    }

    private static String bold(String text) {
        return "<emphasis role=\"bold\">" + text + "</emphasis>";
    }

    private static String tag(String name) {
        return bold("&lt;" + name + "&gt;");
    }

    private static String listitem(String name,
                                   String desc) {
        String item =
                    "<listitem><para>" + name + "</para>";
        if (desc != null) {
            item += "<para>" + desc + "</para>";
        }
        return item;
    }

    private static void dumpAttrsWiki(XmlAttr[] attrs,
                                      String typeName,
                                      int type,
                                      PrintStream out,
                                      int indent)
    {
        boolean printed = false;
        if (attrs.length == 0) {
            return;
        }

        // Required attributes
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].getType() != type) {
                continue;
            }

            if (!printed) {
                out.println(StringUtil.repeatChars('*', indent) +
                            " " + typeName + " attributes: ");
                printed = true;
            }

            out.println(StringUtil.repeatChars('*', indent) +
                        " " + attrs[i].getName());
        }
    }

    private static void dumpNodeWiki(XmlTagHandler tag,
                                     PrintStream out,
                                     int indent)
        throws XmlTagException
    {
        XmlTagInfo[] subTags = tag.getSubTags();

        if (indent == 1) {
            out.println(StringUtil.repeatChars('*', indent) +
                        " Tag *<" + tag.getName() + ">*: ");
        }

        if (tag instanceof XmlAttrHandler) {
            XmlAttr[] attrs;

            attrs = ((XmlAttrHandler) tag).getAttributes();

            dumpAttrsWiki(attrs, "*REQUIRED*", XmlAttr.REQUIRED,
                        out, indent + 1);
            dumpAttrsWiki(attrs, "*OPTIONAL*", XmlAttr.OPTIONAL,
                        out, indent + 1);
        }

        subTags = tag.getSubTags();
        if (subTags.length != 0) {

            for (int i = 0; i < subTags.length; i++) {
                String name = subTags[i].getTag().getName();
                int type = subTags[i].getType();
                String desc = "";

                switch (type) {
                    case XmlTagInfo.REQUIRED:
                        desc = "REQUIRED";
                        break;
                    case XmlTagInfo.OPTIONAL:
                        desc = "OPTIONAL";
                        break;
                    case XmlTagInfo.ONE_OR_MORE:
                        desc = "REQUIRED at least ONCE";
                        break;
                    case XmlTagInfo.ZERO_OR_MORE:
                        desc = "can be specified any # of times";
                        break;
                }

                out.println(StringUtil.repeatChars('*', indent + 1) +
                            " Sub Tag *<" + name + ">* " + desc);
                dumpNodeWiki(subTags[i].getTag(), out, indent + 1);
            }
        }
    }

    public static void dumpWiki(XmlTagHandler root,
                                PrintStream out) {
        try {
            dumpNodeWiki(root, out, 1);
        } catch (XmlTagException exc) {
            out.println("Error traversing tags: " + exc.getMessage());
        }
    }

    public static DocumentBuilderFactory createDocumentBuilderFactory()
        throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf;
    }

    public static TransformerFactory createTransformerFactory()
        throws TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        return transformerFactory;
    }
}
