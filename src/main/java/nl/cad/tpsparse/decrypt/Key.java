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

import java.nio.charset.Charset;

import nl.cad.tpsparse.bin.RandomAccess;

/**
 * Encapsulates the encryption algorithm of TPS files. It works in 64 byte
 * blocks which are shuffled in curious ways.
 * @author E.Hooijmeijer
 */
public class Key {

    private RandomAccess rx;

    private Key() {
        super();
    }

    public Key(String key) {
        this(key.getBytes(Charset.forName("cp1258")));
    }

    /**
     * creates a new key to en/decrypt TPS files.
     * @param bytes the bytes to make the key from, an additional 0 byte is
     * appended.
     */
    public Key(byte[] bytes) {
        byte[] keybytes = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, keybytes, 0, bytes.length);
        // Smear out the password over 64 bytes.
        byte[] block = new byte[64];
        for (int t = 0; t < block.length; t++) {
            int tx = (t * 0x11) & 0x3f;
            block[tx] = (byte) ((t + (int) (keybytes[(t + 1) % keybytes.length] & 0xFF)) & 0xFF);
        }
        rx = new RandomAccess(block);
    }

    public static Key initializedKey(byte[] bytes) {
        Key key = new Key();
        key.rx = new RandomAccess(bytes);
        return key;
    }

    /**
     * shuffles twice, which is required to initialize the key.
     */
    public Key init() {
        shuffle();
        shuffle();
        return this;
    }

    /**
     * shuffles the smeared key, this time by 4 byte words and binary logic. You
     * need to call this twice to properly initialize the key. for each of the 4
     * byte words in the key, it takes one, uses the last 4 bits to find another
     * 4 byte word in the key, and then does some arithmetic:
     * <ul>
     * <li>the first word is replaced by : worda + worda & wordb.</li>
     * <li>the second word is replaced by : worda + worda | wordb.</li>
     * <ul>
     */
    public void shuffle() {
        for (int t = 0; t < 0x10; t++) {
            int worda = getWord(t);
            int posb = worda & 0x0F;
            int wordb = getWord((int) posb);
            //
            int and1 = worda & wordb;
            int sum1 = (int) ((((long) worda) & 0xFFFFFFFFL) + ((long) and1) & 0xFFFFFFFFL);
            setWord(posb, sum1);
            //
            int or1 = worda | wordb;
            int sum2 = (int) ((((long) or1) & 0xFFFFFFFFL) + ((long) worda) & 0xFFFFFFFFL);
            //
            setWord(t, sum2);
        }
    }

    /**
     * encrypts a buffer of 64 bytes using the key. Two words are picked, one
     * using the current offset, and one using the 4 last bits of the key. These
     * two words are then replaced by the result of the following formula:
     * <ul>
     * <li>data2 = key + (key.data2 | !key.data1)</li>
     * <li>data1 = key + (!key.data2 | key.data1)</li>
     * </ul>
     * Basically half of the bits of each word go to the other word and vice
     * versa. Then its offset against the key. By doing it like this no
     * information is lost, it just gets shifted around.
     * @param buffer the buffer.
     */
    public void encrypt64(byte[] buffer) {
        RandomAccess data = new RandomAccess(buffer);
        for (int t = 0; t < 0x10; t++) {
            int posa = t;
            int keya = getWord(t);
            int posb = keya & 0x0F;
            //
            int data2 = data.jumpAbs(posa * 4).leLong();
            int data1 = data.jumpAbs(posb * 4).leLong();
            //
            int and1 = keya & data2;
            int nota = ~keya;
            int and2 = nota & data1;
            int andor = and1 | and2;
            //
            int sum1 = (int) ((((long) keya) & 0xFFFFFFFFL) + ((long) andor) & 0xFFFFFFFFL);
            data.jumpAbs(posa * 4).setLeLong(sum1);
            //
            int and3 = keya & data1;
            int and4 = nota & data2;
            int andor2 = and3 | and4;
            int sum2 = (int) ((((long) andor2) & 0xFFFFFFFFL) + ((long) keya) & 0xFFFFFFFFL);
            //
            data.jumpAbs(posb * 4).setLeLong(sum2);
        }
    }

    /**
     * decrypts the buffer. This is the reverse process of the encrypt. Note
     * that it runs in reverse order. The purpose is to undo the steps taken in
     * the encryption. So first the addition needs to be undone, and then the
     * bits have to be placed at their original location.
     * <ul>
     * <li>data1 = (key & (data1 - key)) | (!key & (data2 - key))</li>
     * <li>data2 = (!key & (data1 - key)) | (key & (data2 - key))</li>
     * </ul>
     * @param buffer the buffer.
     */
    public void decrypt64(byte[] buffer) {
        RandomAccess data = new RandomAccess(buffer);
        for (int t = 0x0F; t >= 0; t--) {
            int posa = t;
            int keya = getWord((int) posa);
            int posb = keya & 0x0F;
            //
            int data1 = data.jumpAbs(posa * 4).leLong();
            data1 = (int) ((((long) data1) & 0xFFFFFFFFL) - (((long) keya) & 0xFFFFFFFFL));
            int data2 = data.jumpAbs(posb * 4).leLong();
            data2 = (int) ((((long) data2) & 0xFFFFFFFFL) - (((long) keya) & 0xFFFFFFFFL));
            int and1 = data1 & keya;
            int not1 = ~keya;
            int and2 = data2 & not1;
            int andor = and1 | and2;
            data.jumpAbs(posa * 4).setLeLong(andor);
            //
            int and3 = data2 & keya;
            int and4 = data1 & not1;
            int andor2 = and3 | and4;
            data.jumpAbs(posb * 4).setLeLong(andor2);
        }
    }

    private void setWord(int t, int value) {
        rx.jumpAbs(t * 4);
        rx.setLeLong(value);
    }

    /**
     * allows to peek inside the key, useful for testing.
     * @param w the word to get (0..15).
     * @return the word.
     */
    public int getWord(int w) {
        rx.jumpAbs(w * 4);
        return rx.leLong();
    }

    /**
     * decodes multiple blocks of 64 bytes.
     * @param bytes the bytes.
     * @return the decoded bytes (same array).
     */
    public byte[] decrypt(byte[] bytes, int ofs, int len) {
        if ((ofs % 64) != 0) {
            throw new IllegalArgumentException("ofs must be dividable by 64.");
        }
        if ((len % 64) != 0) {
            throw new IllegalArgumentException("len must be dividable by 64.");
        }
        byte[] buffer = new byte[64];
        for (int t = 0; t < len / 64; t++) {
            System.arraycopy(bytes, ofs + t * 64, buffer, 0, 64);
            decrypt64(buffer);
            System.arraycopy(buffer, 0, bytes, ofs + t * 64, 64);
        }
        return bytes;
    }

    @Override
    public String toString() {
        return rx.toHex(64, false);
    }

    public byte[] getBytes() {
        return rx.data();
    }

	public int[] toIntArray() {
		return new RandomAccess(this.getBytes()).leLongArray(16);
	}

}
