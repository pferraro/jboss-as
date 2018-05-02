/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate5.infinispan;

import java.util.Properties;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;

/**
 * Infinispan-backed region factory that retrieves its cache manager from the Infinispan subsystem.
 * This is used for (JPA) container managed persistence contexts.
 * Each deployment application will use a unique Hibernate cache region name in the shared cache.
 *
 * @author Paul Ferraro
 * @author Scott Marlow
 */
@Deprecated
public class SharedInfinispanRegionFactory extends org.infinispan.hibernate.cache.commons.InfinispanRegionFactory {
    private static final long serialVersionUID = -3277051412715973863L;

    @Override
    public void start(SessionFactoryOptions settings, Properties properties) throws CacheException {
        properties.setProperty("hibernate.cache.infinispan.shared", "true");
        super.start(settings, properties);
    }
}
