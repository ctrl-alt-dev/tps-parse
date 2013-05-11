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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows random-access reading of primitives from byte array. Supports little
 * endian and big endian integer formats. IEEE floating point and various styles
 * of strings.
 * @author E.Hooijmeijer
 */
public class RandomAccess {

    private int ofs = 0;
    private byte[] data;
    private List<Integer> positionStack = new ArrayList<Integer>();

    public RandomAccess(byte[] data) {
        this.ofs = 0;
        this.data = data;
    }

    public RandomAccess(String hex) {
        String[] bytes = hex.split(" ");
        data = new byte[bytes.length];
        for (int t = 0; t < data.length; t++) {
            data[t] = (byte) Integer.parseInt(bytes[t], 16);
        }
    }

    /**
     * pushes the current position on the stack.
     */
    public void pushPosition() {
        positionStack.add(ofs);
    }

    /**
     * pops the previous position from the stack.
     */
    public void popPosition() {
        ofs = positionStack.remove(positionStack.size() - 1);
    }

    /**
     * reads an 2s-complement signed 4 byte integer.
     * @return an integer.
     */
    public int leLong() {
        int out = (data[ofs] & 0xFF) | ((data[ofs + 1] & 0xFF) << 8) | ((data[ofs + 2] & 0xFF) << 16) | ((data[ofs + 3] & 0xFF) << 24);
        //
        // For some records it seems that 1s complement is used to encode
        // negative numbers ?!
        //
        ofs += 4;
        return out;
    }

    /**
     * writes a 4 byte little endian value to the current position. Used when
     * decrypting.
     * @param value the value.
     */
    public void setLeLong(int value) {
        data[ofs] = (byte) ((value >> 0) & 0xFF);
        data[ofs + 1] = (byte) ((value >> 8) & 0xFF);
        data[ofs + 2] = (byte) ((value >> 16) & 0xFF);
        data[ofs + 3] = (byte) ((value >> 24) & 0xFF);
        ofs += 4;
    }

    /**
     * reads an unsigned 4 byte integer into a long.
     * @return an unsigned 4 byte value.
     */
    public long leULong() {
        long out = (data[ofs] & 0xFFL) | ((data[ofs + 1] & 0xFFL) << 8L) | ((data[ofs + 2] & 0xFFL) << 16L) | ((data[ofs + 3] & 0xFFL) << 24L);
        ofs += 4;
        return out;
    }

    /**
     * @return big endian signed long.
     */
    public int beLong() {
        int out = (data[ofs + 3] & 0xFF) | ((data[ofs + 2] & 0xFF) << 8) | ((data[ofs + 1] & 0xFF) << 16) | ((data[ofs] & 0xFF) << 24);
        ofs += 4;
        return out;
    }

    /**
     * @return big endian unsigned long.
     */
    public long beULong() {
        long out = (data[ofs + 3] & 0xFFL) | ((data[ofs + 2] & 0xFFL) << 8L) | ((data[ofs + 1] & 0xFFL) << 16L) | ((data[ofs + 0] & 0xFFL) << 24L);
        ofs += 4;
        return out;
    }

    public int leShort() {
        int out = (data[ofs] & 0xFF) | ((data[ofs + 1] & 0xFF) << 8);
        ofs += 2;
        return out;
    }

    public Object leUShort() {
        int out = (data[ofs] & 0xFF) | ((data[ofs + 1] & 0xFF) << 8);
        ofs += 2;
        return out;
    }

    public int beShort() {
        int out = (data[ofs + 1] & 0xFF) | ((data[ofs] & 0xFF) << 8);
        ofs += 2;
        return out;
    }

    public int leByte() {
        int out = (data[ofs] & 0xFF);
        ofs += 1;
        return out;
    }

    public int beByte() {
        int out = (data[ofs] & 0xFF);
        ofs += 1;
        return out;
    }

    public float leFloat() {
        return Float.intBitsToFloat(leLong());
    }

    public double leDouble() {
        long lsb = leLong() & 0xFFFFFFFFL;
        long msb = leLong() & 0xFFFFFFFFL;
        return Double.longBitsToDouble(msb << 32 | lsb);
    }

    public String fixedLengthString(int len) {
        String str = new String(data, ofs, len, Charset.forName("ISO-8859-1"));
        ofs += len;
        return str;
    }

