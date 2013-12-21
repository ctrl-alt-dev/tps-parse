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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

import nl.cad.tpsparse.convert.AbstractTpsToCsv;
import nl.cad.tpsparse.convert.BufferingTpsToCsv;
import nl.cad.tpsparse.convert.StreamingTpsToCsv;
import nl.cad.tpsparse.csv.BufferingCsvWriter;
import nl.cad.tpsparse.csv.CsvDiff;
import nl.cad.tpsparse.csv.CsvFile;
import nl.cad.tpsparse.csv.CsvReader;
import nl.cad.tpsparse.csv.CsvWriter;
import nl.cad.tpsparse.csv.ImmediateCsvWriter;
import nl.cad.tpsparse.tps.NotATopSpeedFileException;
import nl.cad.tpsparse.tps.TpsBlock;
import nl.cad.tpsparse.tps.TpsFile;
import nl.cad.tpsparse.tps.TpsFile.DetailVisitor;
import nl.cad.tpsparse.tps.TpsPage;
import nl.cad.tpsparse.tps.TpsRecord;
import nl.cad.tpsparse.tps.record.FieldDefinitionRecord;
import nl.cad.tpsparse.tps.record.IndexDefinitionRecord;
import nl.cad.tpsparse.tps.record.MemoDefinitionRecord;
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
        @Parameter(names = { "-tpsEncoding" }, description = "TPS (input) encoding for strings.", required = false)
        private String tpsEncoding = "ISO-8859-1";
        @Parameter(names = { "-compare" }, description = "Compare Output File to existing Csv", required = false, converter = FileConverter.class)
        private File compareToFile;
        @Parameter(names = { "-raw" }, description = "Don't attempt any character encoding, output the bytes as is.")
        private boolean raw = false;
        @Parameter(names = { "-owner", "-password" }, description = "specify the owner/password for the tps file.")
        private String password;
        @Parameter(names = { "-direct" }, description = "writes directly to file without buffering, useful for large files. Doesn't sort.")
        private boolean direct = false;
        @Parameter(names = { "-verbose" }, description = "more verbose output.")
        private boolean verbose = false;
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
        } catch (UnsupportedCharsetException ex) {
            System.out.println("Unknown or unsupported characterset '" + ex.getCharsetName() + "'.");
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
                    CsvWriter csv = openOutputCsvFile(args, tableDefinitions, table);
                    try {
                        AbstractTpsToCsv tpsToCsv = null;
                        if (args.direct) {
                            tpsToCsv = new StreamingTpsToCsv(args.sourceFile, args.targetFile, csv, tpsFile, table);
                        } else {
                            tpsToCsv = new BufferingTpsToCsv(args.sourceFile, args.targetFile, csv, tpsFile, table);
                        }
                        tpsToCsv.setIgnoreErrors(args.ignoreErrors);
                        tpsToCsv.setVerbose(args.verbose);
                        tpsToCsv.run();
                    } finally {
                        finishCsvFile(args, tableDefinitions, table, csv);
                    }
                    //
                    if ((args.compareToFile != null) && (tableDefinitions.size() == 1)) {
                        runDiff(args);
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

    private static void runDiff(Args args) throws IOException {
        System.out.println("Diff of " + args.targetFile + " v.s " + args.compareToFile + " : ");
        CsvFile generated = new CsvReader(args.separator, args.quoteCharacter).read(args.targetFile, args.encoding);
        CsvFile compareTo = new CsvReader(args.separator, args.quoteCharacter).read(args.compareToFile, args.encoding);
        CsvDiff diff = new CsvDiff();
        if (!diff.compareCsv(generated, compareTo)) {
            for (String error : diff.getErrors()) {
                System.err.println(" " + error);
            }
        } else {
            System.out.println(" No (real) differences.");
        }
    }

    private static CsvWriter openOutputCsvFile(Args args, Map<Integer, TableDefinitionRecord> tableDefinitions, Map.Entry<Integer, TableDefinitionRecord> table)
            throws IOException {
        CsvWriter csv = null;
        if (args.direct) {
            if (tableDefinitions.size() == 1) {
                csv = new ImmediateCsvWriter(args.separator, args.quoteCharacter, args.targetFile, args.encoding);
            } else {
                csv = new ImmediateCsvWriter(args.separator, args.quoteCharacter, buildTargetFile(args, table), args.encoding);
            }
        } else {
            csv = new BufferingCsvWriter(args.separator, args.quoteCharacter);
        }
        return csv;
    }

    private static void finishCsvFile(Args args, Map<Integer, TableDefinitionRecord> tableDefinitions, Map.Entry<Integer, TableDefinitionRecord> table,
            CsvWriter csv) throws IOException {
        if (csv instanceof BufferingCsvWriter) {
            if (tableDefinitions.size() == 1) {
                if (args.raw) {
                    ((BufferingCsvWriter) csv).writeRaw(args.targetFile);
                } else {
                    ((BufferingCsvWriter) csv).writeToFile(args.targetFile, args.encoding);
                }
            } else {
                File target = buildTargetFile(args, table);
                if (args.raw) {
                    ((BufferingCsvWriter) csv).writeRaw(target);
                } else {
                    ((BufferingCsvWriter) csv).writeToFile(target, args.encoding);
                }
            }
        } else {
            ((ImmediateCsvWriter) csv).close();
        }
    }

    private static File buildTargetFile(Args args, Map.Entry<Integer, TableDefinitionRecord> table) {
        File parentFile = args.targetFile.getParentFile();
        String name = args.targetFile.getName();
        File target = new File(parentFile, name.substring(0, name.lastIndexOf('.')) + "." + table.getKey() + ".csv");
        return target;
    }

    private static TpsFile openFile(Args args) throws IOException {
        try {
            if (args.verbose) {
                System.out.println("Opening " + args.sourceFile);
            }
            TpsFile tpsFile = new TpsFile(args.sourceFile);
            tpsFile.setStringEncoding(Charset.forName(args.tpsEncoding));
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
