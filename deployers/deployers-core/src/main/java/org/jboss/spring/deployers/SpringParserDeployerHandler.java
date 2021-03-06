/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.spring.deployers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.spring.vfs.VFSUtil;

/**
 * Utility class that implements the deployer behaviour that is common to VFS2 and VFS3 in a
 * manner that is agnostic of the VFS version.
 * <p/>
 * VFS-version specific deployers will delegate to this class for the actual work
 *
 * @author Marius Bogoevici
 */
public class SpringParserDeployerHandler {

    boolean useLegacyDefaultName;

    public SpringParserDeployerHandler() {
    }

    /**
     * Get the use name flag.
     *
     * @return true if the default name should be determined from deployment unit
     */
    protected boolean isUseLegacyDefaultName() {
        return useLegacyDefaultName;
    }

    /**
     * Should we use deployment unit's name as default.
     * e.g. using string before .jar|spring|... as the name
     * <p/>
     * Previous versions used string before .spring as the name,
     * setting this to true results in this legacy behaviour.
     * <p/>
     * Current default is string before -spring.xml.
     *
     * @param useLegacyDefaultName the flag
     */
    public void setUseLegacyDefaultName(boolean useLegacyDefaultName) {
        this.useLegacyDefaultName = useLegacyDefaultName;
    }

    /**
     * Get default name from meta file.
     *
     * @param file the virtual file
     * @return default name
     */
    String getDefaultNameForFile(Object file) throws IOException {
        String shortName = VFSUtil.invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_GET_NAME, file);
        int p = shortName.indexOf("-spring.xml");
        return shortName.substring(0, p);
    }

    /**
     * Get default name from unit.
     *
     * @param unit the deployment unit
     * @return default name
     */
    String getDefaultNameForUnit(DeploymentUnit unit) {
        String shortName = unit.getSimpleName();
        int p = shortName.lastIndexOf(".");
        return shortName.substring(0, p);
    }

    public SpringMetaData parse(DeploymentUnit unit, Object file) throws IOException, URISyntaxException {
        String defaultName;
        if (isUseLegacyDefaultName()) {
            defaultName = getDefaultNameForUnit(unit);
        } else {
            defaultName = getDefaultNameForFile(file);
        }

        return new SpringMetaData(Collections.singletonList(createSpringContextDescriptor(file, defaultName)));
    }


    private SpringContextDescriptor createSpringContextDescriptor(Object file, String defaultName) throws IOException {
        return new SpringContextDescriptor(VFSUtil.<URL>invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_TO_URL, file), defaultName);
    }

    public SpringMetaData handleMultipleFiles(List<?> files) throws IOException {
        List<SpringContextDescriptor> descriptors = new ArrayList<SpringContextDescriptor>();
        for (Object virtualFile : files) {
            String defaultName;
            if (isUseLegacyDefaultName()) {
                throw new IllegalStateException("Cannot use the legacy default name for multiple Spring configuration files");
            } else {
                defaultName = getDefaultNameForFile(virtualFile);
            }
            descriptors.add(new SpringContextDescriptor(VFSUtil.<URL>invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_TO_URL, virtualFile), defaultName));
        }
        return new SpringMetaData(descriptors);
    }
}