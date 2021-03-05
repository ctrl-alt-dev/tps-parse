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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NavigableMap;

import org.junit.Test;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.decrypt.Key;

public class PartialKeyTest {

    @Test
    public void shouldEqualsAndCompare() {
        PartialKey k1 = new PartialKey();
        PartialKey k2 = k1.apply(0x0F, 42);
        PartialKey k3 = k1.apply(0x0F, 42);

        assertFalse(k1.equals(k2));
        assertTrue(k3.equals(k2));

        assertEquals(-42, k1.compareTo(k2));
        assertEquals(42, k2.compareTo(k1));
        assertEquals(0, k2.compareTo(k3));
    }

    @Test
    public void shouldInvalid() {
        PartialKey k1 = new PartialKey();
        assertEquals(16, k1.getInvalidIndexes().length);
        assertEquals(15, k1.getInvalidIndexes()[0]);
        assertFalse(k1.isComplete());

        for (int t = 0; t < 16; t++) {
            k1 = k1.apply(t, t);
        }
        assertEquals(0, k1.getInvalidIndexes().length);
        assertTrue(k1.isComplete());
    }

    @Test
    public void shouldDecrypt() {
        Key k = new Key("aaa").init();
        byte[] plain = new byte[64];
        byte[] crypt = new byte[64];
        k.encrypt64(crypt);
        Block bplain = new Block(new RandomAccess(plain), false);
        Block bcrypt = new Block(new RandomAccess(crypt), true);

        PartialKey k1 = new PartialKey().apply(15, k.getWord(15));
        Block result = k1.partialDecrypt(15, bcrypt);

        assertEquals(bplain.getValues()[15], result.getValues()[15]);
    }

    @Test
    public void shouldReverseKeyScan() {
        Key k = new Key("aaa").init();
        byte[] plain = new byte[64];
        byte[] crypt = new byte[64];
        k.encrypt64(crypt);
        Block bplain = new Block(new RandomAccess(plain), false);
        Block bcrypt = new Block(new RandomAccess(crypt), true);

        PartialKey k1 = new PartialKey();
        PartialKey toFind = k1.apply(15, k.getWord(15));

        NavigableMap<PartialKey, Block> results = k1.reverseKeyIndexScan(15, bcrypt, bplain);

        assertTrue(results.containsKey(toFind));
        assertEquals(1216, results.size());
    }

    @Test
    public void shouldForwardKeyScan() {
        Key k = new Key("aaa").init();
        byte[] plain = new byte[64];
        byte[] crypt = new byte[64];
        k.encrypt64(crypt);
        Block bplain = new Block(new RandomAccess(plain), false);
        Block bcrypt = new Block(new RandomAccess(crypt), true);

        PartialKey k1 = new PartialKey();
        PartialKey toFind = k1.apply(3, k.getWord(3));

        NavigableMap<PartialKey, Block> results = k1.forwardKeyIndexScan(3, bcrypt, bplain);
        assertTrue(results.containsKey(toFind));
        assertEquals(1, results.size());
    }

}
