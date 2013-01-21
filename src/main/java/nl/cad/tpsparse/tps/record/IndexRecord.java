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

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.tps.TpsRecord;
import nl.cad.tpsparse.tps.header.IndexHeader;

public class IndexRecord {

    private IndexHeader hdr;
    private RandomAccess data;
    private int recordNumber;

    public IndexRecord(TpsRecord record) {
        this.hdr = (IndexHeader) record.getHeader();
        data = record.getData();
        data.jumpAbs(data.length() - 4);
        recordNumber = data.beLong();
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    @Override
    public String toString() {
        return "IndexRecord(" + hdr.getTableNumber() + "," + hdr.getIndexNumber() + "," + recordNumber + ")";
    }

}
