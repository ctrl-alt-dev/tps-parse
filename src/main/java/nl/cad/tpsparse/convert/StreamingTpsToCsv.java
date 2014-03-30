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

import nl.cad.tpsparse.csv.CsvWriter;
import nl.cad.tpsparse.tps.TpsFile;
import nl.cad.tpsparse.tps.TpsFile.Visitor;
import nl.cad.tpsparse.tps.TpsRecord;
import nl.cad.tpsparse.tps.header.DataHeader;
import nl.cad.tpsparse.tps.record.DataRecord;
import nl.cad.tpsparse.tps.record.MemoRecord;
import nl.cad.tpsparse.tps.record.TableDefinitionRecord;
import nl.cad.tpsparse.util.Utils;

/**
 * Streaming Tps to Csv converter that doesn't consume as much memory. It
 * doesn't sort or check for duplicates though. It still buffers memo's.
 * @author E.Hooijmeijer
 */
public class StreamingTpsToCsv extends AbstractTpsToCsv {

    public StreamingTpsToCsv(File tpsFile, File csvFile, CsvWriter csv, TpsFile tps, Map.Entry<Integer, TableDefinitionRecord> table) {
        super(tpsFile, csvFile, csv, tps, table);
    }

    @Override
    public void run() {
        //
        buildCsvHeaders();
        //
        processRecords(prefetchMemos());
        //
    }

    protected void processRecords(final List<List<MemoRecord>> memos) {
        if (isVerbose()) {
            System.out.println("Processing records");
            System.out.println("Memory: " + Utils.reportMemoryUsage());
        }
        getTpsFile().visit(new Visitor() {
            @Override
            public void onTpsRecord(TpsRecord record) {
                if (record.getHeader() instanceof DataHeader) {
                    if (record.getHeader().getTableNumber() == getTableId()) {
                        DataRecord dataRecord = new DataRecord(record, getTable());
                        onRecord(memos, dataRecord);
                    }
                }
            }
        }, isIgnoreErrors());
    }

}
