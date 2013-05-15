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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;

/**
 * @author E.Hooijmeijer
 */
public class CsvDiffTest {

    private static final String LS = System.getProperty("line.separator");

    private CsvDiff diff;
    private CsvReader generated;
    private CsvReader compareTo;

    @Before
    public void init() {
        diff = new CsvDiff();
        generated = new CsvReader(',', '"');
        compareTo = new CsvReader(',', '"');
    }

    @Test
    public void shouldHaveNoDiff() throws IOException {
        assertTrue(diff.compareCsv(generated.read(read("a,b,c" + LS + "1,2,3"), null), compareTo.read(read("a,b,c" + LS + "1,2,3"), null)));
    }

    @Test
    public void shouldHaveDiffCols() throws IOException {
        assertFalse(diff.compareCsv(generated.read(read("a,b,c" + LS + "2,2,3"), null), compareTo.read(read("a,b,c,d" + LS + "1,2,3,4"), null)));
    }

    @Test
    public void shouldHaveDiffValue() throws IOException {
        assertFalse(diff.compareCsv(generated.read(read("a,b,c" + LS + "2,2,3"), null), compareTo.read(read("a,b,c" + LS + "1,2,3"), null)));
        assertFalse(diff.compareCsv(generated.read(read("a,z,c" + LS + "2,2,3"), null), compareTo.read(read("a,b,c" + LS + "1,2,3"), null)));
    }

    @Test
    public void shouldHaveDiffInLines() throws IOException {
        assertFalse(diff.compareCsv(generated.read(read("a,b,c" + LS + "1,2,3" + LS + "2,2,3"), null), compareTo.read(read("a,b,c" + LS + "1,2,3"), null)));
        assertFalse(diff.compareCsv(generated.read(read("a,b,c" + LS + "1,2,3"), null), compareTo.read(read("a,b,c" + LS + "1,2,3" + LS + "2,2,3"), null)));
    }

    /**
     * @param string
     * @return
     */
    private BufferedReader read(String string) {
        return new BufferedReader(new StringReader(string));
    }

}
