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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.decrypt.Key;
import nl.cad.tpsparse.tps.header.AbstractHeader;
import nl.cad.tpsparse.tps.header.DataHeader;
import nl.cad.tpsparse.tps.header.IndexHeader;
import nl.cad.tpsparse.tps.header.MemoHeader;
import nl.cad.tpsparse.tps.header.MetadataHeader;
import nl.cad.tpsparse.tps.header.TableDefinitionHeader;
import nl.cad.tpsparse.tps.header.TableNameHeader;
import nl.cad.tpsparse.tps.record.DataRecord;
import nl.cad.tpsparse.tps.record.IndexRecord;
import nl.cad.tpsparse.tps.record.MemoRecord;
import nl.cad.tpsparse.tps.record.TableDefinitionRecord;
import nl.cad.tpsparse.tps.record.TableNameRecord;
import nl.cad.tpsparse.util.Utils;

/**
 * Main entry point for parsing the TPS File.
 * 
 * Constructs from file, stream and byte arrays. Typical usage consists of
 * retrieving first the table definitions and then retrieving the records and
 * memos.
 * 
 * The TPS File consists of a TpsHeader section (the first 0x200 bytes) followed
 * by a number of TpsBlocks. These blocks are referenced in the TpsHeader. Each
 * TpsBlock consists of a number of TpsPages. I have not been able to find any
 * meta-data that points to the start of the pages. The current algorithm works
 * by beginning at the block start, parsing the TpsPage and then seeking for the
 * next TpsPage using its offset in the file (always at a 0x0100 boundary and
 * the value at that address must have the same value as the offset). Far from
 * perfect but it seems to work.
 * 
 * A TpsPage consists of a number of TpsRecords which hold the actual data. The
 * Page itself is used to group and compress (using Run Length Encoding) the
 * TpsRecords.
 * 
 * There are a number of records types each defined by their header. The
 * following record types are implemented: - TableDefinition, holding the table
 * meta data, such as columns. - Memo, holding the data of a single memo field.
 * - DataRecords, holding the data of a single row.
 * 
 * @author E.Hooijmeijer
 */
public class TpsFile {

    /**
     * used to traverse the Block,Page,Record hierarchy.
     */
    public interface Visitor {
        /**
         * called for each record.
         * @param record the record.
         */
        void onTpsRecord(TpsRecord record);
    }

    /**
     * used to traverse Block, Page and Record hierarchy in full detail.
     */
    public interface DetailVisitor {

        void onStartBlock(TpsBlock block);

        void onStartPage(TpsPage page);

        void onTpsRecord(TpsBlock block, TpsPage page, TpsRecord record);
    }

    private RandomAccess read;
    private Charset stringEncoding = Charset.forName("ISO-8859-1");

    /**
     * constructs a new TpsFile from the given file.
     * @param file the file.
     * @throws IOException when reading the file fails.
     */
    public TpsFile(File file) throws IOException {
        this(Utils.readFully(file));
    }

    /**
     * constructs a new TpsFile from the given file.
     * @param file the file.
     * @param owner the owner id, also known as the password.
     * @throws IOException when reading the file fails.
     */
    public TpsFile(File file, String owner) throws IOException {
        this(Utils.readFully(file), owner);
    }

    /**
     * constructs a new TpsFile from the given inputstream.
     * @param in the inputstream.
     * @throws IOException when reading the file fails.
     */
    public TpsFile(InputStream in) throws IOException {
        this(Utils.readFully(in));
    }

    /**
     * constructs a new TpsFile from the given inputstream.
     * @param in the inputstream.
     * @param owner the owner id, also known as the password.
     * @throws IOException when reading the file fails.
     */
    public TpsFile(InputStream in, String owner) throws IOException {
        this(Utils.readFully(in), owner);
    }

    /**
     * constructs a new TpsFile from the given byte array.
     * @param data the byte array.
     */
    public TpsFile(byte[] data) {
        this(new RandomAccess(data));
    }

    /**
     * constructs a new TpsFile from the given byte array.
     * @param owner the owner id, also known as the password.
     * @param data the byte array.
     */
    public TpsFile(byte[] data, String owner) {
        Key key = new Key(owner).init();
        key.decrypt(data, 0, 0x200);
        this.read = new RandomAccess(data);
        TpsHeader hdr = getHeader();
        for (int t = 0; t < hdr.getPageStart().length; t++) {
            int ofs = hdr.getPageStart()[t];
            int end = hdr.getPageEnd()[t];
            //
            if (((ofs == 0x0200) && (end == 0x200)) || (ofs >= read.length())) {
                continue;
            } else {
                key.decrypt(data, ofs, end - ofs);
            }
        }
    }

    /**
     * constructs a new TpsFile from the given RandomAccess.
     * @param read the RandomAccess.
     */
    public TpsFile(RandomAccess read) {
        this.read = read;
    }

    /**
     * changes the string encoding of the strings in the TPS file. Default is
     * ISO-8859-1 but others are sometimes used like CP850.
     * @param stringEncoding the string encoding used inside the TPS file.
     */
    public void setStringEncoding(Charset stringEncoding) {
        this.stringEncoding = stringEncoding;
    }

