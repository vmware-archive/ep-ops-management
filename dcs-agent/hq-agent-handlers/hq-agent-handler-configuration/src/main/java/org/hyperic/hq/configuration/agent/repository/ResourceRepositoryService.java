/*
 * Copyright (c) 2015 VMware, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.hyperic.hq.configuration.agent.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.configuration.agent.repository.ConfigurationInfoTranslator.PlatformKeyConfigPrefix;
import org.hyperic.hq.configuration.agent.repository.ResourceRepositoryException.ResourceRepositoryError;
import org.hyperic.hq.configuration.agent.repository.model.ConfigurationInfo;
import org.hyperic.hq.configuration.agent.repository.model.ConfigurationInfo.ChangeType;
import org.hyperic.hq.configuration.agent.repository.model.ResourceKey;
import org.hyperic.hq.product.TypeInfo;
import org.springframework.util.CollectionUtils;

/**
 * This service maintains an in-memory representation of resources hierarchy, along with their configuration and
 * scheduling info. This service provides an API to construct, update and query the hierarchy.
 * 
 */
public class ResourceRepositoryService {

    private static final Log log = LogFactory.getLog(ResourceRepositoryService.class);
    private static final int MAX_HIERARCHY_DEPTH = 3;

    private static ResourceRepositoryService instance;

    private final MultiValueMap resourceRelationships;
    private final Map<ResourceKey, ConfigurationInfo> configurationInfos;

    /**
     * Returns a singleton instance of ResourceRepositoryService.
     * 
     * @return A singleton instance of ResourceRepositoryService.
     */
    synchronized public static ResourceRepositoryService getInstance() {
        if (instance == null) {
            instance = new ResourceRepositoryService();
        }
        return instance;
    }

    public ResourceRepositoryService() {
        resourceRelationships = new MultiValueMap();
        configurationInfos = new HashMap<ResourceKey, ConfigurationInfo>();
    }

    /**
     * Compares given configuration with the exsiting one. If configurations differ, then swap current configuration
     * with given one. The method returns the compare result between current configuration and given one. In case given
     * configuration refers to virtual server, the method will only return compare result.
     * 
     * @param newConfiguration - The configuration to add/update in the repository.
     * @throws MissingConfigurationInfoExeption
     */
    public ChangeType compareAndSwap(ConfigurationInfo newConfiguration)
        throws ResourceRepositoryException {

        if (newConfiguration == null) {
            throw new ResourceRepositoryException(ResourceRepositoryError.NULL_CONFIG);
        }
        if (newConfiguration.getResourceKind() == null) {
            throw new ResourceRepositoryException(ResourceRepositoryError.MISSING_RESOURCE_KIND);
        }
        ResourceKey resourceKey = newConfiguration.getResourceKey();
        if (resourceKey == null) {
            throw new ResourceRepositoryException(ResourceRepositoryError.MISSING_RESOURCE_KEY);
        }

        // Compare old and new configuration, if no change there is no need
        // to update repository
        ConfigurationInfo oldConfigInfo = configurationInfos.get(newConfiguration.getResourceKey());
        ChangeType changeType = newConfiguration.compare(oldConfigInfo);
        if (changeType == null) {
            throw new ResourceRepositoryException(ResourceRepositoryError.RESOURCE_KINDS_DIFFER);
        }

        if (ChangeType.EQUAL.equals(changeType)) {
            return changeType;
        }

        configurationInfos.put(resourceKey, newConfiguration);
        ResourceKey parentResourceKey = resourceKey.getParentResourceKey();
        if (parentResourceKey != null) {
            // Add relationship only if not added before, in order to avoid
            // duplicates
            if (resourceRelationships.containsKey(parentResourceKey)) {
                if (!resourceRelationships.getCollection(parentResourceKey).contains(resourceKey)) {
                    resourceRelationships.put(parentResourceKey, resourceKey);
                }
            } else {
                resourceRelationships.put(parentResourceKey, resourceKey);
            }

        }
        return changeType;
    }

    /**
     * Returns the stored configuration for a given resource key. If no such key, return null.
     * 
     * @param resourceKey - the resource key of desired configuration info.
     * @return Configuration info of the associated key, or null if not exists.
     */
    public ConfigurationInfo getConfiguration(ResourceKey resourceKey) {
        return configurationInfos.get(resourceKey);
    }

    /**
     * Returns a resource parent configuration info if exists in the reposiroty, or null if not
     */
    public ConfigurationInfo getParentConfiguration(ResourceKey resourceKey) {
        ResourceKey parentResourceKey = resourceKey.getParentResourceKey();
        if (parentResourceKey == null) {
            return null;
        }
        return configurationInfos.get(parentResourceKey);
    }

