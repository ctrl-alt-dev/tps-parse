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
package nl.cad.tpsparse.bin;

import static org.junit.Assert.assertEquals;
import nl.cad.tpsparse.bin.RandomAccess;

import org.junit.Test;

public class RleTest {

    @Test
    public void shouldDeRleSimpleBlock() {
        // skip one '1', repeat 7 '1'.
        byte[] block = new byte[] { 0x01, 0x31, 0x07 };
        RandomAccess deRle = new RandomAccess(new byte[0]).deRle(new RandomAccess(block));
        assertEquals(8, deRle.length());
        assertEquals("11111111", deRle.toAscii());
    }

    @Test
    public void shouldDeRleDoubleBlock() {
        // skip one '1', repeat 7 '1', skip '2','3', repeat '3' 3 times.
        byte[] block = new byte[] { 0x01, 0x31, 0x07, 0x02, 0x32, 0x33, 0x03 };
        RandomAccess deRle = new RandomAccess(new byte[0]).deRle(new RandomAccess(block));
        assertEquals(13, deRle.length());
        assertEquals("1111111123333", deRle.toAscii());
    }

    @Test
    public void shouldEndAfterSkip() {
        // skip one '1', repeat 7 '1', skip '2','3', repeat '3' 3 times.
        byte[] block = new byte[] { 0x01, 0x31 };
        RandomAccess deRle = new RandomAccess(new byte[0]).deRle(new RandomAccess(block));
        assertEquals(1, deRle.length());
        assertEquals("1", deRle.toAscii());
    }

    @Test
    public void shouldEndAfterRepeat() {
        byte[] block = new byte[] { 0x01, 0x31, 0x07 };
        RandomAccess deRle = new RandomAccess(new byte[0]).deRle(new RandomAccess(block));
        assertEquals(8, deRle.length());
        assertEquals("11111111", deRle.toAscii());
    }

    @Test
    public void shouldDeRleLongSkip() {
        byte[] block = new byte[131];
        block[0] = (byte) 0x80;
        block[1] = (byte) 0x01;
        block[130] = (byte) 0x10;
        RandomAccess deRle = new RandomAccess(new byte[0]).deRle(new RandomAccess(block));
        assertEquals(128 + 16, deRle.length());
    }

    @Test
    public void shouldDeRleLongRepeat() {
        byte[] block = new byte[] { 0x01, 0x31, (byte) 0x80, 0x01 };
        RandomAccess deRle = new RandomAccess(new byte[0]).deRle(new RandomAccess(block));
        assertEquals(129, deRle.length());
    }
}
