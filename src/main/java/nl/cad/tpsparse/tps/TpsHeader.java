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
package nl.cad.tpsparse.tps;

import nl.cad.tpsparse.bin.RandomAccess;

/**
 * TpsHeader represents the first 0x200 bytes of a TPS file.
 * Aside from the 'tOpS' identifier, it holds an array of page addresses and other meta information.  
 * @author E.Hooijmeijer
 */
public class TpsHeader {

    private int addr;
    private int hdrSize;
    private int fileLength1;
    private int fileLength2;
    private String topSpeed;
    private int zeros;
    private int lastIssuedRow;
    private int changes;
    private int managementPageRef;

    private RandomAccess rx;

    private int[] pageStart;
    private int[] pageEnd;

    /**
     * 
     * @param rx
     */
    public TpsHeader(RandomAccess rx) {
        this.rx = rx;
        addr = rx.leLong();
        if (addr != 0) {
            throw new NotATopSpeedFileException("File doesn't start with 0x00000000 - its not a TopSpeed file or it may be 'encrypted'.");
        }
        hdrSize = rx.leShort();
        RandomAccess header = rx.read(hdrSize - 6);
        fileLength1 = header.leLong();
        fileLength2 = header.leLong();
        topSpeed = header.fixedLengthString(4);
        zeros = header.leShort();
        lastIssuedRow = header.beLong();
        changes = header.leLong();
        managementPageRef = header.toFileOffset(header.leLong());
        //
        pageStart = header.toFileOffset(header.leLongArray((0x110 - 0x20) / 4));
        pageEnd = header.toFileOffset(header.leLongArray((0x200 - 0x110) / 4));
    }

    public boolean isTopSpeedFile() {
        return "tOpS".equals(topSpeed);
    }

    public int[] getPageEnd() {
        return pageEnd;
    }

    public int[] getPageStart() {
        return pageStart;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TpsHeader(" + rx.toHex8(addr) + "," + rx.toHex4(hdrSize) + "," + rx.toHex8(fileLength1) + "," + rx.toHex8(fileLength2) + "," + topSpeed
                + "," + rx.toHex4(zeros) + "," + rx.toHex8(lastIssuedRow) + "," + rx.toHex8(changes) + "," + rx.toHex8(managementPageRef) + ")\n");
        for (int t = 0; t < pageStart.length; t++) {
            sb.append(rx.toHex8(pageStart[t]) + ".." + rx.toHex8(pageEnd[t]) + "\n");
        }
        return sb.toString();
    }

    public int getAddr() {
        return addr;
    }

    public int getHdrSize() {
        return hdrSize;
    }

    public int getFileLength1() {
        return fileLength1;
    }

    public int getFileLength2() {
        return fileLength2;
    }

    public String getTopSpeed() {
        return topSpeed;
    }

    public int getZeros() {
        return zeros;
    }

    public int getLastIssuedRow() {
        return lastIssuedRow;
    }

    public int getChanges() {
        return changes;
    }

    public int getManagementPageRef() {
        return managementPageRef;
    }

}
