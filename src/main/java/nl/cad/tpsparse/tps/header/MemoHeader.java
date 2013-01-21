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
package nl.cad.tpsparse.tps.header;

import nl.cad.tpsparse.bin.RandomAccess;

/**
 * Memo's are identified by 0xFC.
 */
public class MemoHeader extends AbstractHeader {

    private int owningRecord;
    private int sequenceNr;
    private int memoIndex;

    public MemoHeader(RandomAccess rx) {
        super(rx);
        isType(0xFC);
        this.owningRecord = rx.beLong();
        this.memoIndex = rx.beByte();
        this.sequenceNr = rx.beShort();
    }

    public int getOwningRecord() {
        return owningRecord;
    }

    public int getSequenceNr() {
        return sequenceNr;
    }

    public int getMemoIndex() {
        return memoIndex;
    }

    @Override
    public String toString() {
        return "Memo(" + getTableNumber() + "," + owningRecord + "," + memoIndex + "," + sequenceNr + ")";
    }

    public boolean isApplicable(int tableNr, int memoIdx) {
        if (tableNr != getTableNumber()) {
            return false;
        }
        if (memoIdx != getMemoIndex()) {
            return false;
        }
        return true;
    }

}