    /**
     * Return true if a resource is ready for configuration and/or scheduling. A resource is considered to be ready, if
     * all of its ancestors exsits in this repository.
     * 
     * @param resourceKey the resource key of the resource in question
     * @return true if the resource is ready for configuration and/or scheduling, false o.w.
     * @throws ResourceRepositoryException
     */
    public boolean isResourceReadyForConfigurationAndScheduling(ResourceKey resourceKey)
        throws ResourceRepositoryException {
        if (resourceKey == null || !configurationInfos.containsKey(resourceKey)) {
            return false;
        }
        ConfigurationInfo config = configurationInfos.get(resourceKey);
        if (isTopOfHierarchy(config)) {
            return true;
        }
        ResourceKey parentResourceKey = resourceKey.getParentResourceKey();
        if (configurationInfos.containsKey(parentResourceKey) &&
                    isResourceReadyForConfigurationAndScheduling(parentResourceKey)) {
            return true;
        }
        return false;
    }

    public boolean isResourceExists(ResourceKey resourceKey) {
        if (resourceKey == null) {
            return false;
        }
        return configurationInfos.containsKey(resourceKey);
    }

    /**
     * Return a flat map of resource configuration & properties merged with its ancestors configuration & properties.
     * 
     * @param resourceKey
     * @return a flat map of resource and resource ancestors configuration & properties
     * @throws ResourceRepositoryException
     */
    public Map<String, String> getFullResourceConfiguration(ResourceKey resourceKey)
        throws ResourceRepositoryException {
        return getResourceAndAncestorsConfigurationAndProperties(resourceKey, false);
    }

    /**
     * Return a flat map of resource configuration merged with its ancestors configuration.
     * 
     * @param resourceKey
     * @return a flat map of resource and resource ancestors configuration
     * @throws ResourceRepositoryException in case hierarchy is not complete
     */
    public Map<String, String> getResourceAndAncestorsConfiguration(ResourceKey resourceKey)
        throws ResourceRepositoryException {
        return getResourceAndAncestorsConfigurationAndProperties(resourceKey, true);
    }

    /**
     * Get resources properties and configuration, parent properties put first, and then child properties, so in case of
     * overlapping property name the child property will overwrite the parent property.
     * 
     * @param resourceKey
     * @param filterProperties
     * @return
     * @throws ResourceRepositoryException
     */
    private Map<String, String> getResourceAndAncestorsConfigurationAndProperties(ResourceKey resourceKey,
                                                                                  boolean filterProperties)
        throws ResourceRepositoryException {
        List<ConfigurationInfo> resourceHierarchy = getResourceAncestors(resourceKey);

        if (CollectionUtils.isEmpty(resourceHierarchy)) {
            return Collections.emptyMap();
        }

        Map<String, String> configMap = new HashMap<String, String>();
        for (ConfigurationInfo configInfo : resourceHierarchy) {
            configMap.putAll(configInfo.getConfiguration());
            configMap.putAll(configInfo.getSecuredConfiguration());
            if (filterProperties) {
                configMap.putAll(getFilteredResourceProperties(configInfo));
            } else {
                configMap.putAll(configInfo.getProperties());
            }
        }

        return configMap;
    }

    /**
     * Return a given resource complete ancestors hierarchy. The hierarchy includes the resource itself.
     * 
     * @param resourceKey - the resource in question
     * @return a list of all the given resource ancestors includin itself
     * @throws ResourceRepositoryException
     */
    public List<ConfigurationInfo> getResourceAncestors(ResourceKey resourceKey)
        throws ResourceRepositoryException {
        List<ConfigurationInfo> aggregated = new ArrayList<ConfigurationInfo>();
        aggregated.addAll(getResourceAncestorsHelper(resourceKey, 1));
        return aggregated;
    }

    /**
     * Resource order is important, it's down in the hierarchy, first parent then children.
     * 
     * @param resourceKey
     * @param depth
     * @return
     * @throws ResourceRepositoryException
     */
    private List<ConfigurationInfo> getResourceAncestorsHelper(ResourceKey resourceKey,
                                                               int depth)
        throws ResourceRepositoryException {

        ConfigurationInfo current = configurationInfos.get(resourceKey);
        if (current == null) {
            return Collections.emptyList();
        }
        List<ConfigurationInfo> aggregated = new ArrayList<ConfigurationInfo>();
        if (!(depth == MAX_HIERARCHY_DEPTH || isTopOfHierarchy(current))) {
            aggregated.addAll(getResourceAncestorsHelper(current.getResourceKey().getParentResourceKey(), depth + 1));
        }
        aggregated.add(current);
        return aggregated;
    }

