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
package nl.cad.tpsparse;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nl.cad.tpsparse.csv.CsvFile;
import nl.cad.tpsparse.csv.CsvReader;
import nl.cad.tpsparse.csv.CsvWriter;
import nl.cad.tpsparse.tps.NotATopSpeedFileException;
import nl.cad.tpsparse.tps.TpsBlock;
import nl.cad.tpsparse.tps.TpsFile;
import nl.cad.tpsparse.tps.TpsFile.DetailVisitor;
import nl.cad.tpsparse.tps.TpsPage;
import nl.cad.tpsparse.tps.TpsRecord;
import nl.cad.tpsparse.tps.record.DataRecord;
import nl.cad.tpsparse.tps.record.FieldDefinitionRecord;
import nl.cad.tpsparse.tps.record.IndexDefinitionRecord;
import nl.cad.tpsparse.tps.record.MemoDefinitionRecord;
import nl.cad.tpsparse.tps.record.MemoRecord;
import nl.cad.tpsparse.tps.record.TableDefinitionRecord;

import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.FileConverter;

/**
 * Converts TPS files into CSV files. Also displays various information on a TPS
 * file.
 * @author E.Hooijmeijer
 */
public class Main {

    /**
     * JCommander style commandline parameters.
     */
    public static class Args {
        @Parameter(names = "-s", description = "source TPS file or folder containing TPS files.", converter = FileConverter.class, required = false)
        private File sourceFile;
        @Parameter(names = "-i", description = "displays TPS file information.")
        private boolean info;
        @Parameter(names = "-idx", description = "displays the record ids for the available indexes.")
        private boolean index;
        @Parameter(names = "-sort", description = "sorts the records on their row number. Otherwise you'll get the sequence as they are in the file.")
        private boolean sort;
        @Parameter(names = "-layout", description = "displays the file layout.")
        private boolean layout;
        @Parameter(names = "-e", description = "shows stacktraces.")
        private boolean stackTraces;
        @Parameter(names = "-t", description = "target CSV file or folder to create CSV files in.", converter = FileConverter.class, required = false)
        private File targetFile;
        @Parameter(names = "-sep", description = "separator character, used to separate fields. Use two hex digits for non standard chars (09=tab).", required = false, converter = CharConverter.class)
        private char separator = ',';
        @Parameter(names = "-quot", description = "quote character, used to quote field values. Use two hex digits for non standard chars.", required = false, converter = CharConverter.class)
        private char quoteCharacter = '\"';
        @Parameter(names = "-ignoreErrors", description = "ignores errors, parsing only the pages that are readable (data is lost!)", required = false)
        private boolean ignoreErrors;
        @Parameter(names = { "-?", "-help", "--help" }, description = "displays help and usage information.", required = false)
        private boolean help;
        @Parameter(names = { "-encoding" }, description = "CSV output encoding.", required = false)
        private String encoding = "ISO-8859-1";
        @Parameter(names = { "-compare" }, description = "Compare Output File to existing Csv", required = false, converter = FileConverter.class)
        private File compareToFile;
        @Parameter(names = { "-raw" }, description = "Don't attempt any character encoding, output the bytes as is.")
        private boolean raw = false;
        @Parameter(names = { "-owner", "-password" }, description = "specify the owner/password for the tps file.")
        private String password;
    }

    public static void main(String[] args) {
        Args params = new Args();
        JCommander cmd = new JCommander(params);
        try {
            cmd.parse(args);
            if (params.help || params.sourceFile == null) {
                System.out.println("TPS-to-CSV : converts Clarion TPS files to CSV.");
                System.out.println("(C) 2012-2013 E.Hooijmeijer, Apache 2 licensed (https://www.apache.org/licenses/LICENSE-2.0.html)\n");
                System.out.println("WARNING : This software is based on Reverse Engineered TPS Files.");
                System.out.println("          As such, its probably incomplete and may mis-interpret data.");
                System.out.println("          It is no replacement for any existing Clarion tooling.");
                System.out.println("          Check the output files thoroughly before proceeding.\n");
                System.out.println("Commercial Clarion tooling is available at http://www.softvelocity.com/\n");
                System.out.println("Typical use:");
                System.out.println(" java -jar tps-to-csv.jar -s [source file or folder] -t [target file or folder] -sort -raw\n");
                cmd.usage();
            } else {
                if (params.sourceFile.isFile()) {
                    parseFile(params);
                } else {
                    if (params.targetFile != null && params.targetFile.isFile()) {
                        throw new ParameterException("If the source is a folder, the target must also be a folder.");
                    }
                    File[] files = listFiles(params.sourceFile);
                    File targetBase = params.targetFile;
                    for (File file : files) {
                        params.sourceFile = file;
                        params.targetFile = (targetBase == null ? null : new File(targetBase, file.getName() + ".csv"));
                        parseFile(params);
                    }
                }
            }
            //
        } catch (IOException ex) {
            System.out.println("Error reading TPS file: " + ex.getMessage());
        } catch (ParameterException ex) {
            System.out.println(ex.getMessage());
            cmd.usage();
        }
    }

