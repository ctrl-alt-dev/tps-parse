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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and parses CSV files into a CsvFile object.
 * @author E.Hooijmeijer
 */
public class CsvReader {

    public interface LineSource {
        String readLine() throws IOException;

        int getLineCnt();
    }

    private final char sep;
    private final char quot;

    /**
     * creates a new csv reader. 
     * the csv files are intepreted according to the given separator and quote character.
     * @param sep the separator character.
     * @param quot the quot character.
     */
    public CsvReader(char sep, char quot) {
        this.sep = sep;
        this.quot = quot;
    }

    /**
     * reads a csv from file according to the given encoding.
     * @param file the file.
     * @param encoding the charset encoding.
     * @return the CsvFile.
     * @throws IOException if reading fails.
     */
    public CsvFile read(File file, String encoding) throws IOException {
        return read(new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName(encoding))), file);
    }

    /**
     * reads a csv file from a buffered reader.
     * @param in the reader.
     * @param file the source file (may be null).
     * @return the csv file.
     * @throws IOException if reading fails.
     */
    public CsvFile read(final BufferedReader in, File file) throws IOException {
        CsvFile result = new CsvFile();
        String line = "";
        LineSource ls = new LineSource() {
            private int lineCnt;

            @Override
            public String readLine() throws IOException {
                lineCnt++;
                return in.readLine();
            }

            @Override
            public int getLineCnt() {
                return lineCnt;
            }
        };
        //
        try {
            line = ls.readLine();
            while (line != null) {
                String[] fields = split(line, ls);
                result.append(fields);
                line = ls.readLine();
            }
        } catch (Exception ex) {
            throw new RuntimeException("In line " + ls.getLineCnt() + " of file " + file + "\n" + line, ex);
        } finally {
            in.close();
        }
        return result;
    }

    /**
     * Splits a line into separate cells.
     * @param line the line to split.
     * @param in the line source, if an additional line is needed.
     * @return the split string.
     * @throws IOException if reading fails.
     */
    private String[] split(String line, LineSource in) throws IOException {
        List<String> fields = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        do {
            char prevPrev = ' ';
            char prev = ' ';
            char next = ' ';
            for (int t = 0; t < line.length(); t++) {
                char current = line.charAt(t);
                next = (t + 1 < line.length() ? line.charAt(t + 1) : sep);
                if ((current == sep) && (!inQuote)) {
                    fields.add(sb.toString());
                    sb.delete(0, sb.length());
                } else if (current == quot) {
                    if ((prev == quot) && (prevPrev != quot) && (next != sep)) {
                        sb.append(quot);
                        current = ' '; // reset!
                        inQuote = !inQuote;
                    } else if (!inQuote) {
                        inQuote = true;
                    } else {
                        inQuote = false;
                    }
                } else {
                    if (Character.isDefined(current) && current >= 32 && current != 65533) {
                        sb.append(current);
                    }
                }
                prevPrev = prev;
                prev = current;
            }
            if (inQuote) {
                sb.append("\n");
                line = in.readLine();
            }
        } while ((inQuote) && (line != null));
        if (sb.length() > 0) {
            fields.add(sb.toString());
        }
        return fields.toArray(new String[fields.size()]);
    }
}
