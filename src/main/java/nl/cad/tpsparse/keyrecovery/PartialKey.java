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

import java.util.Arrays;
import java.util.NavigableMap;
import java.util.TreeMap;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.decrypt.Key;

/**
 * A key being recovered.
 * A key is divided in sixteen 32 bit values.
 */
public class PartialKey implements Comparable<PartialKey> {

    private boolean[] valid;
    private int[] key;

    public PartialKey() {
        valid = new boolean[16];
        key = new int[16];
    }

    private PartialKey(PartialKey src, int idx, int value) {
        this();
        System.arraycopy(src.key, 0, key, 0, key.length);
        System.arraycopy(src.valid, 0, valid, 0, valid.length);
        key[idx] = value;
        valid[idx] = true;
    }

    public Block partialDecrypt(int idx, Block block) {
        if (!valid[idx]) {
            throw new IllegalArgumentException();
        }
        int keya = key[idx];
        int posa = idx;
        int posb = keya & 0x0F;
        //
        int data1 = block.getValues()[posa];
        data1 = (int) ((((long) data1) & 0xFFFFFFFFL) - (((long) keya) & 0xFFFFFFFFL));
        int data2 = block.getValues()[posb];
        data2 = (int) ((((long) data2) & 0xFFFFFFFFL) - (((long) keya) & 0xFFFFFFFFL));
        int and1 = data1 & keya;
        int not1 = ~keya;
        int and2 = data2 & not1;
        int andor = and1 | and2;
        //
        int and3 = data2 & keya;
        int and4 = data1 & not1;
        int andor2 = and3 | and4;

        return block.apply(posa, posb, andor, andor2);
    }

    /**
     * Attempts to find matching key values for the given index by matching a block
     * of crypttext with plaintext. This only works if there are no other key indexes
     * with a swap for this index. For index 0x0F it always works because none of
     * the other indexes will select index 0x0F.
     */
    public NavigableMap<PartialKey, Block> keyIndexScan(int idx, Block encrypted, Block plaintext) {
        NavigableMap<PartialKey, Block> results = new TreeMap<PartialKey, Block>();
        int posa = idx;
        int plain = plaintext.getValues()[posa];
        for (long v = 0; v <= 0xFFFFFFFFL; v++) {
            int keya = (int) v;
            int posb = keya & 0x0F;
            int data1 = encrypted.getValues()[posa];
            data1 = (int) ((((long) data1) & 0xFFFFFFFFL) - (((long) keya) & 0xFFFFFFFFL));
            int data2 = encrypted.getValues()[posb];
            data2 = (int) ((((long) data2) & 0xFFFFFFFFL) - (((long) keya) & 0xFFFFFFFFL));
            int and1 = data1 & keya;
            int not1 = ~keya;
            int and2 = data2 & not1;
            int andor = and1 | and2;
            //
            if (andor == plain) {
                //
                int and3 = data2 & keya;
                int and4 = data1 & not1;
                int andor2 = and3 | and4;
                //
                Block dec = encrypted.apply(posa, posb, andor, andor2);
                PartialKey key = this.apply(idx, keya);
                results.put(key, dec);
            }
        }
        return results;
    }

    public PartialKey apply(int idx, int keya) {
        return new PartialKey(this, idx, keya);
    }

    public Key toKey() {
        if (this.isComplete()) {
            byte[] key = new byte[64];
            RandomAccess rx = new RandomAccess(key);
            for (int t = 0; t < this.key.length; t++) {
                rx.setLeLong(this.key[t]);
            }
            return Key.initializedKey(key);
        } else {
            throw new IllegalStateException("Incomplete PartialKey");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PartialKey) {
            return Arrays.equals(key, ((PartialKey) obj).key) && Arrays.equals(valid, ((PartialKey) obj).valid);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key) + 31 * Arrays.hashCode(valid);
    }

    @Override
    public int compareTo(PartialKey cmp) {
        int d = 0;
        for (int t = 0; t < key.length; t++) {
            d = key[t] - cmp.key[t];
            if (d != 0) {
                return d;
            }
            d = (valid[t] ? 1 : 0) - (cmp.valid[t] ? 1 : 0);
            if (d != 0) {
                return d;
            }
        }
        return 0;
    }

    public boolean isComplete() {
        for (int t = 0; t < valid.length; t++) {
            if (!valid[t]) {
                return false;
            }
        }
        return true;
    }

    public int[] getInvalidIndexes() {
        int cnt = 0;
        for (int t = 0; t < valid.length; t++) {
            cnt += (!valid[t] ? 1 : 0);
        }
        int[] idx = new int[cnt];
        for (int t = 0; t < valid.length; t++) {
            if (!valid[t]) {
                idx[--cnt] = t;
            }
        }
        return idx;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int t = 0; t < valid.length; t++) {
            if (valid[t]) {
                String s = Integer.toHexString(key[t]);
                sb.append("00000000".substring(s.length()));
                sb.append(s);
                sb.append(" ");
            } else {
                sb.append("???????? ");
            }
        }
        return sb.toString();
    }

}
