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
package nl.cad.tpsparse.decrypt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.tps.TpsFile;
import nl.cad.tpsparse.tps.record.DataRecord;
import nl.cad.tpsparse.tps.record.TableDefinitionRecord;

/**
 * Tests the key generation, shuffling and decrypting of a data block.
 * @author E.Hooijmeijer
 */
public class KeyTest {

    @Test
    public void shouldCreate() {
        Key k = new Key("a");
        assertEquals(0x74229200, k.getWord(0));
        assertEquals(0x78269604, k.getWord(1));
        assertEquals(0x7c2a9a08, k.getWord(2));
        assertEquals(0x802e9e0c, k.getWord(3));
        assertEquals(0x701e8e3c, k.getWord(15));
    }

    @Test
    public void shouldShuffle() {
        Key k = new Key("a");
        k.shuffle();
        assertEquals(0x14a29220, k.getWord(0));
        assertEquals(0x745d8c18, k.getWord(1));
        assertEquals(0x78559430, k.getWord(2));
        assertEquals(0x646d3c48, k.getWord(3));
    }

    @Test
    public void shouldShuffleTwice() {
        Key k = new Key("a");
        k.shuffle();
        k.shuffle();
        assertEquals(0x7052a480, k.getWord(0));
        assertEquals(0x68dd1890, k.getWord(1));
        assertEquals(0xf1ab48a0, k.getWord(2));
        assertEquals(0x48dcf8a0, k.getWord(3));
    }

    @Test
    public void shouldDecryptBlock() throws IOException {
        byte[] buffer = new RandomAccess(ENCRYPTED_HEADER).data();
        //
        Key k = new Key("a");
        k.shuffle();
        k.shuffle();
        k.decrypt64(buffer);
        RandomAccess r = new RandomAccess(buffer);
        //
        byte[] ok = new RandomAccess(DECRYPTED_HEADER).data();
        assertArrayEquals(ok, r.data());
    }

    @Test
    public void shouldEncryptBlock() throws IOException {
        byte[] buffer = new RandomAccess(DECRYPTED_HEADER).data();
        //
        Key k = new Key("a");
        k.shuffle();
        k.shuffle();
        k.encrypt64(buffer);
        RandomAccess r = new RandomAccess(buffer);
        //
        byte[] ok = new RandomAccess(ENCRYPTED_HEADER).data();
        assertArrayEquals(ok, r.data());
    }

    /**
     * compares the values of two files holding the same data, but one encrypted
     * and one not. Note that record ids may differ.
     * @throws IOException
     */
    @Test
    public void shouldDecryptFile() throws IOException {
        TpsFile enc = new TpsFile(KeyTest.class.getResourceAsStream("/enc/encrypted-a.tps"), "a");
        TpsFile not = new TpsFile(KeyTest.class.getResourceAsStream("/enc/not-encrypted.tps"));
        Map<Integer, TableDefinitionRecord> encDef = enc.getTableDefinitions(false);
        Map<Integer, TableDefinitionRecord> notDef = not.getTableDefinitions(false);
        //
        assertEquals(notDef.size(), encDef.size());
        //
        List<DataRecord> encRecords = enc.getDataRecords(2, encDef.get(2), false);
        List<DataRecord> notRecords = not.getDataRecords(1, notDef.get(1), false);
        //
        assertEquals(notRecords.size(), encRecords.size());
        for (int t = 0; t < notRecords.size(); t++) {
            List<Object> notRec = notRecords.get(t).getValues();
            List<Object> encRec = encRecords.get(t).getValues();
            assertEquals(notRec.size(), encRec.size());
            for (int y = 0; y < notRec.size(); y++) {
                assertEquals(notRec.get(y), encRec.get(y));
            }
        }
    }

    private static final String ENCRYPTED_HEADER = //
            "BC DC 5C 92 90 BC DF B8 B0 5B AF BB A5 F8 30 C5 " + //
                    "05 AE FF D0 F0 BF F7 C2 E0 DC FC 57 F7 BF FB 93 " + //
                    "A8 54 DA C0 70 6D AD AA 30 E9 BD FA D0 7A FD D4 " + //
                    "DD FF FE E1 50 F9 FE C1 E0 D3 77 E3 F5 7A BF F1";

    private static final String DECRYPTED_HEADER = //
            "00 00 00 00 00 02 00 c2 05 00 00 c2 05 00 74 4f " + //
                    "70 53 00 00 00 00 1a 25 07 00 00 00 05 00 00 00 " + //
                    "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " + //
                    "00 00 00 00 00 00 00 00 05 00 00 00 0c 00 00 00";

}
