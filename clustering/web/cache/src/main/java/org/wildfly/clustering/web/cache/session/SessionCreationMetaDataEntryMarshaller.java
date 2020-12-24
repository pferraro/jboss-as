/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * @author Paul Ferraro
 */
public enum SessionCreationMetaDataEntryMarshaller implements ProtoStreamMarshaller<SessionCreationMetaDataEntry<Object>> {
    INSTANCE;

    // Optimize for specification default
    private static final Duration DEFAULT_MAX_INACTIVE_INTERVAL = Duration.ofMinutes(30L);

    @Override
    public SessionCreationMetaDataEntry<Object> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Instant creationTime = Instant.EPOCH;
        Duration maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL;
        int tag = reader.readTag();
        while (tag != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    creationTime = ProtobufUtil.fromByteBuffer(context, reader.readByteBuffer(), Instant.class);
                    break;
                case 2:
                    maxInactiveInterval = ProtobufUtil.fromByteBuffer(context, reader.readByteBuffer(), Duration.class);
                    break;
                default:
                    reader.skipField(tag);
            }
            tag = reader.readTag();
        }
        SessionCreationMetaData metaData = new SimpleSessionCreationMetaData(creationTime);
        metaData.setMaxInactiveInterval(maxInactiveInterval);
        return new SessionCreationMetaDataEntry<>(metaData);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, SessionCreationMetaDataEntry<Object> entry) throws IOException {
        SessionCreationMetaData metaData = entry.getMetaData();
        writer.writeBytes(1, ProtobufUtil.toByteBuffer(context, metaData.getCreationTime()));

        Duration maxInactiveInterval = metaData.getMaxInactiveInterval();
        if (!maxInactiveInterval.equals(DEFAULT_MAX_INACTIVE_INTERVAL)) {
            writer.writeBytes(2, ProtobufUtil.toByteBuffer(context, maxInactiveInterval));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SessionCreationMetaDataEntry<Object>> getJavaClass() {
        return (Class<SessionCreationMetaDataEntry<Object>>) (Class<?>) SessionCreationMetaDataEntry.class;
    }
}
