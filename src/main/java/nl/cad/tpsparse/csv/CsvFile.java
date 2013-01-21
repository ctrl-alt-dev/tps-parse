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

/**
 * Provides access to the rows and cells of a csv.
 * @author E.Hooijmeijer
 */
public class CsvFile {

    private int rowCnt;
    private String[] header;
    private List<String[]> rows = new ArrayList<String[]>();

    public void append(String[] fields) {
        if (rowCnt == 0) {
            header = fields;
        } else {
            if (fields.length != header.length) {
                if (header.length - fields.length == 1) {
                    String[] copy = new String[header.length];
                    System.arraycopy(fields, 0, copy, 0, fields.length);
                    copy[fields.length] = "";
                    fields = copy;
                } else {
                    throw new IllegalArgumentException("Expected " + header.length + " fields, got " + fields.length);
                }
            }
            rows.add(fields);
        }
        rowCnt++;
    }

    /**
     * @return the header
     */
    public String[] getHeader() {
        return header;
    }

    /**
     * @return the rowCnt
     */
    public int getRowCnt() {
        return rowCnt;
    }

    /**
     * @return the rows
     */
    public List<String[]> getRows() {
        return rows;
    }

    public List<String[]> getRowsByColumn(String name, String value) {
        List<String[]> results = new ArrayList<String[]>();
        int idx = getColumnIndex(name);
        for (String[] row : rows) {
            if (row[idx].trim().equals(value)) {
                results.add(row);
            }
        }
        return results;
    }

    public String[] getRowByColumn(String name, String value) {
        List<String[]> results = getRowsByColumn(name, value);
        if (results.isEmpty()) {
            throw new RuntimeException("No row found for '" + name + "'='" + value + "'");
        } else if (results.size() > 1) {
            throw new RuntimeException("Multiple (" + results.size() + ") rows found for '" + name + "'='" + value + "' but expected one.");
        }
        return results.get(0);
    }

    public Object convertTo(Class<?> valueType, String value) {
        if (String.class.equals(valueType)) {
            return value;
        } else if (Long.class.equals(valueType)) {
            String tmp = value.trim().replaceAll(",", "");
            if (tmp.length() == 0) {
                return null;
            } else {
                return Long.valueOf(tmp);
            }
        } else if (Double.class.equals(valueType)) {
            return Double.valueOf(value.trim().replaceAll(",", ""));
        } else if (Boolean.class.equals(valueType)) {
            String tmp = value.trim().replaceAll(",", "");
            if (tmp.equals("1") || tmp.equals("J")) {
                return true;
            } else if (tmp.equals("0") || tmp.equals("N")) {
                return false;
            } else if (tmp.equals("") || tmp.equals("M")) {
                return null;
            } else {
                throw new NumberFormatException("Not a Boolean: '" + tmp + "'");
            }
        } else if (LocalDate.class.equals(valueType)) {
            String tmp = value.trim().replaceAll(",", "");
            if (tmp.length() > 0) {
                int daysSince1800 = Integer.valueOf(tmp);
                if (daysSince1800 != 0) {
                    return new LocalDate(1800, 12, 28).plusDays(daysSince1800);
                } else {
                    return null;
                }
            } else {
                return null;
            }

        } else {
            throw new IllegalArgumentException("Unknown conversion for '" + valueType.getSimpleName() + "' and value '" + value + "'");
        }
    }

    public int getColumnIndex(String name) {
        for (int t = 0; t < header.length; t++) {
            if (name.equals(header[t])) {
                return t;
            }
        }
        throw new IllegalArgumentException("Missing column '" + name + "'");
    }

    public String getRowValue(String name, String[] row) {
        return row[getColumnIndex(name)];
    }

}
