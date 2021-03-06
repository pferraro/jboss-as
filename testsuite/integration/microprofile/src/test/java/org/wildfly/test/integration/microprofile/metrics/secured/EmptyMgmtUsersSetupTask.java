/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.metrics.secured;

import java.io.File;
import java.nio.file.Files;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

public class EmptyMgmtUsersSetupTask implements ServerSetupTask {

    static File mgmtUsersFile;
    byte[] bytes;

    static {
        String jbossHome = System.getProperty("jboss.home", null);
        if (jbossHome == null) {
            throw new IllegalStateException("jboss.home not set");
        }
        mgmtUsersFile = new File(jbossHome + File.separatorChar + "standalone" + File.separatorChar
                + "configuration" + File.separatorChar + "mgmt-users.properties");

        if (!mgmtUsersFile.exists()) {
            throw new IllegalStateException("Determined mgmt-users.properties path " + mgmtUsersFile + " does not exist");
        }
        if (!mgmtUsersFile.isFile()) {
            throw new IllegalStateException("Determined mgmt-users.properties path " + mgmtUsersFile + " is not a file");
        }
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        bytes = Files.readAllBytes(mgmtUsersFile.toPath());
        Files.write(mgmtUsersFile.toPath(), "".getBytes());
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        Files.write(mgmtUsersFile.toPath(), bytes);
    }
}