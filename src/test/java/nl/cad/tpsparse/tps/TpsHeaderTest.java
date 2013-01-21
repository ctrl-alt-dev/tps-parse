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
package nl.cad.tpsparse.tps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class TpsHeaderTest {

    @Test
    public void shouldParseHeader() throws IOException {
        TpsFile file = new TpsFile(TpsHeaderTest.class.getResourceAsStream("/header.dat"));
        TpsHeader hdr = file.getHeader();
        assertTrue(hdr.isTopSpeedFile());
        assertEquals(383744, hdr.getFileLength1());
        assertEquals(5048, hdr.getLastIssuedRow());
        assertEquals(15651, hdr.getChanges());
        assertEquals(60, hdr.getPageStart().length);
        assertEquals(60, hdr.getPageEnd().length);
    }

    @Test(expected = NotATopSpeedFileException.class)
    public void shouldNotParseHeaderIfNotTopspeed() throws IOException {
        TpsFile file = new TpsFile(TpsHeaderTest.class.getResourceAsStream("/bad-header.dat"));
        file.getHeader();
    }
}
