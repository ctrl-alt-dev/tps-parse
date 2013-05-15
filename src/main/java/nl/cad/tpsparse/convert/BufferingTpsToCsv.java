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
package nl.cad.tpsparse.convert;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nl.cad.tpsparse.csv.CsvWriter;
import nl.cad.tpsparse.tps.TpsFile;
import nl.cad.tpsparse.tps.record.DataRecord;
import nl.cad.tpsparse.tps.record.MemoRecord;
import nl.cad.tpsparse.tps.record.TableDefinitionRecord;

/**
 * Performs TPS to CSV conversion by buffering everything into memory. These
 * records are automatically sorted and checked for duplicates.
 * 
 * @author E.Hooijmeijer
 */
public class BufferingTpsToCsv extends AbstractTpsToCsv {

    public BufferingTpsToCsv(File tpsFile, File csvFile, CsvWriter csv, TpsFile tps, Map.Entry<Integer, TableDefinitionRecord> table) {
        super(tpsFile, csvFile, csv, tps, table);
    }

    @Override
    public void run() {
        //
        buildCsvHeaders();
        //
        processRecords(prefetchMemos(), buildRecordsById());
        //
    }

    protected void processRecords(List<List<MemoRecord>> memos, Map<Integer, DataRecord> recordsById) {
        if (isVerbose()) {
            System.out.println("Converting " + recordsById.size() + " records to CSV");
        }
        for (Map.Entry<Integer, DataRecord> entry : recordsById.entrySet()) {
            DataRecord rec = entry.getValue();
            onRecord(memos, rec);
        }
    }

    protected Map<Integer, DataRecord> buildRecordsById() {
        if (isVerbose()) {
            System.out.println("Sorting records and checking for duplicates.");
        }
        Map<Integer, DataRecord> recordsById = new TreeMap<Integer, DataRecord>();
        //
        for (DataRecord rec : getTpsFile().getDataRecords(getTableId(), getTable(), isIgnoreErrors())) {
            int recordNumber = rec.getRecordNumber();
            if (!recordsById.containsKey(recordNumber)) {
                recordsById.put(recordNumber, rec);
            } else {
                System.err.println(getSourceFile().getName() + ": Duplicate record " + recordNumber);
            }
        }
        return recordsById;
    }

}
