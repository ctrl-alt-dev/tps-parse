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

import java.util.ArrayList;
import java.util.List;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.bin.RunLengthEncodingException;

/**
 * The TpsPage holds TpsRecords, which may be compressed using Run Length Encoding (RLE).
 * 
 * There is no proper flag that signals if the page is compressed.
 * Currently the page is thought of as compressed when the pageSize is different from
 * the uncompressedSize <i>and</i> the flags are 0x00. Some records with the flag set
 * to a non zero value also have different lengths, but the data is not compressed..
 * 
 * Building the TpsRecords out of the TpsPage uses also a custom compression algorithm.
 * As most headers of the TpsRecords are identical, some bytes can be saved by reusing
 * the headers of previous records. The first byte of each TpsRecord indicates what
 * parts should be reused. Obviously the first record on a page should have a full
 * header (0xC0).
 * 
 * @author E.Hooijmeijer
 */
public class TpsPage {

    private int addr;
    private int pageSize;
    private int pageSizeUncompressed;
    private int pageSizeUncompressedWithoutHeader;
    private int recordCount;
    private int flags;

    private RandomAccess compressedData;
    private RandomAccess data;
    private List<TpsRecord> records = new ArrayList<TpsRecord>();

    public TpsPage(RandomAccess rx) {
        addr = rx.leLong();
        pageSize = rx.leShort();
        RandomAccess header = rx.read(pageSize - 6);
        pageSizeUncompressed = header.leShort();
        pageSizeUncompressedWithoutHeader = header.leShort();
        recordCount = header.leShort();
        flags = header.leByte();
        //
        compressedData = header.read(pageSize - 13);
    }

    protected void uncompress() {
        if ((pageSize != pageSizeUncompressed) && (flags == 0)) {
            try {
                compressedData.pushPosition();
                data = compressedData.deRle(compressedData);
            } catch (Exception ex) {
                throw new RunLengthEncodingException("Bad RLE DataBlock at index " + compressedData + " in " + this, ex);
            } finally {
                compressedData.popPosition();
            }
        } else {
            data = compressedData;
        }
    }

    protected RandomAccess getData() {
        if (isFlushed()) {
            uncompress();
        }
        return data;
    }

    public void flush() {
        data = null;
        records.clear();
    }

    protected boolean isFlushed() {
        return data == null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        RandomAccess data = new RandomAccess(new byte[0]);
        sb.append("TpsPage(" + data.toHex8(addr) + "," + data.toHex4(pageSize) + "," + data.toHex4(pageSizeUncompressed) + ","
                + data.toHex4(pageSizeUncompressedWithoutHeader) + "," + data.toHex4(recordCount) + "," + data.toHex2(flags) + ")");
        return sb.toString();
    }

    public int getAddr() {
        return addr;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageSizeUncompressed() {
        return pageSizeUncompressed;
    }

    public int getPageSizeUncompressedWithoutHeader() {
        return pageSizeUncompressedWithoutHeader;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public int getFlags() {
        return flags;
    }

    public RandomAccess getUncompressedData() {
        return getData();
    }

    /**
     * (re)parses all TpsRecords in the page.
     */
    public void parseRecords() {
        if (isFlushed()) {
            uncompress();
        }
        records.clear();
        // Skip pages with non 0x00 flags as they don't seem to contain TpsRecords.
        if (flags == 0x00) {
            data.pushPosition();
            try {
                TpsRecord prev = null;
                do {
                    TpsRecord current = null;
                    if (prev == null) {
                        current = new TpsRecord(data);
                    } else {
                        current = new TpsRecord(prev, data);
                    }
                    records.add(current);
                    prev = current;
                } while (!data.isAtEnd() && records.size() < recordCount);
            } finally {
                data.popPosition();
            }
        }
    }

    public List<TpsRecord> getRecords() {
        if (isFlushed()) {
            parseRecords();
        }
        return records;
    }
}
