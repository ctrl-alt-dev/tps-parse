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
package nl.cad.tpsparse.csv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Writes properly escaped and encoded CSV files.
 * @author E.Hooijmeijer
 */
public class CsvWriter {

    private StringBuilder out = new StringBuilder();

    private char sep;
    private char quot;

    private int columns;
    private int currentColumn;
    private List<Boolean> ignoreColumn = new ArrayList<Boolean>();

    /**
     * creates a new writer.
     * @param sep the separator character.
     * @param quot the quote character.
     */
    public CsvWriter(char sep, char quot) {
        this.sep = sep;
        this.quot = quot;
    }

    /**
     * adds a column. 
     * @param name the name of the column.
     * @param ignore if the column should be ignored in the output.
     */
    public void addColumn(String name, boolean ignore) {
        ignoreColumn.add(ignore);
        addValue(name);
        columns++;
    }

    /**
     * adds a column
     * @param name the name of the column.
     */
    public void addColumn(String name) {
        addColumn(name, false);
    }

    /** 
     * adds a cell value.
     * @param value the value.
     */
    public void addCell(Object value) {
        addValue(value);
    }

    /**
     * begins a new row.
     * checks if the row has the expected amount of columns.
     */
    public void newRow() {
        if (columns != currentColumn) {
            throw new IllegalArgumentException("Missing column " + columns + " != " + currentColumn);
        }
        out.append(System.getProperty("line.separator"));
        currentColumn = 0;
    }

    /**
     * adds a single value to the csv, quotes and escapes it if needed.
     * @param value the value.
     */
    private void addValue(Object value) {
        if (!ignoreColumn.get(currentColumn)) {
            String str = toString(value);
            if (currentColumn != 0) {
                out.append(sep);
            }
            if (str != null) {
                if ((str.indexOf(sep) >= 0) || (str.indexOf('\n') >= 0) || value instanceof String) {
                    out.append(quot);
                    for (int t = 0; t < str.length(); t++) {
                        out.append(str.charAt(t));
                        // escape quotes by doubling them.
                        if (str.charAt(t) == quot) {
                            out.append(quot);
                        }
                    }
                    out.append(quot);
                } else {
                    out.append(str);
                }
            }
        }
        currentColumn++;
    }

    /**
     * converts an object value to string.
     * @param value the object value.
     * @return the value as a string.
     */
    private String toString(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof LocalDate) {
            return ((LocalDate) value).toString("yyyy-MM-dd");
        } else if (value instanceof LocalTime) {
            return ((LocalTime) value).toString("HH:mm");
        } else if (value.getClass().isArray()) {
            return toArrayString((Object[]) value);
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * there is no real place for arrays in CSV, but as it may
     * be stored in TPS we have to do something with it. So
     * we surround it with square brackets and put pipes in between.
     * @param value the array value.
     * @return the string representation of the array.
     */
    private String toArrayString(Object[] value) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int t = 0; t < value.length; t++) {
            if (t > 0) {
                sb.append("|");
            }
            sb.append(toString(value[t]));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        return out.toString();
    }

    /**
     * writes output as raw character bytes.
     * Does not attempt any character encoding.
     * @param target the target file.
     * @throws IOException if writing fails.
     */
    public void writeRaw(File target) throws IOException {
        FileOutputStream w = new FileOutputStream(target);
        try {
            String str = out.toString();
            for (int t = 0; t < str.length(); t++) {
                w.write(str.charAt(t));
            }
        } finally {
            w.close();
        }
    }

    /**
     * writes output encoded according to the given character set. 
     * @param target the target file.
     * @param charset the character set.
     * @throws IOException if writing fails.
     */
    public void writeToFile(File target, String charset) throws IOException {
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), Charset.forName(charset)));
        try {
            wr.write(out.toString());
        } finally {
            wr.close();
        }
    }

    /**
     * writes to file using ISO-8859-1 encoding.
     * @param target
     * @throws IOException
     */
    public void writeToFile(File target) throws IOException {
        writeToFile(target, "ISO-8859-1");
    }
}
