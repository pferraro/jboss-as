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

package org.wildfly.clustering.server.singleton;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;

/**
 * @author Paul Ferraro
 */
public class LocalSingletonServiceBuilder<T> implements SingletonServiceBuilder<T>, Singleton, Service<T> {

    private final ValueDependency<Group> group;
    private final ServiceName name;
    private final Service<T> service;

    public LocalSingletonServiceBuilder(LocalSingletonServiceBuilderContext context, ServiceName name, Service<T> service) {
        this.group = context.getGroupDependency();
        this.name = name;
        this.service = service;
    }

    @Override
    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
        // Quorum requirements are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy policy) {
        // Election policies are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> backupService(Service<T> backupService) {
        // A backup service will never run on a local singleton
        return this;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        return this.group.register(target.addService(this.name, this));
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public T getValue() {
        return this.service.getValue();
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.service.start(context);
    }

    @Override
    public void stop(StopContext context) {
        this.service.stop(context);
    }

    @Override
    public boolean isPrimary() {
        return true;
    }

    @Override
    public Set<Node> getProviders() {
        return Collections.singleton(this.group.getValue().getLocalNode());
    }

    @Override
    public Optional<Node> getPrimaryProvider() {
        return Optional.of(this.group.getValue().getLocalNode());
    }
}
