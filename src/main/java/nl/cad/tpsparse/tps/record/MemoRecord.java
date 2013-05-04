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
package nl.cad.tpsparse.tps.record;

import java.nio.charset.Charset;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.tps.header.AbstractHeader;
import nl.cad.tpsparse.tps.header.MemoHeader;

public class MemoRecord {

    private MemoHeader header;
    private RandomAccess data;

    public MemoRecord(AbstractHeader abstractHeader, RandomAccess data) {
        this.header = (MemoHeader) abstractHeader;
        this.data = data;
    }

    @Override
    public String toString() {
        return header.getOwningRecord() + " : " + data.toAscii();
    }

    public int getOwner() {
        return header.getOwningRecord();
    }

    public String getDataAsMemo() {
        return new String(data.data(), Charset.forName("ISO-8859-1"));
    }

    public byte[] getDataAsBlob() {
        int length = data.leLong();
        return data.readBytes(length);
    }

    public RandomAccess getData() {
        return data;
    }

}