    /**
     * reads the header.
     * @return the header.
     */
    public TpsHeader getHeader() {
        read.jumpAbs(0);
        TpsHeader tpsHeader = new TpsHeader(read);
        if (!tpsHeader.isTopSpeedFile()) {
            throw new NotATopSpeedFileException("Not a TopSpeedFile (" + tpsHeader.getTopSpeed() + ")");
        }
        return tpsHeader;
    }

    /**
     * scans through the file returning all TpsBlocks.
     * @param ignoreErrors ignores any parse errors in the record (risky!).
     * @return the TpsBlocks in the file.
     */
    public List<TpsBlock> getTpsBlocks(boolean ignoreErrors) {
        TpsHeader hdr = getHeader();
        List<TpsBlock> results = new ArrayList<TpsBlock>();
        for (int t = 0; t < hdr.getPageStart().length; t++) {
            int ofs = hdr.getPageStart()[t];
            int end = hdr.getPageEnd()[t];
            // Skips the first entry (0 length) and any blocks that are beyond
            // the file size.
            if (((ofs == 0x0200) && (end == 0x200)) || (ofs >= read.length())) {
                continue;
            } else {
                results.add(new TpsBlock(read, ofs, end, ignoreErrors));
            }
        }
        return results;
    }

    /**
     * visits all the TpsRecords in the file by traversing the Block, Page and
     * Record hierarchy.
     * @param v the visitor.
     */
    public void visit(Visitor v) {
        visit(v, false);
    }

    /**
     * visits all the TpsRecords in the file by traversing the Block, Page and
     * Record hierarchy.
     * @param v the visitor.
     * @param ignoreErrors ignores any page parse errors (at your own peril!).
     */
    public void visit(Visitor v, boolean ignoreErrors) {
        for (TpsBlock data : this.getTpsBlocks(ignoreErrors)) {
            for (TpsPage page : data.getPages()) {
                page.parseRecords();
                for (TpsRecord record : page.getRecords()) {
                    v.onTpsRecord(record);
                }
                page.flush();
            }
        }
    }

    /**
     * visits the hierarchy with callbacks for each type.
     * @param dv the detail visitor.
     */
    public void visitDetails(DetailVisitor dv) {
        for (TpsBlock data : this.getTpsBlocks(false)) {
            dv.onStartBlock(data);
            for (TpsPage page : data.getPages()) {
                dv.onStartPage(page);
                page.parseRecords();
                for (TpsRecord record : page.getRecords()) {
                    dv.onTpsRecord(data, page, record);
                }
                page.flush();
            }
        }
    }

    /**
     * retrieves all data records for the given table and table definition.
     * @param table the table number.
     * @param def the table definition.
     * @param ignoreErrors skips pages with errors.
     * @return a list of records.
     */
    public List<DataRecord> getDataRecords(final int table, final TableDefinitionRecord def, boolean ignoreErrors) {
        final List<DataRecord> results = new ArrayList<DataRecord>();
        visit(new Visitor() {
            @Override
            public void onTpsRecord(TpsRecord record) {
                if (record.getHeader() instanceof DataHeader) {
                    if (record.getHeader().getTableNumber() == table) {
                        results.add(new DataRecord(record, def));
                    }
                }
            }
        }, ignoreErrors);
        return results;
    }

    /**
     * @return all table name records.
     */
    public List<TableNameRecord> getTableNameRecords() {
        final List<TableNameRecord> results = new ArrayList<TableNameRecord>();
        visit(new Visitor() {
            @Override
            public void onTpsRecord(TpsRecord record) {
                if (record.getHeader() instanceof TableNameHeader) {
                    results.add(new TableNameRecord(record));
                }
            }
        });
        return results;
    }

    /**
     * finds all index entries.
     * @param table the table.
     * @param index the index.
     * @return all index records for the given table and index.
     */
    public List<IndexRecord> getIndexes(final int table, final int index) {
        final List<IndexRecord> results = new ArrayList<IndexRecord>();
        visit(new Visitor() {
            @Override
            public void onTpsRecord(TpsRecord record) {
                if (record.getHeader() instanceof IndexHeader) {
                    IndexHeader idxHdr = (IndexHeader) record.getHeader();
                    if (idxHdr.getTableNumber() == table && (idxHdr.getIndexNumber() == index || index == -1)) {
                        results.add(new IndexRecord(record));
                    }
                }
            }
        });
        return results;
    }

    /**
     * builds an list of records ids for the given table and index.
     * @param table the table.
     * @param index the index.
     * @return the list of ids.
     */
    public List<Integer> getIndexRecordIds(int table, int index) {
        List<IndexRecord> idx = getIndexes(table, index);
        List<Integer> results = new ArrayList<Integer>();
        for (IndexRecord i : idx) {
            results.add(i.getRecordNumber());
        }
        return results;
    }