    /**
     * zero terminated (or c-strings) are terminated by the first 0.
     * @return the string.
     */
    public String zeroTerminatedString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int value = 0;
        do {
            value = leByte();
            if (value != 0) {
                out.write(value);
            }
        } while (value != 0);
        return new String(out.toByteArray(), Charset.forName("ISO-8859-1"));
    }

    /**
     * pascal strings have their lenght encoded in the first byte.
     * @return the string.
     */
    public String pascalString() {
        int len = leByte();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int t = 0; t < len; t++) {
            out.write(leByte());
        }
        return new String(out.toByteArray(), Charset.forName("ISO-8859-1"));
    }

    public RandomAccess jumpAbs(int ofs) {
        this.ofs = ofs;
        return this;
    }

    public void jumpRel(int ofs) {
        this.ofs += ofs;
    }

    public int position() {
        return this.ofs;
    }

    public int length() {
        return data.length;
    }

    public byte[] data() {
        return data;
    }

    public RandomAccess read(int len) {
        byte[] tmp = new byte[len];
        System.arraycopy(data, ofs, tmp, 0, len);
        ofs += len;
        return new RandomAccess(tmp);
    }

    public byte[] readBytes(int len) {
        byte[] tmp = new byte[len];
        System.arraycopy(data, ofs, tmp, 0, len);
        ofs += len;
        return tmp;
    }

    /**
     * unpacks a run length encoded sequence of bytes.
     * @param cmp the random access to read from.
     * @return a random access of the decompressed bytes.
     */
    public RandomAccess deRle(RandomAccess cmp) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            do {
                int skip = cmp.leByte();
                if (skip == 0) {
                    throw new RunLengthEncodingException("Bad RLE Skip (0x00)");
                }
                if (skip > 0x7F) {
                    int msb = cmp.leByte();
                    int lsb = (skip & 0x7F);
                    int shift = 0x80 * (msb & 0x01);
                    skip = ((msb << 7) & 0x00FF00) + lsb + shift;
                }
                out.write(cmp.readBytes(skip));
                if (!cmp.isOneByteLeft()) {
                    cmp.jumpRel(-1);
                    int toRepeat = cmp.leByte();
                    int repeatsMinusOne = cmp.leByte();
                    if (repeatsMinusOne > 0x7F) {
                        int msb = cmp.leByte();
                        int lsb = (repeatsMinusOne & 0x7F);
                        int shift = 0x80 * (msb & 0x01);
                        repeatsMinusOne = ((msb << 7) & 0x00FF00) + lsb + shift;
                    }
                    byte[] repeat = new byte[repeatsMinusOne];
                    for (int t = 0; t < repeat.length; t++) {
                        repeat[t] = (byte) toRepeat;
                    }
                    out.write(repeat);
                }
            } while (!cmp.isAtEnd());
            //
        } catch (IOException ex) {
            throw new RunLengthEncodingException(ex);
        }
        return new RandomAccess(out.toByteArray());
    }

    public String toHex4(int value) {
        String tmp = Integer.toHexString(value);
        while (tmp.length() < 4) {
            tmp = "0" + tmp;
        }
        return tmp;
    }

    public String toHex2(int value) {
        String tmp = Integer.toHexString(value);
        while (tmp.length() < 2) {
            tmp = "0" + tmp;
        }
        return tmp;
    }

    public boolean isOneByteLeft() {
        return ofs > length() - 1;
    }

    public boolean isAtEnd() {
        return ofs >= length() - 1;
    }

    @Override
    public String toString() {
        return Integer.toHexString(ofs) + "/" + Integer.toHexString(length());
    }

    public String toHex8(int value) {
        String tmp = Integer.toHexString(value);
        while (tmp.length() < 8) {
            tmp = "0" + tmp;
        }
        return tmp;
    }

    public int[] leLongArray(int i) {
        int[] results = new int[i];
        for (int t = 0; t < i; t++) {
            results[t] = leLong();
        }
        return results;
    }

    public byte[] remainder() {
        byte[] result = new byte[data.length - ofs];
        System.arraycopy(data, ofs, result, 0, result.length);
        return result;
    }

    public int toFileOffset(int pageReference) {
        return (pageReference << 8) + 0x200;
    }

    public int[] toFileOffset(int[] pageReferences) {
        int[] results = new int[pageReferences.length];
        for (int t = 0; t < results.length; t++) {
            results[t] = toFileOffset(pageReferences[t]);
        }
        return results;
    }

    public String toHex() {
        return toHex(32, false);
    }

    public String toHex(int step, boolean asc) {
        StringBuilder sb = new StringBuilder();
        for (int t = 0; t < data.length; t += step) {
            sb.append(toHex4(t) + " : ");
            for (int y = 0; y < step; y++) {
                if (t + y < data.length) {
                    sb.append(toHex2(data[t + y] & 0x00FF));
                    sb.append(" ");
                }
            }
            if (asc) {
                sb.append(" ");
                for (int y = 0; y < step; y++) {
                    if (t + y < data.length) {
                        int ch = data[t + y] & 0x00FF;
                        if (ch < 32 || ch > 127) {
                            sb.append(".");
                        } else {
                            sb.append((char) ch);
                        }
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String toAscii() {
        StringBuilder sb = new StringBuilder();
        boolean wasHex = false;
        for (int t = 0; t < data.length; t++) {
            int v = data[t] & 0x00FF;
            if ((v < 32) || (v > 127)) {
                sb.append(" ");
                sb.append(toHex2(v));
                wasHex = true;
            } else {
                if (wasHex) {
                    sb.append(" ");
                    wasHex = false;
                }
                if (v == 32) {
                    sb.append(".");
                } else {
                    sb.append((char) v);
                }
            }
        }
        return sb.toString();
    }

    public String binaryCodedDecimal(int len, int totalDigits, int digitsAfterDecimalPoint) {
        StringBuilder sb = new StringBuilder();
        for (byte b : readBytes(len)) {
            sb.append(toHex2(b & 0xFF));
        }
        String sign = sb.substring(0, 1);
        String number = sb.substring(1);
        if (digitsAfterDecimalPoint > 0) {
            number = number.substring(number.length() - digitsAfterDecimalPoint - totalDigits, number.length() - digitsAfterDecimalPoint) + "."
                    + number.substring(number.length() - digitsAfterDecimalPoint);
        }
        return (!sign.equals("0") ? "-" : "") + number;
    }

}
