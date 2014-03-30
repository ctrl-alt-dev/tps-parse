/*
 *  Copyright 2012-2013 E.Hooijmeijer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package nl.cad.tpsparse.tps;

import java.io.ByteArrayOutputStream;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.tps.header.AbstractHeader;
import nl.cad.tpsparse.tps.header.DataHeader;
import nl.cad.tpsparse.tps.header.IndexHeader;
import nl.cad.tpsparse.tps.header.MemoHeader;
import nl.cad.tpsparse.tps.header.MetadataHeader;
import nl.cad.tpsparse.tps.header.TableDefinitionHeader;
import nl.cad.tpsparse.tps.header.TableNameHeader;

/**
 * This is the main data container of a TpsFile.
 * 
 * There are two ways of constructing a TpsRecord:
 * - one without any previous records.
 * - one by copying a part of the previous record, to save some bytes.
 * 
 * There are different kinds of TpsRecords each identified by their Header, which is
 * actually a header in a header. The first byte determines the copy mechanism used,
 * while the following two shorts hold the record length and the real header length.
 * Then there is a table identifier and then the actual record type.
 * 
 * @author E.Hooijmeijer
 */
public class TpsRecord {

    private int flags;
    private int recordLength;
    private int headerLength;
    private RandomAccess data;
    private AbstractHeader header;

    /**
     * constructs a new TpsRecord. This constructor is typically called on the
     * first of a list.
     * @param rx to read the data from.
     */
    public TpsRecord(RandomAccess rx) {
        flags = rx.leByte();
        if ((flags & 0xC0) != 0xC0) {
            throw new IllegalArgumentException("Can't construct a TpsRecord without record lengths (0x" + rx.toHex2(flags) + ")");
        }
        recordLength = rx.leShort();
        headerLength = rx.leShort();
        data = rx.read(recordLength);
        //
        buildHeader();
    }

    /**
     * creates a new TpsRecord by partially copying the previous one.
     * @param previous the previous record.
     * @param rx the data to read from.
     */
    public TpsRecord(TpsRecord previous, RandomAccess rx) {
        flags = rx.leByte();
        if ((flags & 0x80) != 0) {
            recordLength = rx.leShort();
        } else {
            recordLength = previous.getRecordLength();
        }
        if ((flags & 0x40) != 0) {
            headerLength = rx.leShort();
        } else {
            headerLength = previous.getHeaderLength();
        }
        int copy = flags & 0x3F;
        //
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(previous.getData().data(), 0, copy);
            out.write(rx.readBytes(recordLength - copy));
            data = new RandomAccess(out.toByteArray());
            if (data.length() != recordLength) {
                throw new IllegalArgumentException("Data and record length mismatch.");
            }
        } catch (Exception ex) {
            throw new RuntimeException("When reading " + (recordLength - copy) + " bytes of TpsRecord at " + rx);
        }
        //
        buildHeader();
    }

    /**
     * constructs the header for the record by peeking at the type.
     * Most records have their type at the 5th byte, except for the
     * table name, which has it at position 0.
     */
    private void buildHeader() {
        RandomAccess hdr = data.read(headerLength);
        if (hdr.length() >= 5) {
            //
            if ((hdr.peek(0) & 0xFF) == 0xFE) {
                header = new TableNameHeader(hdr);
            } else {
                //
                switch ((int) (hdr.peek(4) & 0xFF)) {
                case 0xF3:
                    header = new DataHeader(hdr);
                    break;
                case 0xF6:
                    header = new MetadataHeader(hdr);
                    break;
                case 0xFA:
                    header = new TableDefinitionHeader(hdr);
                    break;
                case 0xFC:
                    header = new MemoHeader(hdr);
                    break;
                default:
                    header = new IndexHeader(hdr);
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "TpsRecord(L:" + recordLength + ",H:" + headerLength + "," + header + ")";
    }

    public RandomAccess getData() {
        return data;
    }

    public int getFlags() {
        return flags;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    public AbstractHeader getHeader() {
        return header;
    }
}
