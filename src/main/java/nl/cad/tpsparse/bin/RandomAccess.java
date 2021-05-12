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
    private int baseOfs;
    private int length;

    public RandomAccess(byte[] data) {
        this.ofs = 0;
        this.data = data;
        this.baseOfs = 0;
        this.length = data.length;
    }

    public RandomAccess(byte[] data, int baseOfs, int length) {
        this.baseOfs = baseOfs;
        this.length = length;
        this.ofs = 0;
        this.data = data;
    }

    public RandomAccess(String hex) {
        String[] bytes = hex.split(" ");
        data = new byte[bytes.length];
        this.baseOfs = 0;
        this.length = data.length;
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
        checkSpace(4);
        int ref = baseOfs + ofs;
        int out = (data[ref] & 0xFF) | ((data[ref + 1] & 0xFF) << 8) | ((data[ref + 2] & 0xFF) << 16) | ((data[ref + 3] & 0xFF) << 24);
        //
        // For some records it seems that 1s complement is used to encode
        // negative numbers ?!
        //
        ofs += 4;
        return out;
    }

    private void checkSpace(int bytes) {
        if (ofs + bytes > length) {
            throw new ArrayIndexOutOfBoundsException(ofs + bytes);
        }
        if (ofs < 0) {
            throw new ArrayIndexOutOfBoundsException(ofs);
        }
    }

    /**
     * writes a 4 byte little endian value to the current position. Used when
     * decrypting.
     * @param value the value.
     */
    public void setLeLong(int value) {
        checkSpace(4);
        int ref = baseOfs + ofs;
        data[ref] = (byte) ((value >> 0) & 0xFF);
        data[ref + 1] = (byte) ((value >> 8) & 0xFF);
        data[ref + 2] = (byte) ((value >> 16) & 0xFF);
        data[ref + 3] = (byte) ((value >> 24) & 0xFF);
        ofs += 4;
    }

    /**
     * reads an unsigned 4 byte integer into a long.
     * @return an unsigned 4 byte value.
     */
    public long leULong() {
        checkSpace(4);
        int ref = baseOfs + ofs;
        long out = (data[ref] & 0xFFL) | ((data[ref + 1] & 0xFFL) << 8L) | ((data[ref + 2] & 0xFFL) << 16L) | ((data[ref + 3] & 0xFFL) << 24L);
        ofs += 4;
        return out;
    }

    /**
     * @return big endian signed long.
     */
    public int beLong() {
        checkSpace(4);
        int ref = baseOfs + ofs;
        int out = (data[ref + 3] & 0xFF) | ((data[ref + 2] & 0xFF) << 8) | ((data[ref + 1] & 0xFF) << 16) | ((data[ref] & 0xFF) << 24);
        ofs += 4;
        return out;
    }

    /**
     * @return big endian unsigned long.
     */
    public long beULong() {
        checkSpace(4);
        int ref = baseOfs + ofs;
        long out = (data[ref + 3] & 0xFFL) | ((data[ref + 2] & 0xFFL) << 8L) | ((data[ref + 1] & 0xFFL) << 16L) | ((data[ref + 0] & 0xFFL) << 24L);
        ofs += 4;
        return out;
    }

    public int leShort() {
        checkSpace(2);
        int ref = baseOfs + ofs;
        int out = (data[ref] & 0xFF) | ((data[ref + 1] & 0xFF) << 8);
        ofs += 2;
        return out;
    }

    public Object leUShort() {
        checkSpace(2);
        int ref = baseOfs + ofs;
        int out = (data[ref] & 0xFF) | ((data[ref + 1] & 0xFF) << 8);
        ofs += 2;
        return out;
    }

    public int beShort() {
        checkSpace(2);
        int ref = baseOfs + ofs;
        int out = (data[ref + 1] & 0xFF) | ((data[ref] & 0xFF) << 8);
        ofs += 2;
        return out;
    }

    public int leByte() {
        checkSpace(1);
        int ref = baseOfs + ofs;
        int out = (data[ref] & 0xFF);
        ofs += 1;
        return out;
    }

    public int beByte() {
        checkSpace(1);
        int ref = baseOfs + ofs;
        int out = (data[ref] & 0xFF);
        ofs += 1;
        return out;
    }

    public byte peek(int pos) {
        return data[baseOfs + pos];
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
        checkSpace(len);
        return fixedLengthString(len, Charset.forName("ISO-8859-1"));
    }

    public String fixedLengthString(int len, Charset charset) {
        int ref = baseOfs + ofs;
        String str = new String(data, ref, len, charset);
        ofs += len;
        return str;
    }

    /**
     * zero terminated (or c-strings) are terminated by the first 0.
     * @return the string.
     */
    public String zeroTerminatedString() {
        return zeroTerminatedString(Charset.forName("ISO-8859-1"));
    }

    public String zeroTerminatedString(Charset charset) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int value = 0;
        do {
            value = leByte();
            if (value != 0) {
                out.write(value);
            }
        } while (value != 0);
        return new String(out.toByteArray(), charset);
    }

    /**
     * pascal strings have their length encoded in the first byte.
     * @return the string.
     */
    public String pascalString() {
        return pascalString(Charset.forName("ISO-8859-1"));
    }

    public String pascalString(Charset charset) {
        int len = leByte();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int t = 0; t < len; t++) {
            out.write(leByte());
        }
        return new String(out.toByteArray(), charset);
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
        return this.length;
    }

    public byte[] data() {
        if (baseOfs == 0 && data.length == length) {
            return data;
        } else {
            byte[] tmp = new byte[length];
            System.arraycopy(data, baseOfs, tmp, 0, length);
            return tmp;
        }
    }

    public RandomAccess read(int len) {
        checkSpace(len);
        int ref = baseOfs + ofs;
        ofs += len;
        return new RandomAccess(data, ref, len);
    }

    public byte[] readBytes(int len) {
        checkSpace(len);
        byte[] tmp = new byte[len];
        int ref = baseOfs + ofs;
        System.arraycopy(data, ref, tmp, 0, len);
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

    public String toStringBase() {
        return Integer.toHexString(baseOfs + ofs) + "/" + Integer.toHexString(length());
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
        int ref = baseOfs + ofs;
        byte[] result = new byte[length - ofs];
        System.arraycopy(data, ref, result, 0, result.length);
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
        for (int t = 0; t < length; t += step) {
            sb.append(toHex4(t) + " : ");
            for (int y = 0; y < step; y++) {
                if (t + y < length) {
                    sb.append(toHex2(data[baseOfs + t + y] & 0x00FF));
                    sb.append(" ");
                }
            }
            if (asc) {
                sb.append(" ");
                for (int y = 0; y < step; y++) {
                    if (t + y < length) {
                        int ch = data[baseOfs + t + y] & 0x00FF;
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
        for (int t = 0; t < length; t++) {
            int v = data[baseOfs + t] & 0x00FF;
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
            int decimalIndex = number.length() - digitsAfterDecimalPoint;
            number = trimLeadingZeros(number.substring(0, decimalIndex)) + "." + number.substring(decimalIndex);
        } else {
            number = trimLeadingZeros(number);
        }
        return (!sign.equals("0") ? "-" : "") + number;
    }

    /**
     * @param number a number string.
     * @return the number string without leading zeros.
     */
    private String trimLeadingZeros(String number) {
        int idx = -1;
        for (int t = 0; t < number.length(); t++) {
            if (number.charAt(t) == '0') {
                idx++;
            } else {
                break;
            }
        }
        if (number.length() == 0) {
            return "0";
        } else if (idx == number.length() - 1) {
            return number.substring(idx);
        } else {
            return number.substring(idx + 1);
        }
    }

}
