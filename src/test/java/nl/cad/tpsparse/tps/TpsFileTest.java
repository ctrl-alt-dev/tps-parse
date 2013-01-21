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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import nl.cad.tpsparse.tps.record.DataRecord;
import nl.cad.tpsparse.tps.record.FieldDefinitionRecord;
import nl.cad.tpsparse.tps.record.IndexRecord;
import nl.cad.tpsparse.tps.record.TableDefinitionRecord;

import org.junit.Before;
import org.junit.Test;

/**
 * TpsFileTest.
 */
public class TpsFileTest {

    private TpsFile file;

    @Before
    public void init() throws IOException {
        file = new TpsFile(TpsHeaderTest.class.getResourceAsStream("/table.tps"));
    }

    @Test
    public void shouldParseFile() throws IOException {
        List<TpsRecord> records = file.getAllRecords();
        assertEquals(10, records.size());
    }

    @Test
    public void shouldParseTableMetadata() {
        assertEquals(1, file.getTableNameRecords().size());
        Map<Integer, TableDefinitionRecord> tableDefinitions = file.getTableDefinitions(false);
        assertEquals(1, tableDefinitions.size());
        assertEquals(2, tableDefinitions.get(1).getFields().size());
        assertEquals(2, tableDefinitions.get(1).getIndexes().size());
        assertEquals(0, tableDefinitions.get(1).getMemos().size());
    }

    @Test
    public void shouldParseTableFieldInfo() {
        Map<Integer, TableDefinitionRecord> tableDefinitions = file.getTableDefinitions(false);
        List<FieldDefinitionRecord> fields = tableDefinitions.get(1).getFields();
        assertEquals("CON1:OUDNR", fields.get(0).getFieldName());
        assertEquals("OUDNR", fields.get(0).getFieldNameNoTable());
        assertEquals("SIGNED-SHORT", fields.get(0).getFieldTypeName());
        assertEquals("CON1:NEWNR", fields.get(1).getFieldName());
        assertEquals("NEWNR", fields.get(1).getFieldNameNoTable());
        assertEquals("SIGNED-SHORT", fields.get(1).getFieldTypeName());
    }

    @Test
    public void shouldParseRecord() {
        Map<Integer, TableDefinitionRecord> tableDefinitions = file.getTableDefinitions(false);
        List<DataRecord> dataRecords = file.getDataRecords(1, tableDefinitions.get(1), false);
        assertEquals(1, dataRecords.size());
        assertEquals(2, dataRecords.get(0).getRecordNumber());
        assertEquals(2, dataRecords.get(0).getValues().size());
        assertEquals(Integer.valueOf(1), dataRecords.get(0).getValues().get(0));
        assertEquals(Integer.valueOf(1), dataRecords.get(0).getValues().get(1));
    }

    @Test
    public void shouldParseIndexData() {
        List<IndexRecord> indexes = file.getIndexes(1, 0);
        assertEquals(1, indexes.size());
        assertEquals(2, indexes.get(0).getRecordNumber());
        //
        indexes = file.getIndexes(1, 1);
        assertEquals(1, indexes.size());
        assertEquals(2, indexes.get(0).getRecordNumber());
    }
}
