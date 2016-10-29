/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.UnaryRequirementAliasBuilder;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.GroupJndiNameFactory;
import org.wildfly.clustering.server.GroupRequirementAliasBuilderProvider;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.GroupAliasBuilderProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(GroupAliasBuilderProvider.class)
public class GroupRegistryFactoryAliasBuilderProvider extends GroupRequirementAliasBuilderProvider {

    public GroupRegistryFactoryAliasBuilderProvider() {
        super(ClusteringRequirement.REGISTRY_FACTORY, GroupJndiNameFactory.REGISTRY_FACTORY);
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getBuilders(ServiceNameRegistry<ClusteringRequirement> registry, String aliasName, String targetName) {
        Collection<CapabilityServiceBuilder<?>> builders = super.getBuilders(registry, aliasName, targetName);
        List<CapabilityServiceBuilder<?>> result = new ArrayList<>(builders.size() + 2);
        result.addAll(builders);
        result.add(new UnaryRequirementAliasBuilder<>(registry.getServiceName(ClusteringRequirement.REGISTRY), ClusteringRequirement.REGISTRY, targetName, ClusteringRequirement.REGISTRY.getType()));
        result.add(new UnaryRequirementAliasBuilder<>(registry.getServiceName(ClusteringRequirement.REGISTRY_ENTRY), ClusteringRequirement.REGISTRY_ENTRY, targetName, ClusteringRequirement.REGISTRY_ENTRY.getType()));
        return result;
    }
}
