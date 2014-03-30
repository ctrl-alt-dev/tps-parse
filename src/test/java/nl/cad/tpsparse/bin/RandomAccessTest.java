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
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;

import org.junit.Test;

public class RandomAccessTest {
    @Test
    public void shouldParseByte() {
        assertEquals(0x01, new RandomAccess(new byte[] { 0x01 }).beByte());
        assertEquals(0x81, new RandomAccess(new byte[] { (byte) 0x81 }).beByte());
        assertEquals(0x01, new RandomAccess(new byte[] { 0x01 }).leByte());
        assertEquals(0x81, new RandomAccess(new byte[] { (byte) 0x81 }).leByte());
    }

    @Test
    public void shouldParseShort() {
        assertEquals(0x0102, new RandomAccess(new byte[] { 0x01, 0x02 }).beShort());
        assertEquals(0x8182, new RandomAccess(new byte[] { (byte) 0x81, (byte) 0x82 }).beShort());
        assertEquals(0x0102, new RandomAccess(new byte[] { 0x02, 0x01 }).leShort());
        assertEquals(0x8182, new RandomAccess(new byte[] { (byte) 0x82, (byte) 0x81 }).leShort());
    }

    @Test
    public void shouldParseLong() {
        assertEquals(0x01020304, new RandomAccess(new byte[] { 0x01, 0x02, 0x03, 0x04 }).beLong());
        assertEquals(0x81828384, new RandomAccess(new byte[] { (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84 }).beLong());
        assertEquals(0x01020304, new RandomAccess(new byte[] { 0x04, 0x03, 0x02, 0x01 }).leLong());
        assertEquals(0x81828384, new RandomAccess(new byte[] { (byte) 0x84, (byte) 0x83, (byte) 0x82, (byte) 0x81 }).leLong());
    }

    @Test
    public void shouldReadFixedLengthString() {
        assertEquals(" !", new RandomAccess(new byte[] { 0x20, 0x21, 0x22 }).fixedLengthString(2));
    }

    @Test
    public void shouldReadFixedLengthStringWithCP850() {
        assertEquals("é!", new RandomAccess(new byte[] { (byte) 0x82, 0x21, 0x22 }).fixedLengthString(2, Charset.forName("CP850")));
    }

    @Test
    public void shouldReadZeroTerminatedString() {
        assertEquals(" !\"", new RandomAccess(new byte[] { 0x20, 0x21, 0x22, 0x00, 0x23 }).zeroTerminatedString());
    }

    @Test
    public void shouldToHex() {
        assertEquals("0000 : 20 21 22 00 23 \n", new RandomAccess(new byte[] { 0x20, 0x21, 0x22, 0x00, 0x23 }).toHex());
    }

    @Test
    public void shouldToAscii() {
        assertEquals(".!\" 00 #", new RandomAccess(new byte[] { 0x20, 0x21, 0x22, 0x00, 0x23 }).toAscii());
    }

    @Test
    public void shouldFloat() {
        assertEquals(0.0f, new RandomAccess(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }).leFloat(), 0.0);
        assertTrue(Float.isInfinite(new RandomAccess(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x7f }).leFloat()));
        assertTrue(Float.isInfinite(new RandomAccess(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0xff }).leFloat()));
        assertEquals(0.1f, new RandomAccess(new byte[] { (byte) 0xcd, (byte) 0xcc, (byte) 0xcc, (byte) 0x3d }).leFloat(), 0.0);
    }

    @Test
    public void shouldDouble() {
        assertEquals(0.0, new RandomAccess(
                new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }).leDouble(), 0.0);
        assertTrue(Double.isInfinite(new RandomAccess(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf0,
                (byte) 0x7f }).leDouble()));
        assertTrue(Double.isInfinite(new RandomAccess(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf0,
                (byte) 0xff }).leDouble()));
        assertEquals(0.1, new RandomAccess(
                new byte[] { (byte) 0x9a, (byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0xb9, (byte) 0x3f }).leDouble(), 0.0);
    }

    @Test
    public void shouldParseBCD() {
        assertEquals("0", new RandomAccess(new byte[] { 0x00, 0x00 }).binaryCodedDecimal(2, 3, 0));
        assertEquals("979", new RandomAccess(new byte[] { 0x09, 0x79 }).binaryCodedDecimal(2, 3, 0));
        assertEquals("0.00", new RandomAccess(new byte[] { 0x00, 0x00 }).binaryCodedDecimal(2, 1, 2));
        assertEquals("10.0", new RandomAccess(new byte[] { 0x01, 0x00 }).binaryCodedDecimal(2, 2, 1));
        assertEquals("0.0", new RandomAccess(new byte[] { 0x00, 0x00 }).binaryCodedDecimal(2, 2, 1));
        assertEquals("0.00", new RandomAccess(new byte[] { 0x00, 0x00, 0x00 }).binaryCodedDecimal(3, 3, 2));
        assertEquals("1.23", new RandomAccess(new byte[] { 0x01, 0x23 }).binaryCodedDecimal(2, 1, 2));
        assertEquals("-1.23", new RandomAccess(new byte[] { (byte) 0xF1, 0x23 }).binaryCodedDecimal(2, 1, 2));
        // from real world TPS, 7 bytes -> 14 digits, minus one for the sign =
        // 13 digits, minus 8 behind the dot, remains 5 before, not 7!
        assertEquals("0.00000000", new RandomAccess(new byte[] { (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }).binaryCodedDecimal(7, 7, 8));
    }

    @Test
    public void leReadWriteTest() {
        RandomAccess rx = new RandomAccess(new byte[] { 1, 2, 3, 4 });
        int value = rx.leLong();
        rx.jumpAbs(0);
        rx.setLeLong(value);
        rx.jumpAbs(0);
        int value2 = rx.leLong();
        assertEquals(value, value2);
    }

    @Test
    public void shouldReuseBuffer() {
        RandomAccess rx = new RandomAccess(new byte[] { 1, 2, 3, 4 });
        rx.beByte();
        RandomAccess read = rx.read(3);
        assertEquals(3, read.length());
        assertEquals(0, read.position());
        assertEquals(2, read.leByte());
        assertEquals(3, read.leByte());
        assertEquals(4, read.leByte());
        read.jumpAbs(0);
        assertEquals(2, read.leByte());
        read.jumpRel(1);
        assertEquals(4, read.leByte());
        read.jumpAbs(1);
        RandomAccess read2 = read.read(2);
        assertEquals(2, read2.length());
        assertEquals(0, read2.position());
        assertEquals(3, read2.leByte());
        assertEquals(4, read2.leByte());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldFailBeyondBuffer() {
        RandomAccess rx = new RandomAccess(new byte[] { 1, 2, 3, 4 });
        rx.beByte();
        RandomAccess read = rx.read(3);
        read.leLong();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldFailBeforeBuffer() {
        RandomAccess rx = new RandomAccess(new byte[] { 1, 2, 3, 4 });
        rx.beByte();
        RandomAccess read = rx.read(3);
        read.jumpAbs(-1);
        read.beByte();
    }

}
