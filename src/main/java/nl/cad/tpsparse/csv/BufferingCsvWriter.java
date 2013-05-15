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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * @author E.Hooijmeijer
 */
public class BufferingCsvWriter extends CsvWriter {

    private StringBuilder buffer = new StringBuilder();

    public BufferingCsvWriter(char sep, char quot) {
        super(sep, quot);
    }

    @Override
    protected void addRow(String row) {
        buffer.append(row);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        return buffer.toString();
    }

    /**
     * writes output as raw character bytes. Does not attempt any character
     * encoding.
     * @param target the target file.
     * @throws IOException if writing fails.
     */
    public void writeRaw(File target) throws IOException {
        FileOutputStream w = new FileOutputStream(target);
        try {
            String str = buffer.toString();
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
            wr.write(buffer.toString());
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