    /**
     * @param folder the folder to scan.
     * @return the tps files in the folder.
     */
    private static File[] listFiles(File folder) {
        return folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".tps");
            }
        });
    }

    /**
     * processes a single file.
     * @param args the commandline arguments.
     * @throws IOException if reading/writing fails.
     */
    private static void parseFile(Args args) throws IOException {
        TpsFile tpsFile = openFile(args);
        //
        try {
            Map<Integer, TableDefinitionRecord> tableDefinitions = tpsFile.getTableDefinitions(args.ignoreErrors);
            //
            if (args.info) {
                info(args.sourceFile, tableDefinitions);
            }
            //
            if (args.index) {
                index(tpsFile, tableDefinitions);
            }
            //
            if (args.layout) {
                layout(tpsFile);
            }
            //
            if (args.targetFile != null) {
                //
                for (Map.Entry<Integer, TableDefinitionRecord> table : tableDefinitions.entrySet()) {
                    //
                    CsvWriter csv = new CsvWriter(args.separator, args.quoteCharacter);
                    TableDefinitionRecord def = table.getValue();
                    csv.addColumn("Rec No");
                    for (FieldDefinitionRecord field : def.getFields()) {
                        String csvName = toCsvName(getFieldPrefix(def.getFields(), field) + field.getFieldNameNoTable());
                        if (field.isArray()) {
                            for (int idx = 0; idx < field.getNrOfElements(); idx++) {
                                csv.addColumn(csvName + "[" + idx + "]", field.isGroup());
                            }
                        } else {
                            csv.addColumn(csvName, field.isGroup());
                        }
                    }
                    for (MemoDefinitionRecord memo : def.getMemos()) {
                        csv.addColumn(toCsvName(memo.getName()));
                    }
                    csv.newRow();
                    //
                    // Prefetch memo's.
                    //
                    List<List<MemoRecord>> memos = new ArrayList<List<MemoRecord>>();
                    for (int t = 0; t < table.getValue().getMemos().size(); t++) {
                        memos.add(tpsFile.getMemoRecords(table.getKey(), t, args.ignoreErrors));
                    }
                    //
                    Map<Integer, DataRecord> recordsById = new TreeMap<Integer, DataRecord>();
                    //
                    for (DataRecord rec : tpsFile.getDataRecords(table.getKey(), table.getValue(), args.ignoreErrors)) {
                        int recordNumber = rec.getRecordNumber();
                        if (!recordsById.containsKey(recordNumber)) {
                            recordsById.put(recordNumber, rec);
                        } else {
                            System.err.println(args.sourceFile.getName() + ": Duplicate record " + recordNumber);
                        }
                    }
                    //
                    if (args.sort) {
                        for (Map.Entry<Integer, DataRecord> entry : recordsById.entrySet()) {
                            DataRecord rec = entry.getValue();
                            onRecord(table, csv, memos, rec);
                        }
                    } else {
                        for (DataRecord rec : tpsFile.getDataRecords(table.getKey(), table.getValue(), args.ignoreErrors)) {
                            onRecord(table, csv, memos, rec);
                        }
                    }
                    //
                    if (tableDefinitions.size() == 1) {
                        if (args.raw) {
                            csv.writeRaw(args.targetFile);
                        } else {
                            csv.writeToFile(args.targetFile, args.encoding);
                        }
                    } else {
                        File parentFile = args.targetFile.getParentFile();
                        String name = args.targetFile.getName();
                        File target = new File(parentFile, name.substring(0, name.lastIndexOf('.')) + "." + table.getKey() + ".csv");
                        if (args.raw) {
                            csv.writeRaw(target);
                        } else {
                            csv.writeToFile(target, args.encoding);
                        }
                    }
                    //
                    if ((args.compareToFile != null) && (tableDefinitions.size() == 1)) {
                        System.out.println("Diff of " + args.targetFile + " v.s " + args.compareToFile);
                        CsvFile generated = new CsvReader(args.separator, args.quoteCharacter).read(args.targetFile, args.encoding);
                        CsvFile compareTo = new CsvReader(args.separator, args.quoteCharacter).read(args.compareToFile, args.encoding);
                        compareCsv(generated, compareTo);
                    }
                    //
                }
            }
        } catch (Exception ex) {
            System.err.println(args.sourceFile.getName() + " : " + ex.getMessage());
            if (args.stackTraces) {
                ex.printStackTrace();
            }
        }
    }

    private static TpsFile openFile(Args args) throws IOException {
        try {
            TpsFile tpsFile = new TpsFile(args.sourceFile);
            tpsFile.getHeader();
            return tpsFile;
        } catch (NotATopSpeedFileException ex) {
            if (!StringUtils.isEmpty(args.password)) {
                System.out.println("Encrypted file, using set password.");
                return new TpsFile(args.sourceFile, args.password);
            } else {
                throw ex;
            }
        }
    }

    /**
     * compares two csv files.
     * @param generated the generated file.
     * @param compareTo the file to compare to.
     */
    private static void compareCsv(CsvFile generated, CsvFile compareTo) {
        assertEquals("Number of header fields", compareTo.getHeader().length, generated.getHeader().length);
        for (int t = 0; t < compareTo.getHeader().length; t++) {
            assertEquals("Header field.", compareTo.getHeader()[t], generated.getHeader()[t]);
        }
        assertEquals("Number of rows", compareTo.getRows().size(), generated.getRows().size());
        List<String> spurious = findMissingIds(generated, compareTo);
        if (!spurious.isEmpty()) {
            System.err.println("Spurious Row Ids : " + spurious);
        }
        List<String> missing = findMissingIds(compareTo, generated);
        if (!missing.isEmpty()) {
            System.err.println("Missing Row Ids : " + missing);
        }
        int genRow = 0;
        int cmpRow = 0;
        do {
            String[] row = generated.getRows().get(genRow);
            String[] cmp = compareTo.getRows().get(cmpRow);
            //
            while (spurious.contains(row[0].trim()) && cmpRow < generated.getRows().size() - 1) {
                System.err.println("Skipping generated row " + genRow + " as its spurious.");
                genRow++;
                row = generated.getRows().get(genRow);
            }
            while (missing.contains(cmp[0].trim()) && cmpRow < compareTo.getRows().size() - 1) {
                System.err.println("Skipping compare row " + cmpRow + " as its missing.");
                cmpRow++;
                cmp = compareTo.getRows().get(cmpRow);
            }
            //
            if ((genRow < generated.getRows().size() && cmpRow < compareTo.getRows().size())) {
                assertEquals("Row " + genRow + " number of cells", cmp.length, row.length);
                for (int y = 0; y < row.length; y++) {
                    assertSloppyEquals("Row G:" + genRow + "==C:" + cmpRow + ", Col " + y + " (" + compareTo.getHeader()[y] + ") equals", cmp[y], row[y]);
                }
            }
            genRow++;
            cmpRow++;
        } while (genRow < generated.getRows().size() && cmpRow < compareTo.getRows().size());
    }

    /**
     * scans for missing ids.
     * @param generated the generated file.
     * @param compareTo the file to compare to.
     * @return a list of missing ids.
     */
    private static List<String> findMissingIds(CsvFile generated, CsvFile compareTo) {
        List<String> sb = new ArrayList<String>();
        for (int t = 0; t < generated.getRows().size(); t++) {
            boolean found = false;
            String rid = generated.getRows().get(t)[0].trim();
            for (int y = 0; y < compareTo.getRows().size(); y++) {
                String cmp = compareTo.getRows().get(y)[0].trim();
                if (rid.equals(cmp)) {
                    found = true;
                }
            }
            if (!found) {
                sb.add(rid);
            }
        }
        return sb;
    }

    private static void assertSloppyEquals(String msg, String expect, String got) {
        String sloppyExpect = toSloppy(expect);
        String sloppyGot = toSloppy(got);
        assertEquals(msg, sloppyExpect, sloppyGot);
    }

    private static String toSloppy(String expect) {
        expect = expect.trim();
        boolean isInt = true;
        try {
            Integer.parseInt(expect);
        } catch (NumberFormatException ex) {
            isInt = false;
        }
        if ((expect.indexOf('.') > 0) || (expect.indexOf(',') > 0) || isInt) {
            try {
                double d = Double.parseDouble(expect.replace(",", ""));
                return Double.toString(d);
            } catch (NumberFormatException ex) {
            }
        }
        return expect;
    }

    private static void assertEquals(String msg, int expect, int got) {
        if (expect != got) {
            System.err.println(msg + ". expected " + expect + " got " + got);
        }
    }

    private static void assertEquals(String msg, String expect, String got) {
        if (!expect.equals(got)) {
            System.err.println(msg + ". expected '" + expect + "' got '" + got + "'");
            if (expect.length() > 128) {
                assertEquals(" lengths differ: ", expect.length(), got.length());
                int max = Math.min(expect.length(), got.length());
                for (int t = 0; t < max; t++) {
                    if (expect.charAt(t) != got.charAt(t)) {
                        System.err.println(" First difference at position " + t + " char 0x" + Integer.toHexString(expect.charAt(t)) + " != 0x"
                                + Integer.toHexString(got.charAt(t)));
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param fields all fields.
     * @param field the field to get the prefix for.
     * @return the field prefix if the field is in a group.
     */
    private static String getFieldPrefix(List<FieldDefinitionRecord> fields, FieldDefinitionRecord field) {
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
     * @param name the name.
     * @return the csv name.
     */
    private static String toCsvName(String name) {
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

    private static void onRecord(Map.Entry<Integer, TableDefinitionRecord> table, CsvWriter csv, List<List<MemoRecord>> memos, DataRecord rec) {
        int recordNumber = rec.getRecordNumber();
        csv.addCell(recordNumber);
        List<FieldDefinitionRecord> fields = table.getValue().getFields();
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
        for (int t = 0; t < table.getValue().getMemos().size(); t++) {
            MemoDefinitionRecord def = table.getValue().getMemos().get(t);
            List<MemoRecord> list = memos.get(t);
            boolean found = false;
            for (MemoRecord memo : list) {
                if (memo.getOwner() == recordNumber) {
                    if (def.isMemo()) {
                        csv.addCell(memo.getDataAsMemo());
                    } else {
                        String fileName = recordNumber + "-" + t + ".bin";
                        csv.addCell(fileName);
                        writeFile(fileName, memo.getDataAsBlob());
                    }
                    found = true;
                }
            }
            if (!found) {
                csv.addCell("");
            }
        }
        csv.newRow();
    }

    private static void writeFile(String fileName, byte[] data) {
        File file = new File(fileName);
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

    private static void index(TpsFile tps, Map<Integer, TableDefinitionRecord> tableDefinitions) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, TableDefinitionRecord> table : tableDefinitions.entrySet()) {
            TableDefinitionRecord def = table.getValue();
            for (int t = 0; t < def.getIndexes().size(); t++) {
                IndexDefinitionRecord field = def.getIndexes().get(t);
                sb.append(field.getName() + " : ");
                //
                boolean first = false;
                for (Integer idx : tps.getIndexRecordIds(table.getKey(), t)) {
                    if (first) {
                        sb.append(", ");
                    } else {
                        first = true;
                    }
                    sb.append(idx);
                }
                //
                sb.append("\n");
            }
        }
        System.out.println(sb.toString());
    }

    private static void info(File sourceFile, Map<Integer, TableDefinitionRecord> tableDefinitions) {
        StringBuilder sb = new StringBuilder();
        String type = "";
        sb.append(sourceFile.getName() + " : contains " + tableDefinitions.size() + " table(s).\n");
        for (Map.Entry<Integer, TableDefinitionRecord> table : tableDefinitions.entrySet()) {
            TableDefinitionRecord def = table.getValue();
            sb.append("Table " + table.getKey() + " : " + def.getFields().size() + " Fields, " + def.getIndexes().size() + " Indexes, " + def.getMemos().size()
                    + " Memos, " + def.getRecordLength() + " bytes per row, driver version " + def.getDriverVersion() + ".\n");
            for (int t = 0; t < def.getFields().size(); t++) {
                FieldDefinitionRecord field = def.getFields().get(t);
                if (field.isArray()) {
                    type = " array[" + field.getNrOfElements() + "] of " + field.getFieldTypeName();
                } else {
                    type = " of type " + field.getFieldTypeName();
                }
                sb.append("Field '" + field.getFieldName() + "'" + type + " at offset " + field.getOffset() + ", " + field.getLength() + " bytes\n");
            }
            for (int t = 0; t < def.getIndexes().size(); t++) {
                IndexDefinitionRecord field = def.getIndexes().get(t);
                sb.append("Index '" + field.getName() + "' on " + field.getFieldsInKey() + " fields \n");
                for (FieldDefinitionRecord keyField : field.getFieldRecords(table.getValue())) {
                    sb.append("  " + keyField.getFieldName() + "\n");
                }
            }
            for (int t = 0; t < def.getMemos().size(); t++) {
                MemoDefinitionRecord field = def.getMemos().get(t);
                sb.append("Memo  '" + field.getName() + "' with flags " + field.getFlags() + " \n");
            }
        }
        System.out.println(sb.toString());
    }

    private static void layout(TpsFile tpsFile) {
        tpsFile.visitDetails(new DetailVisitor() {

            @Override
            public void onStartBlock(TpsBlock block) {
                System.out.println(block);
            }

            @Override
            public void onStartPage(TpsPage page) {
                System.out.println("  " + page);

            }

            @Override
            public void onTpsRecord(TpsBlock block, TpsPage page, TpsRecord record) {
                System.out.println("    " + record);
            }
        });
    }
}
