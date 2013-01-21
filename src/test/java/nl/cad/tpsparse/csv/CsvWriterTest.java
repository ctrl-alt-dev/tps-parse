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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * CsvWriterTest.
 */
public class CsvWriterTest {

    private static final String LS = System.getProperty("line.separator");

    @Test
    public void shouldIgnoreColums() {
        CsvWriter wr = new CsvWriter(',', '"');
        wr.addColumn("a", false);
        wr.addColumn("b", true);
        wr.addColumn("c", false);
        wr.newRow();
        wr.addCell("1");
        wr.addCell("2");
        wr.addCell("3");
        wr.newRow();
        //
        assertEquals("\"a\",\"c\"" + LS + "\"1\",\"3\"" + LS, wr.toString());
    }

    @Test
    public void shouldEscapeQuotes() {
        CsvWriter wr = new CsvWriter(',', '"');
        wr.addColumn("a");
        wr.addColumn("b");
        wr.addColumn("c");
        wr.newRow();
        wr.addCell("1");
        wr.addCell(" \"2\" ");
        wr.addCell("3");
        wr.newRow();
        //
        assertEquals("\"a\",\"b\",\"c\"" + LS + "\"1\",\" \"\"2\"\" \",\"3\"" + LS, wr.toString());
    }
}
