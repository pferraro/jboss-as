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
import java.time.LocalTime;
import java.time.temporal.ChronoField;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshaller for {@link LocalTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal {@link LocalTime#MIDNIGHT} as zero bytes</li>
 * <li>Marshal number of seconds in day as unsigned integer, using hours or minutes precision, if possible</li>
 * <li>Marshal sub-second value of day as unsigned integer, using millisecond precision if possible</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum LocalTimeMarshaller implements ProtoStreamMarshaller<LocalTime> {
    INSTANCE;

    private static final int HOURS_OF_DAY = 0;
    private static final int MINUTES_OF_DAY = 1;
    private static final int SECONDS_OF_DAY = 2;
    private static final int MILLIS = 3;
    private static final int NANOS = 4;

    static LocalTime readFrom(LocalTime time, RawProtoStreamReader reader, int index) throws IOException {
        switch (index) {
            case HOURS_OF_DAY:
                return time.with(ChronoField.HOUR_OF_DAY, reader.readUInt32());
            case MINUTES_OF_DAY:
                return time.with(ChronoField.MINUTE_OF_DAY, reader.readUInt32());
            case SECONDS_OF_DAY:
                return time.with(ChronoField.SECOND_OF_DAY, reader.readUInt32());
            case MILLIS:
                return time.with(ChronoField.MILLI_OF_SECOND, reader.readUInt32());
            case NANOS:
                return time.withNano(reader.readUInt32());
            default:
                return time;
        }
    }

    static void writeTo(LocalTime time, RawProtoStreamWriter writer, int index) throws IOException {
        int secondOfDay = time.toSecondOfDay();
        if (secondOfDay > 0) {
            if (secondOfDay % 60 == 0) {
                int minutesOfDay = secondOfDay / 60;
                if (minutesOfDay % 60 == 0) {
                    int hoursOfDay = minutesOfDay / 60;
                    writer.writeUInt32(index + HOURS_OF_DAY, hoursOfDay);
                } else {
                    writer.writeUInt32(index + MINUTES_OF_DAY, minutesOfDay);
                }
            } else {
                writer.writeUInt32(index + SECONDS_OF_DAY, secondOfDay);
            }
        }
        int nanos = time.getNano();
        if (nanos > 0) {
            // Use ms precision, if possible
            if (nanos % 1_000_000 == 0) {
                writer.writeUInt32(index + MILLIS, time.get(ChronoField.MILLI_OF_SECOND));
            } else {
                writer.writeUInt32(index + NANOS, nanos);
            }
        }
    }

    @Override
    public LocalTime readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        LocalTime time = LocalTime.MIDNIGHT;
        int tag = reader.readTag();
        while (tag != 0) {
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    time = readFrom(time, reader, index - 1);
                    break;
                default:
                    reader.skipField(tag);
            }
            tag = reader.readTag();
        }
        return time;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, LocalTime time) throws IOException {
        writeTo(time, writer, 1);
    }

    @Override
    public Class<? extends LocalTime> getJavaClass() {
        return LocalTime.class;
    }
}
