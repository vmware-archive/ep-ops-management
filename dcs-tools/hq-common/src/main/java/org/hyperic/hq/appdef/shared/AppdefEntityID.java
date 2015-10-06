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

package org.hyperic.hq.appdef.shared;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.util.StringUtil;

/**
 * a class which represents a unique object within the Appdef framework.
 */

public class AppdefEntityID
            implements Serializable
{

    private static final long serialVersionUID = -8149143799228813017L;

    /**
     * @deprecated HHQ-1456: Do not use this instance variable. It is present only to support object deserialization on
     *             HQ instances just upgraded from 3.1.x to 3.2.
     */
    private int entityType;

    /**
     * @deprecated HHQ-1456: Do not use this instance variable. It is present only to support object deserialization on
     *             HQ instances just upgraded from 3.1.x to 3.2.
     */
    private int entityID;

    private int _entityType; // APPDEF_TYPE_* from AppdefEntityConstants
    private int _entityID; // ID from the database

    /**
     * HHQ-1456: Custom deserialization is only necessary to support HQ instances just upgraded from 3.1.x to 3.2.
     * 
     * @param in The input stream containing the serialized object.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        // assign the "old" 3.1.x instance variable values to the "new" 3.2
        // instance variables
        if (entityType != 0) {
            _entityType = entityType;
            entityType = 0;
        }

        if (entityID != 0) {
            _entityID = entityID;
            entityID = 0;
        }
    }

    /**
     * build an AppdefEntityID from a string key of the form:
     * 
     * [entityType]:[entityID]
     * 
     */
    public AppdefEntityID(String id) {
        try {
            if (id == null)
                throw new InvalidAppdefTypeException("Invalid entity type: " +
                            id);

            List typeList = StringUtil.explode(id, ":");

            if (typeList.size() != 2)
                throw new InvalidAppdefTypeException("Invalid entity type: " +
                            id);

            _entityType = new Integer((String) typeList.get(0)).intValue();
            _entityID = new Integer((String) typeList.get(1)).intValue();
        } catch (NumberFormatException e) {
            throw new InvalidAppdefTypeException("Invalid entity type: " +
                        id);
        }

        if (!AppdefEntityConstants.typeIsValid(_entityType)) {
            throw new InvalidAppdefTypeException("Invalid entity type: " +
                        _entityType);
        }
    }

    public AppdefEntityID(int entityType,
                          int entityID) {
        _entityType = entityType;
        _entityID = entityID;

        if (!AppdefEntityConstants.typeIsValid(entityType)) {
            throw new IllegalArgumentException("Invalid entity type: " +
                        entityType);
        }
    }

    public AppdefEntityID(int entityType,
                          Integer entityID) {
        this(entityType, entityID.intValue());
    }

    public int getType() {
        return _entityType;
    }

    public String getTypeName() {
        return AppdefEntityConstants.typeToString(_entityType);
    }

    public int getID() {
        return _entityID;
    }

    public Integer getId() {
        return new Integer(_entityID);
    }

    public String getAppdefKey() {
        return _entityType + ":" + _entityID;
    }

    /**
     * Return the string name of this entity id object's authz resource type.
     */
    public String getAuthzTypeName() {
        switch (getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return AuthzConstants.platformResType;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return AuthzConstants.serverResType;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return AuthzConstants.serviceResType;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return AuthzConstants.applicationResType;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                return AuthzConstants.groupResType;
            default:
                throw new IllegalArgumentException("Unknown type: " + getType());
        }
    }

    /**
     * Return the ID of this entity id object's authz resource type.
     */
    public Integer getAuthzTypeId() {
        switch (getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return AuthzConstants.authzPlatform;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return AuthzConstants.authzServer;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return AuthzConstants.authzService;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return AuthzConstants.authzApplication;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                return AuthzConstants.authzGroup;
            case AppdefEntityConstants.APPDEF_TYPE_POLICY:
                return AuthzConstants.authzPolicy;
            default:
                throw new IllegalArgumentException("Unknown type: " + getType());
        }
    }

    /**
     * Convenience method to check if this is a platform
     * 
     * @return true if this entity refers to a platform, false otherwise.
     */
    public boolean isPlatform() {
        return getType() == AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    }

    /**
     * Convenience method to check if this is a server
     * 
     * @return true if this entity refers to a server, false otherwise.
     */
    public boolean isServer() {
        return getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER;
    }

    /**
     * Convenience method to check if this is a service
     * 
     * @return true if this entity refers to a service, false otherwise.
     */
    public boolean isService() {
        return getType() == AppdefEntityConstants.APPDEF_TYPE_SERVICE;
    }

    /**
     * Convenience method to check if this is a application
     * 
     * @return true if this entity refers to a application, false otherwise.
     */
    public boolean isApplication() {
        return getType() == AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
    }

    /**
     * Convenience method to check if this is a group
     * 
     * @return true if this entity refers to a group, false otherwise.
     */
    public boolean isGroup() {
        return getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP;
    }

    public String toString() {
        return getAppdefKey();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof AppdefEntityID))
            return false;

        AppdefEntityID o = (AppdefEntityID) other;
        return o._entityID == _entityID &&
                    o._entityType == _entityType;
    }

    public int hashCode() {
        return _entityType * 100000 + _entityID;
    }

    public static AppdefEntityID newPlatformID(Integer id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                    id);
    }

    public static AppdefEntityID newServerID(Integer id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER, id);
    }

    public static AppdefEntityID newServiceID(Integer id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, id);
    }

    public static AppdefEntityID newAppID(Integer id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_APPLICATION,
                    id);
    }

    public static AppdefEntityID newGroupID(Integer id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_GROUP, id);
    }
}
