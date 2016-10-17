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

package org.wildfly.clustering.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

import org.jboss.as.clustering.controller.BinaryRequirementAliasBuilder;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.wildfly.clustering.spi.CacheAliasBuilderProvider;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class CacheRequirementAliasBuilderProvider implements CacheAliasBuilderProvider {

    private final ClusteringCacheRequirement requirement;
    private final BiFunction<String, String, JndiName> jndiNameFactory;

    protected CacheRequirementAliasBuilderProvider(ClusteringCacheRequirement requirement) {
        this(requirement, null);
    }

    protected CacheRequirementAliasBuilderProvider(ClusteringCacheRequirement requirement, BiFunction<String, String, JndiName> jndiNameFactory) {
        this.requirement = requirement;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getBuilders(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String aliasCacheName, String targetCacheName) {
        CapabilityServiceBuilder<?> builder = new BinaryRequirementAliasBuilder<>(registry.getServiceName(this.requirement), this.requirement, containerName, targetCacheName, this.requirement.getType());
        if ((this.jndiNameFactory == null) || JndiNameFactory.DEFAULT_LOCAL_NAME.equals(targetCacheName)) {
            return Collections.singleton(builder);
        }
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(containerName, aliasCacheName).getAbsoluteName());
        CapabilityServiceBuilder<?> binderBuilder = new BinderServiceBuilder<>(binding, builder.getServiceName(), this.requirement.getType());
        return Arrays.asList(builder, binderBuilder);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
