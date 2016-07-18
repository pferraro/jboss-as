/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Command dispatcher context for singleton services.
 * @author Paul Ferraro
 */
public interface SingletonContext<T> {

    /**
     * Starts the primary service on this node.
     */
    void start();

    /**
     * Stops the primary service on this node.
     */
    void stop();

    /**
     * Returns the value of this service.
     * @return a reference to the service value.
     */
    AtomicReference<T> getValueRef();

    /**
     * Indicates whether or not this node is the primary provider of the associated singleton service.
     * @return true, if this node is primary, or false otherwise.
     */
    boolean isPrimary();
}
