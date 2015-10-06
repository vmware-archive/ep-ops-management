/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2007], Hyperic, Inc. 
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

package org.hyperic.hibernate.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

import org.hyperic.util.jdbc.DBUtil;

/**
 * This class must be public for Hibernate to access it.
 */
public class PostgreSQLDialect
            extends org.hibernate.dialect.PostgreSQLDialect
            implements HQDialect {
    private static final String logCtx = PostgreSQLDialect.class.getName();

    public boolean supportsDeferrableConstraints() {
        return true;
    }

    public String getCascadeConstraintsString() {
        return " cascade ";
    }

    public boolean dropConstraints() {
        return false;
    }

    public String getCreateSequenceString(String sequenceName) {
        return new StringBuffer()
                    .append("create sequence ")
                    .append(sequenceName)
                    .append(" start ")
                    .append(HypericDialectConstants.SEQUENCE_START)
                    .append(" increment 1 ")
                    .toString();
    }

    public String getOptimizeStmt(String table,
                                  int cost)
    {
        return "ANALYZE " + table;
    }

    public boolean supportsDuplicateInsertStmt() {
        return false;
    }

    public boolean supportsMultiInsertStmt() {
        return true;
    }

    public boolean tableExists(Statement stmt,
                               String tableName)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            String sql = "SELECT tablename from pg_tables" +
                        " WHERE lower(tablename) = lower('" + tableName + "')";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                return true;
            return false;
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    public boolean viewExists(Statement stmt,
                              String viewName)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            String sql = "SELECT viewname from pg_views" +
                        " WHERE lower(viewname) = lower('" + viewName + "')";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                return true;
            return false;
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    public String getLimitString(int num) {
        return "LIMIT " + num;
    }

    public String getLimitBuf(String sql,
                              int offset,
                              int limit) {
        StringBuilder buf = new StringBuilder(sql);
        buf.append(" LIMIT ");
        buf.append(limit);
        if (offset > 0) {
            buf.append(" offset ").append(offset);
        }
        return buf.toString();
    }

    public boolean usesSequenceGenerator() {
        return true;
    }

    public String getRegExSQL(String column,
                              String regex,
                              boolean ignoreCase,
                              boolean invertMatch) {
        String op = " ~ ";
        if (ignoreCase && invertMatch) {
            op = " !~* ";
        } else if (ignoreCase) {
            op = " ~* ";
        } else if (invertMatch) {
            op = " !~ ";
        }
        return new StringBuffer()
                    .append(column)
                    .append(op)
                    .append(regex)
                    .toString();
    }

    public boolean useEamNumbers() {
        return true;
    }

    public int getMaxExpressions() {
        return -1;
    }

    public boolean supportsPLSQL() {
        return false;
    }

    public boolean useMetricUnion() {
        return false;
    }

    public String getMetricDataHint() {
        return "";
    }

    public Long getSchemaCreationTimestampInMillis(Statement stmt)
        throws SQLException {
        ResultSet rs = null;
        Date installDate = null;

        try {
            String[] sqls = new String[] {
                        "select CTIME from EAM_AGENT_TYPE where ID = 1",
                        "select CTIME from EAM_APPLICATION_TYPE where ID = 2",
                        "select CTIME from EAM_RESOURCE_GROUP where ID = 0",
                        "select CTIME from EAM_ALERT_DEFINITION where ID = 0",
                        "select CTIME from EAM_ESCALATION where ID = 100"
            };

            for (String sql : sqls) {
                rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    Date date = new Date(rs.getLong(1));

                    if (installDate == null) {
                        installDate = date;
                    } else {
                        Calendar cal1 = Calendar.getInstance();
                        Calendar cal2 = Calendar.getInstance();

                        cal1.setTime(installDate);
                        cal2.setTime(date);

                        // Compare date with previous one (they should all be the same date)...
                        if (cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)
                                    || cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR)) {
                            // ...Something has been tampered with!...
                            return null;
                        }
                    }
                }
            }
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
        }

        return installDate.getTime();
    }

    public boolean analyzeDb() {
        return false;
    }

    public boolean supportsAsyncCommit() {
        return true;
    }

    public String getSetAsyncCommitStmt(boolean on) {
        return "set synchronous_commit to " + (on ? "on" : "off");
    }
}
