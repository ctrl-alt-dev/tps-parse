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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import nl.cad.tpsparse.bin.RandomAccess;

/**
 * A block is 0x40 bytes at an offset.
 * A block may be encrypted.
 */
public class Block implements Comparable<Block> {

    private int offset;
    private int[] values;
    private boolean encrypted;

    public Block(int offset, int[] values, boolean encrypted) {
        this.offset = offset;
        this.values = new int[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
        this.encrypted = encrypted;
    }

    public Block(Block block) {
        this(block.offset, block.values, block.encrypted);
    }

    public Block(int offset, Block block) {
        this(offset, block.values, block.encrypted);
    }

    public Block(RandomAccess rx, boolean encrypted) {
        this(rx.position(), rx.leLongArray(16), encrypted);
    }

    public int getOffset() {
        return offset;
    }

    public int[] getValues() {
        return values;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    /**
     * applies a partial en/decryption.
     */
    public Block apply(int a, int b, int va, int vb) {
        Block target = new Block(this);
        target.values[a] = va;
        target.values[b] = vb;
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Block) {
            Block tmp = (Block) obj;
            return (offset == tmp.offset) && (encrypted == tmp.encrypted) && Arrays.equals(values, tmp.values);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return offset + 31 * (encrypted ? 1 : 0) + 31 * 31 * Arrays.hashCode(values);
    }

    @Override
    public int compareTo(Block o) {
        int d = offset - o.offset;
        if (d == 0) {
            d = (encrypted ? 1 : 0) - (o.encrypted ? 1 : 0);
            if (d == 0) {
                for (int t = 0; t < values.length; t++) {
                    d = values[t] - o.values[t];
                    if (d != 0) {
                        return d;
                    }
                }
            }
        }
        return d;
    }

    public boolean sameValue(Block b) {
        return Arrays.equals(values, b.values);
    }

    public void write(ObjectOutputStream out) throws IOException {
        out.writeInt(offset);
        out.writeBoolean(encrypted);
        out.writeInt(values.length);
        for (int t = 0; t < values.length; t++) {
            out.writeInt(values[t]);
        }
    }

    public static Block read(ObjectInputStream in) throws IOException {
        int offset = in.readInt();
        boolean encrypted = in.readBoolean();
        int[] values = new int[in.readInt()];
        for (int t = 0; t < values.length; t++) {
            values[t] = in.readInt();
        }
        return new Block(offset, values, encrypted);
    }
}
