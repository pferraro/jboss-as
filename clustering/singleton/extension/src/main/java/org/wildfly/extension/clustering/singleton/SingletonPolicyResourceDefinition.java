/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.DefaultableCapabilityReference;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.singleton.SingletonRequirement;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringDefaultCacheRequirement;

/**
 * Definition of a singleton policy resource.
 * @author Paul Ferraro
 */
public class SingletonPolicyResourceDefinition extends ChildResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("singleton-policy", value);
    }

    enum Capability implements CapabilityProvider {
        POLICY(SingletonRequirement.SINGLETON_POLICY),
        DEFAULT_BUILDER("org.wildfly.clustering.singleton.policy.default-builder"),
        BUILDER("org.wildfly.clustering.singleton.policy.builder"),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(String name) {
            this.capability = () -> RuntimeCapability.Builder.of(name, true).build();
        }

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CACHE_CONTAINER("cache-container", ModelType.STRING, new CapabilityReference(Capability.DEFAULT_BUILDER, ClusteringDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY)),
        CACHE("cache", ModelType.STRING, new DefaultableCapabilityReference(Capability.BUILDER, ClusteringCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY, CACHE_CONTAINER)),
        QUORUM("quorum", ModelType.INT, new ModelNode(1)),
        ;
        private final AttributeDefinition definition;

        private Attribute(String name, ModelType type, DefaultableCapabilityReference reference) {
            this.definition = createBuilder(name, type).setAllowNull(true).setCapabilityReference(reference).build();
        }

        private Attribute(String name, ModelType type, CapabilityReference reference) {
            this.definition = createBuilder(name, type).setAllowNull(false).setCapabilityReference(reference).build();
        }

        private Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
            return new SimpleAttributeDefinitionBuilder(name, type).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (SingletonModel.VERSION_2_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attribute.CACHE.getDefinition(), Attribute.CACHE_CONTAINER.getDefinition())
                .end();
        }
    }

    SingletonPolicyResourceDefinition() {
        super(WILDCARD_PATH, new SingletonResourceDescriptionResolver(WILDCARD_PATH));
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> new SingletonPolicyBuilder(address));
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);

        new RandomElectionPolicyResourceDefinition().register(registration);
        new SimpleElectionPolicyResourceDefinition().register(registration);
    }
}
