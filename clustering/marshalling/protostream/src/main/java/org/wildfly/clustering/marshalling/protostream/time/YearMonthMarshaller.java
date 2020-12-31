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
import java.time.Instant;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshaller for {@link Period} instances, using the following strategy:
 * <ol>
 * <li>Marshal number of years since the epoch as an unsigned integer</li>
 * <li>Marshal month - 1 as unsigned integer</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum YearMonthMarshaller implements ProtoStreamMarshaller<YearMonth> {
    INSTANCE;

    private static final int POSITIVE_EPOCH_YEAR = 1;
    private static final int NEGATIVE_EPOCH_YEAR = 2;
    private static final int MONTH = 3;

    @Override
    public YearMonth readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        int epochYear = 0;
        int month = 0;
        int tag = reader.readTag();
        while (tag != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case POSITIVE_EPOCH_YEAR:
                    epochYear = reader.readUInt32();
                    break;
                case NEGATIVE_EPOCH_YEAR:
                    epochYear = 0 - reader.readUInt32();
                    break;
                case MONTH:
                    month = reader.readUInt32();
                    break;
                default:
                    reader.skipField(tag);
            }
            tag = reader.readTag();
        }
        return YearMonth.of(epochYear + Instant.EPOCH.atOffset(ZoneOffset.UTC).getYear(), month + 1);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, YearMonth value) throws IOException {
        int epochYear = value.getYear() - Instant.EPOCH.atOffset(ZoneOffset.UTC).getYear();
        if (epochYear != 0) {
            if (epochYear > 0) {
                writer.writeUInt32(POSITIVE_EPOCH_YEAR, epochYear);
            } else {
                writer.writeUInt32(NEGATIVE_EPOCH_YEAR, 0 - epochYear);
            }
        }
        int month = value.getMonthValue() - 1;
        if (month > 0) {
            writer.writeUInt32(MONTH, month);
        }
    }

    @Override
    public Class<? extends YearMonth> getJavaClass() {
        return YearMonth.class;
    }
}
