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

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Writes properly escaped and encoded CSV files.
 * @author E.Hooijmeijer
 */
public abstract class CsvWriter {

    private StringBuilder line = new StringBuilder();

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
     * begins a new row. checks if the row has the expected amount of columns.
     */
    public void newRow() {
        if (columns != currentColumn) {
            throw new IllegalArgumentException("Missing column " + columns + " != " + currentColumn);
        }
        line.append(System.getProperty("line.separator"));
        addRow(line.toString());
        line.delete(0, line.length());
        currentColumn = 0;
    }

    protected abstract void addRow(String row);

    /**
     * adds a single value to the csv, quotes and escapes it if needed.
     * @param value the value.
     */
    private void addValue(Object value) {
        if (!ignoreColumn.get(currentColumn)) {
            String str = toString(value);
            if (currentColumn != 0) {
                line.append(sep);
            }
            if (str != null) {
                if ((str.indexOf(sep) >= 0) || (str.indexOf('\n') >= 0) || value instanceof String) {
                    line.append(quot);
                    for (int t = 0; t < str.length(); t++) {
                        line.append(str.charAt(t));
                        // escape quotes by doubling them.
                        if (str.charAt(t) == quot) {
                            line.append(quot);
                        }
                    }
                    line.append(quot);
                } else {
                    line.append(str);
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
        } else {
            return String.valueOf(value);
        }
    }

}