    private Map<String, String> getFilteredResourceProperties(ConfigurationInfo configuration)
        throws ResourceRepositoryException {
        int type = configuration.getTypeInfo().getType();
        switch (type) {
            case TypeInfo.TYPE_PLATFORM:
                return filterPlatformProperties(configuration.getProperties());
            default:
                return Collections.emptyMap();
        }
    }

    private Map<String, String> filterPlatformProperties(Map<String, String> properties)
        throws ResourceRepositoryException {
        Map<String, String> filteredProperties = new HashMap<String, String>();
        for (PlatformKeyConfigPrefix propertyKey : PlatformKeyConfigPrefix.values()) {// PLATFORM_PROPERTIES
            String propertyValue = properties.get(propertyKey.getPrefixedKey());
            if (propertyValue != null) {
                filteredProperties.put(propertyKey.getPrefixedKey(), propertyValue);
            } else {
                if (PlatformKeyConfigPrefix.IP.equals(propertyKey)) {
                    // TODO: remove when system plugin supports ipv6
                    continue;
                }
                log.error("The following property is missing for platform resource: " + propertyKey.getPrefixedKey());
                throw new ResourceRepositoryException(ResourceRepositoryError.MISSING_RESOURCE_PROPERTIES);
            }
        }
        return filteredProperties;
    }

    /**
     * Returns a given resource complete descendants hierarchy.
     * 
     * @param resourceKey - the resource in question
     * @return a list of all the given resource descendants
     * @throws ResourceRepositoryException
     */
    public List<ConfigurationInfo> getResourceDescendents(ResourceKey resourceKey) {
        List<ConfigurationInfo> aggregated = new ArrayList<ConfigurationInfo>();
        aggregated.addAll(getResourceDescendentsHelper(resourceKey, 1));
        return aggregated;
    }

    @SuppressWarnings("unchecked")
    private List<ConfigurationInfo> getResourceDescendentsHelper(ResourceKey resourceKey,
                                                                 int depth) {

        if (resourceKey == null) {
            return Collections.emptyList();
        }
        Collection<ResourceKey> descendentsKeys = resourceRelationships.getCollection(resourceKey);
        if (CollectionUtils.isEmpty(descendentsKeys)) {
            return Collections.emptyList();
        }
        List<ConfigurationInfo> aggregated = new ArrayList<ConfigurationInfo>();

        for (ResourceKey key : descendentsKeys) {
            if (configurationInfos.containsKey(key)) {
                aggregated.add(configurationInfos.get(key));
                if (depth <= MAX_HIERARCHY_DEPTH) {
                    aggregated.addAll(getResourceDescendentsHelper(key, depth + 1));
                }
            }
        }
        return aggregated;
    }

    private boolean isTopOfHierarchy(ConfigurationInfo config)
        throws ResourceRepositoryException {
        TypeInfo typeInfo = config.getTypeInfo();
        if (typeInfo == null) {
            throw new ResourceRepositoryException(ResourceRepositoryError.MISSING_RESOURCE_TYPE_INFO);
        }

        return (TypeInfo.TYPE_PLATFORM == typeInfo.getType());
    }

    public void removeResource(ResourceKey resourceKey) {
        if (resourceKey != null) {
            ResourceKey parentResoucrKey = resourceKey.getParentResourceKey();
            if (resourceRelationships.getCollection(parentResoucrKey) != null) {
                resourceRelationships.getCollection(parentResoucrKey).remove(resourceKey);
            }
            configurationInfos.remove(resourceKey);
        }
    }

    /**
     * Returns a configuration info with for a given resource internal id. If no such id, return null.
     * 
     * @param internalId - the resource internal id of desired configuration info.
     * @return ConfigurationInfo of the associated internal id, or null if not exists.
     */
    public ConfigurationInfo getConfigInfoByInternalId(int internalId) {
        for (Entry<ResourceKey, ConfigurationInfo> entry : configurationInfos.entrySet()) {
            if (internalId == entry.getValue().getResourceInternalId()) {
                ResourceKey resourceKey = entry.getKey();
                ConfigurationInfo oldConfigInfo = configurationInfos.get(resourceKey);
                ConfigurationInfo configInfo = new ConfigurationInfo();
                configInfo.setResourceKey(resourceKey);
                configInfo.setResourceInternalId(internalId);
                configInfo.setTypeInfo(oldConfigInfo.getTypeInfo());
                configInfo.setVirtualServerConfiguration(oldConfigInfo.getVirtualServerConfiguration());
                configInfo.setResourceKind(oldConfigInfo.getResourceKind());
                return configInfo;
            }
        }
        return null;
    }
}
