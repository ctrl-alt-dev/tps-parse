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
package nl.cad.tpsparse.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares two CSV files.
 * @author E.Hooijmeijer
 */
public class CsvDiff {

    private List<String> errors;

    public CsvDiff() {
        errors = new ArrayList<String>();
    }

    /**
     * compares two csv files.
     * @param generated the generated file.
     * @param compareTo the file to compare to.
     * @return true if there were no errors.
     */
    public boolean compareCsv(CsvFile generated, CsvFile compareTo) {
        assertEquals("Number of header fields", compareTo.getHeader().length, generated.getHeader().length);
        if (!errors.isEmpty()) {
            return false;
        }
        for (int t = 0; t < compareTo.getHeader().length; t++) {
            assertEquals("Header field.", compareTo.getHeader()[t], generated.getHeader()[t]);
        }
        assertEquals("Number of rows", compareTo.getRows().size(), generated.getRows().size());
        List<String> spurious = findMissingIds(generated, compareTo);
        if (!spurious.isEmpty()) {
            errors.add("Spurious Row Ids : " + spurious);
        }
        List<String> missing = findMissingIds(compareTo, generated);
        if (!missing.isEmpty()) {
            errors.add("Missing Row Ids : " + missing);
        }
        int genRow = 0;
        int cmpRow = 0;
        do {
            String[] row = generated.getRows().get(genRow);
            String[] cmp = compareTo.getRows().get(cmpRow);
            //
            while (spurious.contains(row[0].trim()) && cmpRow < generated.getRows().size() - 1) {
                errors.add("Skipping generated row " + genRow + " as its spurious.");
                genRow++;
                row = generated.getRows().get(genRow);
            }
            while (missing.contains(cmp[0].trim()) && cmpRow < compareTo.getRows().size() - 1) {
                errors.add("Skipping compare row " + cmpRow + " as its missing.");
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
        //
        return errors.isEmpty();
    }

    /**
     * @return the errors.
     */
    public List<String> getErrors() {
        return errors;
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

    private void assertSloppyEquals(String msg, String expect, String got) {
        String sloppyExpect = toSloppy(expect);
        String sloppyGot = toSloppy(got);
        assertEquals(msg, sloppyExpect, sloppyGot);
    }

    private String toSloppy(String expect) {
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

    private void assertEquals(String msg, int expect, int got) {
        if (expect != got) {
            errors.add(msg + ". expected " + expect + " got " + got);
        }
    }

    private void assertEquals(String msg, String expect, String got) {
        if (!expect.equals(got)) {
            errors.add(msg + ". expected '" + expect + "' got '" + got + "'");
            if (expect.length() > 128) {
                assertEquals(" lengths differ: ", expect.length(), got.length());
                int max = Math.min(expect.length(), got.length());
                for (int t = 0; t < max; t++) {
                    if (expect.charAt(t) != got.charAt(t)) {
                        errors.add(" First difference at position " + t + " char 0x" + Integer.toHexString(expect.charAt(t)) + " != 0x"
                                + Integer.toHexString(got.charAt(t)));
                        break;
                    }
                }
            }
        }
    }
}
