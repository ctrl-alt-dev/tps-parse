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

import nl.cad.tpsparse.tps.TpsRecord;
import nl.cad.tpsparse.tps.header.TableNameHeader;

public class TableNameRecord {

    private TableNameHeader hdr;
    private int tableNumber;

    public TableNameRecord(TpsRecord record) {
        this.hdr = (TableNameHeader) record.getHeader();
        this.tableNumber = record.getData().beLong();
    }

    public int getTableNumber() {
        return tableNumber;
    }

    @Override
    public String toString() {
        return "TableRecord(" + hdr.getName() + "," + getTableNumber() + ")";
    }

}