    /**
     * retrieves all metadata records for the given table.
     * @param table the table.
     * @return the records.
     */
    public List<TpsRecord> getMetadata(final int table) {
        final List<TpsRecord> results = new ArrayList<TpsRecord>();
        visit(new Visitor() {
            @Override
            public void onTpsRecord(TpsRecord record) {
                if (record.getHeader() instanceof MetadataHeader) {
                    MetadataHeader idxHdr = (MetadataHeader) record.getHeader();
                    if (idxHdr.getTableNumber() == table) {
                        results.add(record);
                    }
                }
            }
        });
        return results;
    }

    /**
     * @return all TpsRecords in the file.
     */
    public List<TpsRecord> getAllRecords() {
        final List<TpsRecord> results = new ArrayList<TpsRecord>();
        visit(new Visitor() {
            @Override
            public void onTpsRecord(TpsRecord record) {
                results.add(record);
            }
        });
        return results;
    }

    /**
     * retrieves all memo records for a given table and memo field. Long Memo
     * fields are not stored in one record, they are spread out among multiple.
     * So here we need to group and join them together.
     * @param tableNr the table number.
     * @param memoIdx the memo index (as a table may have multiple memo fields,
     * zero based).
     * @param ignoreErrors ignores any page parse errors.
     * @return the memo records.
     */
    public List<MemoRecord> getMemoRecords(final int tableNr, final int memoIdx, boolean ignoreErrors) {
        final Map<Integer, List<TpsRecord>> memoGroups = new TreeMap<Integer, List<TpsRecord>>();
        this.visit(new Visitor() {
            @Override
            public void onTpsRecord(TpsRecord record) {
                if (record.getHeader() instanceof MemoHeader) {
                    MemoHeader hdr = (MemoHeader) record.getHeader();
                    if (hdr.isApplicable(tableNr, memoIdx)) {
                        if (memoGroups.get(hdr.getOwningRecord()) == null) {
                            memoGroups.put(Integer.valueOf(hdr.getOwningRecord()), new ArrayList<TpsRecord>());
                        }
                        while (memoGroups.get(hdr.getOwningRecord()).size() <= hdr.getSequenceNr()) {
                            memoGroups.get(hdr.getOwningRecord()).add(null);
                        }
                        memoGroups.get(hdr.getOwningRecord()).set(hdr.getSequenceNr(), record);
                    }
                }
            }
        }, ignoreErrors);
        List<MemoRecord> memos = new ArrayList<MemoRecord>();
        for (Map.Entry<Integer, List<TpsRecord>> memoGroup : memoGroups.entrySet()) {
            if (isComplete(memoGroup.getValue())) {
                AbstractHeader header = memoGroup.getValue().get(0).getHeader();
                memos.add(new MemoRecord(header, merge(memoGroup.getValue())));
            }
        }
        return memos;
    }

    /**
     * checks if the record set is complete.
     * @param values the the record set.
     * @return true if all records are there.
     */
    private boolean isComplete(List<TpsRecord> values) {
        for (TpsRecord value : values) {
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * retrieves all table definitions in the TpsFile. For each table (there can
     * be more than one, although one is most common) the table definition
     * records are grouped together (as they're spread over multiple records)
     * and then merged. From that the actual table definition is built.
     * @param ignoreErrors ignores any errors.
     * @return the table definitions.
     */
    public Map<Integer, TableDefinitionRecord> getTableDefinitions(final boolean ignoreErrors) {
        final Map<Integer, List<TpsRecord>> tableDefs = new TreeMap<Integer, List<TpsRecord>>();
        this.visit(new Visitor() {
            @Override
            public void onTpsRecord(TpsRecord record) {
                if (record.getHeader() instanceof TableDefinitionHeader) {
                    int table = record.getHeader().getTableNumber();
                    int index = ((TableDefinitionHeader) record.getHeader()).getBlock();
                    if (tableDefs.get(table) == null) {
                        tableDefs.put(Integer.valueOf(table), new ArrayList<TpsRecord>());
                    }
                    while (tableDefs.get(table).size() <= index) {
                        tableDefs.get(table).add(null);
                    }
                    tableDefs.get(table).set(index, record);
                }
            }
        }, ignoreErrors);
        Map<Integer, TableDefinitionRecord> tables = new TreeMap<Integer, TableDefinitionRecord>();
        for (Map.Entry<Integer, List<TpsRecord>> table : tableDefs.entrySet()) {
            if (isComplete(table.getValue())) {
                tables.put(table.getKey(), new TableDefinitionRecord(merge(table.getValue()), stringEncoding));
            }
        }
        return tables;
    }

    /**
     * merges the payload of a number of TpsRecords into one.
     * @param records the records.
     * @return the merged records.
     */
    private RandomAccess merge(List<TpsRecord> records) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (TpsRecord rec : records) {
            try {
                out.write(rec.getData().remainder());
            } catch (IOException ex) {
                throw new RuntimeException("Error while merging TpsRecords", ex);
            }
        }
        return new RandomAccess(out.toByteArray());
    }

    public byte[] getBytes() {
        return read.data();
    }

}
