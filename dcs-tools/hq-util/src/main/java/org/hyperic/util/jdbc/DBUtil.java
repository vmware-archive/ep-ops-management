/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.util.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DBUtil {
    protected static final Log log = LogFactory.getLog(DBUtil.class);

    // Constants for DB Errors that we want to catch in other classes
    public static final int ORACLE_ERROR_DIVIDE_BY_ZERO = 1476;
    public static final int ORACLE_ERROR_NOT_AVAILABLE = 1034;

    // Constants for PostgreSQL errors
    // May be found at:
    // http://www.postgresql.org/docs/8.0//errcodes-appendix.html
    public static final int POSTGRES_ERROR_DIVIDE_BY_ZERO = 22012;
    public static final int POSTGRES_CONNECTION_EXCEPTION = 8000;
    public static final int POSTGRES_CONNECTION_FAILURE = 8006;
    public static final int POSTGRES_UNABLE_TO_CONNECT = 8001;

    // Constants for MySQL errors
    // May be found at:
    // http://dev.mysql.com/doc/refman/5.0/en/error-messages-client.html
    public static final int MYSQL_LOCAL_CONN_ERROR = 2002;
    public static final int MYSQL_REMOTE_CONN_ERROR = 2003;

    // Constants for supported databases.
    public static final int DATABASE_UNKNOWN = 0;
    public static final int DATABASE_POSTGRESQL_7 = 1;
    public static final int DATABASE_POSTGRESQL_8 = 2;
    public static final int DATABASE_ORACLE_8 = 3;
    public static final int DATABASE_ORACLE_9 = 4;
    public static final int DATABASE_ORACLE_10 = 8;
    public static final int DATABASE_MYSQL5 = 9;
    public static final int DATABASE_ORACLE_11 = 10;
    public static final int DATABASE_POSTGRESQL_9 = 11;

    // Built-in DB constants
    private static String BUILTIN_DB_JDBC_URL = "jdbc:postgresql://127.0.0.1:9432";
    private static String BUILTIN_DB_NAME = "hqdb";

    public static final int IN_CHUNK_SIZE = 200;

    private static Map _dbTypes = new HashMap();

    private DataSource dataSource;

    @Autowired
    public DBUtil(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Constructor is private because this class should never be instantiated.
     */
    // private DBUtil() {}

    /**
     * Close a database connection. No exception is thrown if it fails, but a warning is logged.
     * 
     * @param ctx The logging context to use if a warning should be issued.
     * @param c The connection to close.
     */
    public static void closeConnection(Object ctx,
                                       Connection c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Exception e) {
            log.warn(ctx.toString() + ": Error closing connection.", e);
        }
    }

    /**
     * Close a database statement. No exception is thrown if it fails, but a warning is logged.
     * 
     * @param ctx The logging context to use if a warning should be issued.
     * @param s The statement to close.
     */
    public static void closeStatement(Object ctx,
                                      Statement s) {
        if (s == null)
            return;
        try {
            s.close();
        } catch (Exception e) {
            log.warn(ctx.toString() + ": Error closing statement.", e);
        }
    }

    /**
     * Close a database result set. No exception is thrown if it fails, but a warning is logged.
     * 
     * @param ctx The logging context to use if a warning should be issued.
     * @param rs The result set to close.
     */
    public static void closeResultSet(Object ctx,
                                      ResultSet rs) {
        if (rs == null)
            return;
        try {
            rs.close();
        } catch (Exception e) {
            log.warn(ctx.toString() + ": Error closing result set.", e);
        }
    }

    /**
     * Close a connection, statement, and result set in one fell swoop. You can pass null for any argument and all will
     * be OK :) No exception is thrown if any close fails, but warnings will be logged.
     * 
     * @param ctx The logging context to use if warnings should be issued.
     * @param c The connection to close.
     * @param s The statement set to close.
     * @param rs The result set to close.
     */
    public static void closeJDBCObjects(Object ctx,
                                        Connection c,
                                        Statement s,
                                        ResultSet rs) {
        closeResultSet(ctx, rs);
        closeStatement(ctx, s);
        closeConnection(ctx, c);
    }

    /**
     * Get the next value of a sequence
     */
    public static int getNextSequenceValue(String ctx,
                                           Connection conn,
                                           String table,
                                           String key)
        throws SQLException {

        String query = null;

        // What database is this connection hitting?
        int dbType = getDBType(conn);
        switch (dbType) {
            case DATABASE_POSTGRESQL_7:
            case DATABASE_POSTGRESQL_8:
            case DATABASE_POSTGRESQL_9:
                query = "SELECT nextval('" + table + "_" + key + "_seq'::text)";
                break;
            case DATABASE_ORACLE_8:
            case DATABASE_ORACLE_9:
            case DATABASE_ORACLE_10:
            case DATABASE_ORACLE_11:
                query = "SELECT " + table + "_" + key + "_seq.nextval " + "FROM DUAL";
                break;
            case DATABASE_MYSQL5:
                query = "SELECT MAX(" + key + ") + 1 FROM " + table;
                break;

            default:
                throw new SequencesNotSupportedException();
        }

        PreparedStatement selectPS = null;
        ResultSet rs = null;
        try {
            selectPS = conn.prepareStatement(query);
            rs = selectPS.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SequenceRetrievalException();
            }
        } finally {
            closeResultSet(ctx, rs);
            closeStatement(ctx, selectPS);
        }
    }

    /**
     * Given a Connection object, this method returns a constant indicating what type of database the Connection is
     * connected to.
     * 
     * @param conn The connection whose database type the caller wished to ascertain.
     * @return One of the DATABASE_XXX constants defined in this class.
     */
    public static int getDBType(Connection conn)
        throws SQLException {

        Class connClass = conn.getClass();
        Integer dbTypeInteger = (Integer) _dbTypes.get(connClass);
        int dbType = DATABASE_UNKNOWN;

        if (dbTypeInteger == null) {

            DatabaseMetaData dbMetaData = conn.getMetaData();
            String dbName = dbMetaData.getDatabaseProductName().toLowerCase();
            String dbVersion = dbMetaData.getDatabaseProductVersion().toLowerCase();
            log.debug("getDBType: dbName='" + dbName + "', version='" + dbVersion + "'");

            if (dbName.indexOf("postgresql") != -1) {
                if (dbVersion.startsWith("7.")) {
                    dbType = DATABASE_POSTGRESQL_7;
                } else if (dbVersion.startsWith("8.")) {
                    dbType = DATABASE_POSTGRESQL_8;
                } else if (dbVersion.startsWith("9.")) {
                    dbType = DATABASE_POSTGRESQL_9;
                }
            } else if (dbName.indexOf("oracle") != -1) {
                if (dbVersion.startsWith("oracle8")) {
                    dbType = DATABASE_ORACLE_8;
                } else if (dbVersion.startsWith("oracle9")) {
                    dbType = DATABASE_ORACLE_9;
                } else if (dbVersion.startsWith("oracle database 10g")) {
                    dbType = DATABASE_ORACLE_10;
                } else if (dbVersion.startsWith("oracle database 11g")) {
                    dbType = DATABASE_ORACLE_11;
                }
            } else if (dbName.indexOf("mysql") != -1) {
                dbType = DATABASE_MYSQL5;
            }

            _dbTypes.put(connClass, new Integer(dbType));

        } else {
            dbType = dbTypeInteger.intValue();
        }

        return dbType;
    }

    /**
     * Is the database PostgreSQL?
     */
    public static boolean isPostgreSQL(Connection c)
        throws SQLException {
        int type = getDBType(c);
        return isPostgreSQL(type);
    }

    public static boolean isPostgreSQL(int type) {
        return (type == DATABASE_POSTGRESQL_7 ||
                    type == DATABASE_POSTGRESQL_8 || type == DATABASE_POSTGRESQL_9);
    }

    /**
     * Is the database Oracle?
     */
    public static boolean isOracle(Connection c)
        throws SQLException {
        int type = getDBType(c);
        return isOracle(type);
    }

    public static boolean isOracle(int type) {
        return (type == DATABASE_ORACLE_8 || type == DATABASE_ORACLE_9 || type == DATABASE_ORACLE_10 || type == DATABASE_ORACLE_11);
    }

    /**
     * Is the database MySQL?
     */
    public static boolean isMySQL(Connection c)
        throws SQLException {
        int type = getDBType(c);
        return isMySQL(type);
    }

    public static boolean isMySQL(int type) {
        return type == DATABASE_MYSQL5;
    }

    public boolean isBuiltinDB() {
        boolean isBuiltin = false;
        Connection conn = null;
        try {
            conn = getConnection();
            DatabaseMetaData dbMetaData = conn.getMetaData();
            String url = dbMetaData.getURL();
            closeConnection(log, conn);

            if (url != null) {
                // built-in db url in the format of:
                // jdbc:postgresql://127.0.0.1:9432/hqdb?protocolVersion=2

                url = url.toLowerCase();
                isBuiltin = url.startsWith(BUILTIN_DB_JDBC_URL) &&
                            url.indexOf(BUILTIN_DB_NAME) > BUILTIN_DB_JDBC_URL.length();
            }
        } catch (SQLException e) {
            log.warn("Error retrieving database meta data.", e);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("isBuiltinDB=" + isBuiltin);
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Error closing connection", e);
                }
            }
        }

        return isBuiltin;
    }

    /**
     * get a connection for a datasource
     */
    public Connection getConnection()
        throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Get the type for a boolean as a string for the required database. This should use the same XML file as DBSetup
     * uses, but this will work for now.
     * 
     * @param conn - a connection
     * @return booleanStr - the appropriate boolean type for the db you're using
     */
    public static String getBooleanType(Connection conn)
        throws SQLException {
        int type = getDBType(conn);
        switch (type) {
            case DATABASE_ORACLE_8:
            case DATABASE_ORACLE_9:
            case DATABASE_ORACLE_10:
            case DATABASE_ORACLE_11:
                return "NUMBER(1)";
            case DATABASE_MYSQL5:
                return "BIT";
            default:
                return "BOOLEAN";
        }

    }

    /**
     * Get the value for a boolean as a string for the required database
     * 
     * @param bool - the boolean you want
     * @param conn - a connection
     * @return booleanStr - the appropriate boolean string for the db you're using
     */
    public static String getBooleanValue(boolean bool,
                                         Connection conn)
        throws SQLException {
        int type = getDBType(conn);
        switch (type) {
            case DATABASE_ORACLE_8:
            case DATABASE_ORACLE_9:
            case DATABASE_ORACLE_10:
            case DATABASE_ORACLE_11:
            case DATABASE_MYSQL5:
                return bool ? "1" : "0";
            default:
                return bool ? "'1'" : "'0'";
        }
    }

    /**
     * Fill out a PreparedStatement correctly with a boolean.
     * 
     * @param bool - the boolean you want
     * @param conn - a connection
     * @return booleanStr - the appropriate boolean string for the db you're using
     */
    public static void setBooleanValue(boolean bool,
                                       Connection conn,
                                       PreparedStatement ps,
                                       int idx)
        throws SQLException {
        int type = getDBType(conn);
        switch (type) {
            case DATABASE_ORACLE_8:
            case DATABASE_ORACLE_9:
            case DATABASE_ORACLE_10:
            case DATABASE_ORACLE_11:
            case DATABASE_MYSQL5:
                ps.setInt(idx, (bool) ? 1 : 0);
                return;
            default:
                ps.setBoolean(idx, bool);
                return;
        }

    }

    /**
     * Seek through the specified ResultSet to the beginning of the page specified by the PageControl
     * 
     * @param rs The result set to seek through.
     * @param pc The page control that indicates how far to seek
     * @return The number of records actually skipped over in the seek.
     */
    public static int seek(ResultSet rs,
                           PageControl pc)
        throws SQLException {

        int stop = pc.getPagenum() * pc.getPagesize();
        int i = 0;
        while (i < stop && rs.next())
            i++;
        return i;
    }

    /**
     * Check to see if a column exists in a table.
     * 
     * @param ctx The logging context to use.
     * @param c The DB connection to use.
     * @param table The table to check.
     * @param column The column to look for. This is done in a case-insensitive manner.
     * @return true if the column exists in the table, false otherwise
     * @throws SQLException If any kind of DB error occurs.
     */
    public static boolean checkColumnExists(String ctx,
                                            Connection c,
                                            String table,
                                            String column)
        throws SQLException {

        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd;
        String checkColumnSql = "SELECT * FROM " + table + " WHERE 1=0";

        try {
            ps = c.prepareStatement(checkColumnSql);
            rs = ps.executeQuery();
            rsmd = rs.getMetaData();
            int numCols = rsmd.getColumnCount();
            for (int i = 0; i < numCols; i++) {
                if (rsmd.getColumnName(i + 1).equalsIgnoreCase(column)) {
                    return true;
                }
            }
            return false;

        } finally {
            closeJDBCObjects(ctx, null, ps, rs);
        }
    }

    /**
     * Creates a string that consists of the clause repeated a number (iterations) of times, joined by the conjunction
     * string
     * 
     * @param iterations the number of times to repeat the clause
     * @param conjunction the string used to join the clause
     * @param clause the clause to repeat
     * @return the resulting String
     */
    private static String composeConjunctions(int iterations,
                                              String conjunction,
                                              String clause) {
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < iterations; i++) {
            if (i > 0)
                strBuf.append(conjunction);

            strBuf.append(clause);
        }

        return strBuf.toString();
    }

    /**
     * Creates the SQL query that is used in PreparedStatement for 0 or more values for a given column. For example:
     * <p/>
     * SELECT id FROM TABLE WHERE id IN (?, ?, ?) SELECT id FROM TABLE WHERE id = ?
     * 
     * @param column the name of the column to query against
     * @param iterations the number of variables
     * @return the WHERE clause (without the WHERE keyword)
     */
    public static String composeConjunctions(String column,
                                             int iterations) {
        if (iterations > 1) {
            StringBuffer strBuf = new StringBuffer(column).append(" IN (").append(
                        composeConjunctions(iterations, ",", "?")).append(") ");

            return strBuf.toString();
        } else if (iterations == 1) {
            return " " + column + "=? ";
        } else {
            // No conditions at all
            return " 1=1 ";
        }
    }

    public static void replacePlaceHolder(StringBuffer buf,
                                          String repl) {
        int index = buf.indexOf("?");
        if (index >= 0)
            buf.replace(index, index + 1, repl);
    }

    public static void replacePlaceHolders(StringBuffer buf,
                                           Object[] objs) {
        for (int i = 0; i < objs.length; i++)
            replacePlaceHolder(buf, objs[i].toString());
    }

}
