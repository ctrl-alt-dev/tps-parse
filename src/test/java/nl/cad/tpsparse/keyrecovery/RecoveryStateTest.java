/*
 *  Copyright 2016 E.Hooijmeijer
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
package nl.cad.tpsparse.keyrecovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.decrypt.Key;

/**
 * @author E.Hooijmeijer
 *
 */
public class RecoveryStateTest {

    private Block plaintext;
    private Block encrypted;

    private List<Block> blocks = new ArrayList<>();

    @Before
    public void init() {
        Key k = new Key("nasigoreng").init();
        byte[] plain = new byte[64];
        byte[] crypt = new byte[64];
        byte[] cseq = new byte[64];
        byte[] cb0b0 = new byte[64];
        for (int t = 0; t < 64; t++) {
            cseq[t] = (byte) t;
            cb0b0[t] = (byte) 0xB0;
        }
        k.encrypt64(crypt);
        k.encrypt64(cb0b0);
        k.encrypt64(cseq);
        plaintext = new Block(new RandomAccess(plain), false);
        encrypted = new Block(new RandomAccess(crypt), true);
        Block cryptSeq = new Block(new RandomAccess(cseq), false);
        Block cryptB0B0 = new Block(new RandomAccess(cb0b0), false);
        blocks.add(encrypted);
        blocks.add(new Block(0x400, cryptB0B0));
        blocks.add(new Block(0x500, cryptSeq));
        blocks.add(new Block(0x600, cryptB0B0));
        blocks.add(new Block(0x700, cryptSeq));
        blocks.add(new Block(0x800, cryptB0B0));
        blocks.add(new Block(0xA00, cryptSeq));
    }

    @Test
    public void shouldDoSelfScan() {
        RecoveryState start = RecoveryState.start(encrypted, plaintext);
        assertEquals(1, start.indexSelfScan(8).size());
        assertEquals(0, start.indexSelfScan(15).size());
    }

    @Test
    public void shouldScanAndReduce() throws IOException {
        RecoveryState start = RecoveryState.start(encrypted, plaintext);
        List<RecoveryState> scanResults = start.indexScan(15);
        assertEquals(192, scanResults.size());

        scanResults = RecoveryState.reduceFirst(scanResults, 15, blocks);
        assertEquals(2, scanResults.size());

        verifyReadAndWrite(scanResults.get(0));

        scanResults = scanResults.get(0).indexScan(14);
        assertEquals(1450, scanResults.size());

        scanResults = RecoveryState.reduceNext(scanResults, 14);
        assertEquals(2, scanResults.size());
    }

    private void verifyReadAndWrite(RecoveryState recoveryState) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(out);
        recoveryState.write(oout);
        oout.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream iin = new ObjectInputStream(in);
        RecoveryState copy = RecoveryState.read(iin);

        assertTrue(copy.equals(recoveryState));
    }

}
