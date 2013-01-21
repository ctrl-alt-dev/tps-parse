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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

/**
 * CsvReaderTest.
 * @author E.Hooijmeijer
 */
public class CsvReaderTest {

    private static final String LS = System.getProperty("line.separator");

    /**
     * escaping quotes in CSV is more problematic than you would think.
     */
    @Test
    public void shouldEscapeQuotes() throws IOException {
        CsvReader rd = new CsvReader(',', '"');
        CsvFile read = rd.read(new BufferedReader(new StringReader("a,b,c" + LS + "1,\" 2 \",3" + LS)), null);
        assertEquals(3, read.getHeader().length);
        assertEquals(" 2 ", read.getRows().get(0)[1]);
        //
        read = rd.read(new BufferedReader(new StringReader("a,b,c" + LS + "1,\" \"\"2\"\" \",3" + LS)), null);
        assertEquals(3, read.getHeader().length);
        assertEquals(" \"2\" ", read.getRows().get(0)[1]);
        //
        read = rd.read(new BufferedReader(new StringReader("a,b,c" + LS + "1,\"\"\"2\"\"\",3" + LS)), null);
        assertEquals(3, read.getHeader().length);
        assertEquals("\"2\"", read.getRows().get(0)[1]);
        //
        read = rd.read(new BufferedReader(new StringReader("a,b,c" + LS + "1,\"\"\"\"\"2\"\"\"\"\",3" + LS)), null);
        assertEquals(3, read.getHeader().length);
        assertEquals("\"\"2\"\"", read.getRows().get(0)[1]);
        //
        read = rd.read(new BufferedReader(new StringReader("a,b,c" + LS + "1,\" K\"\"ln \",3" + LS)), null);
        assertEquals(3, read.getHeader().length);
        assertEquals(" K\"ln ", read.getRows().get(0)[1]);
        //
        read = rd.read(new BufferedReader(new StringReader("a,b,c" + LS + "1,\"\",3" + LS)), null);
        assertEquals(3, read.getHeader().length);
        assertEquals("", read.getRows().get(0)[1]);
        //
        read = rd.read(new BufferedReader(new StringReader("a,b,c" + LS + "1,2,\"\"" + LS)), null);
        assertEquals(3, read.getHeader().length);
        assertEquals("", read.getRows().get(0)[2]);
    }
}
