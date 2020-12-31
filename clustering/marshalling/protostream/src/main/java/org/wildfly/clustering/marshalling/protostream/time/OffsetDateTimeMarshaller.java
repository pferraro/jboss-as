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

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshaller for {@link OffsetDateTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal duration since epoch</li>
 * <li>Marshal zone offset</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum OffsetDateTimeMarshaller implements ProtoStreamMarshaller<OffsetDateTime> {
    INSTANCE;

    @Override
    public OffsetDateTime readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Duration duration = Duration.ZERO;
        ZoneOffset offset = ZoneOffset.UTC;
        int tag = reader.readTag();
        while (tag != 0) {
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case 1:
                case 2:
                case 3:
                case 4:
                    duration = DurationMarshaller.readFrom(duration, reader, index - 1);
                    break;
                case 5:
                case 6:
                case 7:
                    offset = ZoneOffsetMarshaller.readFrom(reader, index - 5);
                    break;
                default:
                    reader.skipField(tag);
            }
            tag = reader.readTag();
        }
        Instant instant = Instant.ofEpochSecond(duration.getSeconds(), duration.getNano());
        return OffsetDateTime.ofInstant(instant, offset);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, OffsetDateTime value) throws IOException {
        Duration duration = Duration.ofSeconds(value.toEpochSecond(), value.getNano());
        DurationMarshaller.writeTo(duration, writer, 1);
        ZoneOffset offset = value.getOffset();
        ZoneOffsetMarshaller.writeTo(offset, writer, 5);
    }

    @Override
    public Class<? extends OffsetDateTime> getJavaClass() {
        return OffsetDateTime.class;
    }
}
