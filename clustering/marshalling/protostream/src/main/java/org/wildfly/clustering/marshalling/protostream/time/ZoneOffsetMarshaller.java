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
import java.time.ZoneOffset;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshalling for {@link ZoneOffset} instances using the following strategy:
 * <ol>
 * <li>Marshal {@link ZoneOffset#UTC} as zero bytes</li>
 * <li>If offset is of form &plusmn;HH, marshal as signed integer of hours</li>
 * <li>If offset is of form &plusmn;HH:MM, marshal as signed integer of total minutes<li>
 * <li>If offset is of form &plusmn;HH:MM:SS, marshal as signed integer of total seconds<li>
 * </ol>
 * @author Paul Ferraro
 */
public enum ZoneOffsetMarshaller implements ProtoStreamMarshaller<ZoneOffset> {
    INSTANCE;

    private static final int HOURS = 0;
    private static final int MINUTES = 1;
    private static final int SECONDS = 2;

    static ZoneOffset readFrom(RawProtoStreamReader reader, int index) throws IOException {
        switch (index) {
            case HOURS:
                return ZoneOffset.ofHours(reader.readSInt32());
            case MINUTES:
                return ZoneOffset.ofTotalSeconds(reader.readSInt32() * 60);
            case SECONDS:
                return ZoneOffset.ofTotalSeconds(reader.readSInt32());
            default:
                return ZoneOffset.UTC;
        }
    }

    static void writeTo(ZoneOffset offset, RawProtoStreamWriter writer, int index) throws IOException {
        int seconds = offset.getTotalSeconds();
        if (seconds != 0) {
            if (seconds % 60 == 0) {
                int minutes = seconds / 60;
                if (minutes % 60 == 0) {
                    int hours = minutes / 60;
                    // Typical offsets
                    writer.writeSInt32(index + HOURS, hours);
                } else {
                    // Uncommon fractional hour offsets
                    writer.writeSInt32(index + MINUTES, minutes);
                }
            } else {
                // Synthetic offsets
                writer.writeSInt32(index + SECONDS, seconds);
            }
        }
    }

    @Override
    public ZoneOffset readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        ZoneOffset offset = ZoneOffset.UTC;
        int tag = reader.readTag();
        while (tag != 0) {
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case 1:
                case 2:
                case 3:
                    offset = readFrom(reader, index - 1);
                    break;
                default:
                    reader.skipField(tag);
            }
            tag = reader.readTag();
        }
        return offset;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, ZoneOffset offset) throws IOException {
        writeTo(offset, writer, 1);
    }

    @Override
    public Class<? extends ZoneOffset> getJavaClass() {
        return ZoneOffset.class;
    }
}
