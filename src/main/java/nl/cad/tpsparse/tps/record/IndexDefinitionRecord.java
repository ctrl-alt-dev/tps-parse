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

import java.util.ArrayList;
import java.util.List;

import nl.cad.tpsparse.bin.RandomAccess;

public class IndexDefinitionRecord {

    private String externalFile;
    private String name;
    private int flags;
    private int fieldsInKey;
    private int[] keyField;
    private int[] keyFieldFlag;

    public IndexDefinitionRecord(RandomAccess rx) {
        externalFile = rx.zeroTerminatedString();
        if (externalFile.length() == 0) {
            int read = rx.leByte();
            if (read != 1) {
                throw new IllegalArgumentException("Bad Index Definition, missing 0x01 after zero string (" + rx.toHex2(read) + ")");
            }
        }
        name = rx.zeroTerminatedString();
        flags = rx.leByte();
        fieldsInKey = rx.leShort();
        keyField = new int[fieldsInKey];
        keyFieldFlag = new int[fieldsInKey];
        for (int t = 0; t < fieldsInKey; t++) {
            keyField[t] = rx.leShort();
            keyFieldFlag[t] = rx.leShort();
        }
    }

    public String getName() {
        return name;
    }

    public int getFieldsInKey() {
        return fieldsInKey;
    }

    public List<FieldDefinitionRecord> getFieldRecords(TableDefinitionRecord rec) {
        List<FieldDefinitionRecord> results = new ArrayList<FieldDefinitionRecord>();
        for (int t = 0; t < keyField.length; t++) {
            results.add(rec.getFields().get(keyField[t]));
        }
        return results;
    }

    @Override
    public String toString() {
        return "IndexDefinition(" + externalFile + "," + name + "," + flags + "," + fieldsInKey + ")";
    }
}
