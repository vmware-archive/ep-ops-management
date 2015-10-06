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

package org.hyperic.tools.db;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hyperic.util.StrongList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class Column
{
    protected static final int DEFAULT_NONE = 0;
    protected static final int DEFAULT_AUTO_INCREMENT = 1;
    protected static final int DEFAULT_CURRENT_TIME = 2;
    protected static final int DEFAULT_SEQUENCE_ONLY = 3;

    private static final int DATABASE_TYPE = 0; // Any database

    protected String m_strName;
    protected String m_strType;
    protected String m_sReferences;
    protected int m_iSize;
    protected boolean m_bPrimaryKey;
    protected boolean m_bRequired;
    protected int m_iDefault;
    protected String m_sDefault;
    protected int m_iInitialSequence = 1;
    protected int m_iIncrementSequence = 1;

    protected String m_strTableName;

    protected Column(Node node,
                     Table table)
        throws SAXException
    {
        if (Column.isColumn(node) == true)
        {
            NamedNodeMap map = node.getAttributes();

            for (int iAttr = 0; iAttr < map.getLength(); iAttr++)
            {
                Node nodeMap = map.item(iAttr);
                String strName = nodeMap.getNodeName();
                String strValue = nodeMap.getNodeValue();

                if (strName.equalsIgnoreCase("name") == true || strName.equalsIgnoreCase("ref") == true)
                {
                    // Get the Column Name
                    this.m_strName = strValue;
                }
                else if (strName.equalsIgnoreCase("type") == true)
                {
                    // Get the Column Type
                    this.m_strType = strValue;
                }
                else if (strName.equalsIgnoreCase("size") == true)
                {
                    // Get the Column Size
                    this.m_iSize = Integer.parseInt(strValue);
                }
                else if (strName.equalsIgnoreCase("primarykey") == true)
                {
                    // Is this a Primary Key Column
                    this.m_bPrimaryKey = strValue.equalsIgnoreCase("true");
                }
                else if (strName.equalsIgnoreCase("required") == true)
                {
                    // Is the Column NotNull
                    this.m_bRequired = strValue.equalsIgnoreCase("true");
                }
                else if (strName.equalsIgnoreCase("default") == true)
                {
                    // Get the default behavior for the column
                    if (strValue.equalsIgnoreCase("autoincrement") == true)
                        this.m_iDefault = Column.DEFAULT_AUTO_INCREMENT;
                    else if (strValue.equalsIgnoreCase("sequence-only") == true)
                        this.m_iDefault = Column.DEFAULT_SEQUENCE_ONLY;
                    else if (strValue.equalsIgnoreCase("CURRENT_TIME") == true
                                || strValue.equalsIgnoreCase("CURRENTTIME") == true)
                        this.m_iDefault = Column.DEFAULT_CURRENT_TIME;
                    else
                        // Assume that this is a default value for the column
                        this.m_sDefault = strValue;
                }
                else if (strName.equalsIgnoreCase("initial") == true)
                {
                    // Get the initial autoincrement value
                    this.m_iInitialSequence = Integer.parseInt(strValue);
                }
                else if (strName.equalsIgnoreCase("references") == true)
                {
                    this.m_sReferences = strValue;
                }
                else if (strName.equalsIgnoreCase("increment") == true)
                {
                    // Get the increment value for the autoincrement default
                    this.m_iIncrementSequence = Integer.parseInt(strValue);
                }
                else
                    System.out.println("Unknown attribute \'" + nodeMap.getNodeName() + "\' in tag \'table\'");

                this.m_strTableName = table.getName();
            }
        }
        else
            throw new SAXException("node is not a Column.");
    }

    protected Column(ResultSet set)
        throws SQLException
    {
        this.m_strName = set.getString(4);
        this.m_strType = set.getString(6);
        this.m_iSize = set.getInt(7);

        if (set.getInt(11) == DatabaseMetaData.columnNoNulls)
            this.m_bRequired = true;

        // System.out.println(set.getString(13));
    }

    protected int getDefault()
    {
        return this.m_iDefault;
    }

    protected int getInitialSequence()
    {
        return this.m_iInitialSequence;
    }

    protected int getIncrementSequence()
    {
        return this.m_iIncrementSequence;
    }

    protected String getMappedType(Collection typemaps,
                                   int dbtype)
    {
        Iterator iter = typemaps.iterator();
        String strResult = null;
        String strType = this.getType();

        while (iter.hasNext() == true)
        {
            TypeMap map = (TypeMap) iter.next();
            strResult = map.getMappedType(strType, dbtype);

            if (strResult != null)
                break;
        }

        if (strResult == null)
            strResult = strType;

        return strResult;
    }

    protected String getName()
    {
        return this.m_strName.toUpperCase();
    }

    protected String getType()
    {
        return this.m_strType;
    }

    protected int getSize()
    {
        return this.m_iSize;
    }

    protected boolean isPrimaryKey()
    {
        return this.m_bPrimaryKey;
    }

    protected boolean isRequired()
    {
        return this.m_bRequired;
    }

    protected String getReferences()
    {
        return this.m_sReferences;
    }

    protected String getsDefault()
    {
        return this.m_sDefault;
    }

    protected String getCreateCommand(List cmds,
                                      Collection typemaps,
                                      int dbtype)
    {
        String strCmd = this.getName() + ' ' + this.getMappedType(typemaps, dbtype);

        if (this.getSize() > 0) {
            strCmd = strCmd + this.getSizeCommand(cmds);
        }

        if (this.hasDefault() == true)
        {
            String strDefault = this.getDefaultCommand(cmds);

            if (strDefault.length() > 0)
                strCmd = strCmd + ' ' + strDefault;
        }

        if (this.m_sDefault != null)
            strCmd += " DEFAULT '" + this.getsDefault() + "'";

        if (this.isRequired() == true)
            strCmd += " NOT NULL";

        if (this.isPrimaryKey() == true)
            strCmd += " PRIMARY KEY";

        if (this.m_sReferences != null)
            strCmd += " REFERENCES " + this.getReferences();

        return strCmd;
    }

    protected static List getColumns(Node nodeTable,
                                     Table table,
                                     int dbtype)
    {
        // /////////////////////////////////////////////////////////////
        // Get the Columns Names and Related Info

        NodeList listCols = nodeTable.getChildNodes();
        List colResult = new StrongList("org.hyperic.tools.db.Column");

        for (int iCol = 0; iCol < listCols.getLength(); iCol++)
        {
            Node node = listCols.item(iCol);

            if (Column.isColumn(node) == true)
            {
                try
                {
                    Column col;

                    if (dbtype == OracleColumn.getClassType())
                        col = new OracleColumn(node, table);
                    else if (dbtype == InstantDBColumn.getClassType())
                        col = new InstantDBColumn(node, table);
                    else if (dbtype == PostgresColumn.getClassType())
                        col = new PostgresColumn(node, table);
                    else if (dbtype == PointbaseColumn.getClassType())
                        col = new PointbaseColumn(node, table);
                    else if (dbtype == MySQLDBColumn.getClassType())
                        col = new MySQLDBColumn(node, table);
                    else
                        col = new Column(node, table);

                    colResult.add(col);
                } catch (SAXException e)
                {
                }
            }
        }

        return colResult;
    }

    protected static List getColumns(DatabaseMetaData meta,
                                     Table table)
        throws SQLException
    {
        ResultSet setCols = meta.getColumns(null, null, table.getName(), null);
        List colResult = new StrongList("org.hyperic.tools.db.Column");

        while (setCols.next() == true)
        {
            Column col = new Column(setCols);
            colResult.add(col);
        }

        return colResult;
    }

    protected static int getClassType()
    {
        return Column.DATABASE_TYPE;
    }

    protected String getDefaultCommand(List cmds)
    {
        String strCmd = "DEFAULT ";

        switch (this.getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
                strCmd += "AUTOINCREMENT";
                if (this.getInitialSequence() > 0)
                    strCmd = strCmd + " INITIAL " + this.getInitialSequence();

                if (this.getIncrementSequence() > 0)
                    strCmd = strCmd + " INCREMENT " + this.getIncrementSequence();

                break;
            case Column.DEFAULT_CURRENT_TIME:
                strCmd += "CURRENT_TIME";
                break;
        }

        return strCmd;
    }

    protected String getSizeCommand(List cmds)
    {
        return "(" + this.getSize() + ')';
    }

    protected void getPreCreateCommands(List cmds) {
        // Do nothing. Subclasses may need to add commands to the collection.
    }

    protected void getPostCreateCommands(List cmds) {
        // Do nothing. Subclasses may need to add commands to the collection.
    }

    protected void getDropCommands(List cmds)
    {
        // Do nothing. Subclasses may need to add commands to the collection.
    }

    protected boolean hasDefault()
    {
        return (this.m_iDefault != Column.DEFAULT_NONE);
    }

    protected static boolean isColumn(Node node)
    {
        return node.getNodeName().equalsIgnoreCase("column");
    }
}
