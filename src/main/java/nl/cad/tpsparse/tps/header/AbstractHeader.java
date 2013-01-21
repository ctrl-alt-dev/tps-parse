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

public class AbstractHeader {

    private int tableNumber;
    private int type;

    public AbstractHeader(RandomAccess rx) {
        this(rx, true);
    }

    public AbstractHeader(RandomAccess rx, boolean readTable) {
        if (readTable) {
            this.tableNumber = rx.beLong();
        }
        this.type = rx.leByte();
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public final int getType() {
        return type;
    }

    public void isType(int expected) {
        if (getType() != expected) {
            throw new IllegalArgumentException("Header is not of expected type " + expected + " but " + type);
        }
    }
}
