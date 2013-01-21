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

import java.util.List;

import nl.cad.tpsparse.tps.TpsRecord;
import nl.cad.tpsparse.tps.header.DataHeader;

public class DataRecord {

    private DataHeader header;
    private TableDefinitionRecord tableDef;
    private List<Object> values;
    private TpsRecord record;

    public DataRecord(TpsRecord record, TableDefinitionRecord tableDef) {
        this.record = record;
        this.header = (DataHeader) record.getHeader();
        this.tableDef = tableDef;
        this.values = tableDef.parse(record.getData().remainder());
    }

    public TpsRecord getRecord() {
        return record;
    }

    public TableDefinitionRecord getTableDef() {
        return tableDef;
    }

    public int getRecordNumber() {
        return header.getRecordNumber();
    }

    public List<Object> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return header.getRecordNumber() + " : " + values;
    }

}
