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

package com.vmware.epops.webapp.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.epops.plugin.PluginLoaderImpl;
import com.vmware.epops.plugin.model.PluginResourceType;

@Controller
@RequestMapping("/")
public class EpopsServlet {

    private final static Logger logger = LoggerFactory
                .getLogger(EpopsServlet.class);

    @Autowired
    private PluginLoaderImpl pluginLoaderImpl;

    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public @ResponseBody
    PluginResourceType[] getModel(@RequestBody List<String> activePluginFileNames)
        throws ServletException, IOException {
        if (CollectionUtils.isEmpty(activePluginFileNames)) {
            return new PluginResourceType[0];
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getModel() called for the following active plugins: {}", activePluginFileNames);
        }
        Collection<PluginResourceType> model = new ArrayList<>();
        try {
            long startTime = System.currentTimeMillis();
            model = pluginLoaderImpl.getModel(activePluginFileNames);
            long time = System.currentTimeMillis() - startTime;
            String duration = DurationFormatUtils.formatDuration(time, "HH:mm:ss:SS");
            logger.info("getModel() took:{}", duration);
        } catch (Exception e) {
            logger.error("Fail to load the epops plugins model", e);
        }
        PluginResourceType[] modelArray = new PluginResourceType[model.size()];
        return model.toArray(modelArray);
    }

}
