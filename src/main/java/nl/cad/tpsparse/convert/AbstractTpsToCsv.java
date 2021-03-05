/*
 *  Copyright 2013 E.Hooijmeijer
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
package nl.cad.tpsparse.convert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.cad.tpsparse.csv.CsvWriter;
import nl.cad.tpsparse.tps.TpsFile;
import nl.cad.tpsparse.tps.record.DataRecord;
import nl.cad.tpsparse.tps.record.FieldDefinitionRecord;
import nl.cad.tpsparse.tps.record.MemoDefinitionRecord;
import nl.cad.tpsparse.tps.record.MemoRecord;
import nl.cad.tpsparse.tps.record.TableDefinitionRecord;
import nl.cad.tpsparse.util.Utils;

/**
 * @author E.Hooijmeijer
 */
public abstract class AbstractTpsToCsv {

    private CsvWriter csv;
    private TableDefinitionRecord table;
    private Integer tableId;
    private File sourceFile;
    private File targetFile;
    private TpsFile tpsFile;

    private boolean ignoreErrors;
    private boolean verbose;

    private int recordCount;

    /**
     * 
     */
    public AbstractTpsToCsv(File tpsFile, File csvFile, CsvWriter csv, TpsFile tps, Map.Entry<Integer, TableDefinitionRecord> table) {
        this.sourceFile = tpsFile;
        this.targetFile = csvFile;
        this.csv = csv;
        this.tpsFile = tps;
        this.tableId = table.getKey();
        this.table = table.getValue();

    }

    public abstract void run();

    protected TpsFile getTpsFile() {
        return tpsFile;
    }

    protected TableDefinitionRecord getTable() {
        return table;
    }

    protected Integer getTableId() {
        return tableId;
    }

    protected File getSourceFile() {
        return sourceFile;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    protected void buildCsvHeaders() {
        csv.addColumn("Rec No");
        for (FieldDefinitionRecord field : table.getFields()) {
            String csvName = toCsvName(getFieldPrefix(table.getFields(), field) + field.getFieldNameNoTable());
            if (field.isArray()) {
                for (int idx = 0; idx < field.getNrOfElements(); idx++) {
                    csv.addColumn(csvName + "[" + idx + "]", field.isGroup());
                }
            } else {
                csv.addColumn(csvName, field.isGroup());
            }
        }
        for (MemoDefinitionRecord memo : table.getMemos()) {
            csv.addColumn(toCsvName(memo.getName()));
        }
        csv.newRow();
    }

    /**
     * @param fields all fields.
     * @param field the field to get the prefix for.
     * @return the field prefix if the field is in a group.
     */
    protected String getFieldPrefix(List<FieldDefinitionRecord> fields, FieldDefinitionRecord field) {
        for (FieldDefinitionRecord f : fields) {
            if (f.isGroup()) {
                if (field.isInGroup(f)) {
                    return f.getFieldNameNoTable() + ".";
                }
            }
        }
        return "";
    }

    /**
     * converts a tps name to a more readable csv name.
     * @param name the name.
     * @return the csv name.
     */
    protected String toCsvName(String name) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int t = 0; t < name.length(); t++) {
            char c = name.charAt(t);
            if (first) {
                sb.append(Character.toUpperCase(c));
                first = false;
            } else {
                if (c == '_') {
                    sb.append(' ');
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
            if (c == '_') {
                first = true;
            }
        }
        return sb.toString();
    }

    protected List<Map<Integer, MemoRecord>> prefetchMemos() {
        if (verbose) {
            System.out.println("Prefetching Memo's");
        }
        List<Map<Integer, MemoRecord>> memos = new ArrayList<>();
        for (int t = 0; t < table.getMemos().size(); t++) {
            memos.add(tpsFile.getMemoRecords(tableId, t, ignoreErrors));
        }
        if (verbose) {
            System.out.println("Memory: " + Utils.reportMemoryUsage());
        }
        return memos;
    }

    /**
     * handles a single record.
     * @param table the table.
     * @param csv the csv to write to.
     * @param memos the preloaded memo's
     * @param rec the data record to read from.
     */
    protected void onRecord(List<Map<Integer, MemoRecord>> memos, DataRecord rec) {
        int recordNumber = rec.getRecordNumber();
        csv.addCell(recordNumber);
        List<FieldDefinitionRecord> fields = table.getFields();
        List<Object> values = rec.getValues();
        for (int t = 0; t < values.size(); t++) {
            FieldDefinitionRecord field = fields.get(t);
            Object value = values.get(t);
            if (field.isArray()) {
                Object[] arr = (Object[]) value;
                for (int idx = 0; idx < field.getNrOfElements(); idx++) {
                    csv.addCell(arr[idx]);
                }
            } else {
                csv.addCell(value);
            }
        }
        for (int t = 0; t < table.getMemos().size(); t++) {
            MemoDefinitionRecord def = table.getMemos().get(t);
            Map<Integer, MemoRecord> list = memos.get(t);
            MemoRecord memo = list.get(recordNumber);
            if (memo != null) {
                if (def.isMemo()) {
                    csv.addCell(memo.getDataAsMemo());
                } else {
                    String fileName = getBaseFileName() + "-" + recordNumber + "-" + t + ".bin";
                    csv.addCell(fileName);
                    try {
                        writeFile(fileName, memo.getDataAsBlob());
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        if (ignoreErrors) {
                            System.out.println("ERROR : for " + fileName + " : BLOB Length mismatch - saving available bytes (" + ex.getMessage() + ")");
                            writeFile(fileName, memo.getDataAsRaw());
                        } else {
                            throw ex;
                        }
                    }
                }
            } else {
                csv.addCell("");
            }
        }
        csv.newRow();
        //
        recordCount++;
        if (isVerbose() && (recordCount % 1000 == 0)) {
            System.out.println("Processed record #" + recordCount + " (" + Utils.reportMemoryUsage() + ")");
        }
    }

    private String getBaseFileName() {
        String name = targetFile.getName();
        int idx = name.lastIndexOf('.');
        if (idx >= 0) {
            return name.substring(0, idx);
        } else {
            return name;
        }
    }

    /**
     * @param fileName
     * @param dataAsBlob
     */
    protected void writeFile(String fileName, byte[] data) {
        File file = new File(targetFile.getParentFile(), fileName);
        if (file.exists()) {
            throw new IllegalArgumentException("File '" + fileName + "' already exists.");
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Error writing " + fileName, ex);
        }
    }

}
