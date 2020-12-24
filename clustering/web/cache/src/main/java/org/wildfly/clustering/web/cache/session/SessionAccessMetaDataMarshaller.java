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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * @author Paul Ferraro
 */
public enum SessionAccessMetaDataMarshaller implements ProtoStreamMarshaller<SimpleSessionAccessMetaData> {
    INSTANCE;

    // Optimize for sub-second request duration
    private static final Duration DEFAULT_LAST_ACCESS = Duration.ofSeconds(1);

    @Override
    public SimpleSessionAccessMetaData readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Duration sinceCreation = Duration.ZERO;
        Duration lastAccess = DEFAULT_LAST_ACCESS;
        int tag = reader.readTag();
        while (tag != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    sinceCreation = ProtobufUtil.fromByteBuffer(context, reader.readByteBuffer(), Duration.class);
                    break;
                case 2:
                    lastAccess = ProtobufUtil.fromByteBuffer(context, reader.readByteBuffer(), Duration.class);
                    break;
                default:
                    reader.skipField(tag);
            }
            tag = reader.readTag();
        }
        SimpleSessionAccessMetaData metaData = new SimpleSessionAccessMetaData();
        metaData.setLastAccessDuration(sinceCreation, lastAccess);
        return metaData;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, SimpleSessionAccessMetaData metaData) throws IOException {
        Duration sinceCreation = metaData.getSinceCreationDuration();
        if (!sinceCreation.isZero()) {
            writer.writeBytes(1, ProtobufUtil.toByteBuffer(context, sinceCreation));
        }
        Duration lastAccess = metaData.getLastAccessDuration();
        if (!lastAccess.equals(DEFAULT_LAST_ACCESS)) {
            writer.writeBytes(2, ProtobufUtil.toByteBuffer(context, lastAccess));
        }
    }

    @Override
    public Class<? extends SimpleSessionAccessMetaData> getJavaClass() {
        return SimpleSessionAccessMetaData.class;
    }
}
