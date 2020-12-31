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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshaller for {@link Duration} instances, using the following strategy:
 * <ol>
 * <li>Marshal {@link Duration#ZERO} as zero bytes</li>
 * <li>Marshal number of seconds of duration as unsigned long</li>
 * <li>Marshal sub-second value of duration as unsigned integer, using millisecond precision, if possible</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum DurationMarshaller implements ProtoStreamMarshaller<Duration> {
    INSTANCE;

    private static final int POSITIVE_SECONDS = 0;
    private static final int NEGATIVE_SECONDS = 1;
    private static final int MILLIS = 2;
    private static final int NANOS = 3;

    static Duration readFrom(Duration duration, RawProtoStreamReader reader, int index) throws IOException {
        switch (index) {
            case POSITIVE_SECONDS:
                return duration.withSeconds(reader.readUInt64());
            case NEGATIVE_SECONDS:
                return duration.withSeconds(0 - reader.readUInt64());
            case MILLIS:
                return duration.withNanos(reader.readUInt32() * 1_000_000);
            case NANOS:
                return duration.withNanos(reader.readUInt32());
            default:
                return duration;
        }
    }

    static void writeTo(Duration duration, RawProtoStreamWriter writer, int index) throws IOException {
        long seconds = duration.getSeconds();
        if (seconds != 0) {
            // Optimize for positive values
            if (seconds > 0) {
                writer.writeUInt64(index + POSITIVE_SECONDS, seconds);
            } else {
                writer.writeUInt64(index + NEGATIVE_SECONDS, 0 - seconds);
            }
        }
        int nanos = duration.getNano();
        if (nanos > 0) {
            // Optimize for ms precision, if possible
            if (nanos % 1_000_000 == 0) {
                writer.writeUInt32(index + MILLIS, nanos / 1_000_000);
            } else {
                writer.writeUInt32(index + NANOS, nanos);
            }
        }
    }

    @Override
    public Duration readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Duration duration = Duration.ZERO;
        int tag = reader.readTag();
        while (tag != 0) {
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case 1:
                case 2:
                case 3:
                case 4:
                    duration = readFrom(duration, reader, index - 1);
                    break;
                default:
                    reader.skipField(tag);
            }
            tag = reader.readTag();
        }
        return duration;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Duration duration) throws IOException {
        writeTo(duration, writer, 1);
    }

    @Override
    public Class<? extends Duration> getJavaClass() {
        return Duration.class;
    }
}
