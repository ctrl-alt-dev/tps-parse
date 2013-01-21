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

public class FieldDefinitionRecord {

    private int fieldType;
    private int offset;
    private String fieldName;
    private int elements;
    private int length;
    private int flags;
    private int index;

    private int stringLength;
    private String stringMask;

    private int bcdDigitsAfterDecimalPoint;
    private int bcdLengthOfElement;

    public FieldDefinitionRecord(RandomAccess rx) {
        this.fieldType = rx.leByte();
        this.offset = rx.leShort();
        this.fieldName = rx.zeroTerminatedString();
        this.elements = rx.leShort();
        this.length = rx.leShort();
        this.flags = rx.leShort();
        this.index = rx.leShort();
        //
        switch (fieldType) {
        case 0x0a:
            bcdDigitsAfterDecimalPoint = rx.leByte();
            bcdLengthOfElement = rx.leByte();
            break;
        case 0x12:
        case 0x13:
        case 0x14:
            stringLength = rx.leShort();
            stringMask = rx.zeroTerminatedString();
            if (stringMask.length() == 0) {
                rx.leByte();
            }
            break;
        }
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public int getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldNameNoTable() {
        int idx = fieldName.indexOf(':');
        if (idx >= 0) {
            return fieldName.substring(idx + 1);
        }
        return fieldName;
    }

    public int getBcdDigitsAfterDecimalPoint() {
        return bcdDigitsAfterDecimalPoint;
    }

    public int getBcdLengthOfElement() {
        return bcdLengthOfElement;
    }

    public int getStringLength() {
        return stringLength;
    }

    public String getStringMask() {
        return stringMask;
    }

    @Override
    public String toString() {
        return "Field(#" + index + ",T:" + fieldType + ",OFS:" + offset + ",LEN:" + length + "," + fieldName + "," + elements + "," + flags + ")";
    }

    public String getFieldTypeName() {
        switch (fieldType) {
        case 1:
            return "BYTE";
        case 2:
            return "SIGNED-SHORT";
        case 3:
            return "UNSIGNED-SHORT";
        case 4:
            return "DATE";
        case 5:
            return "TIME";
        case 6:
            return "SIGNED-LONG";
        case 7:
            return "UNSIGNED-LONG";
        case 8:
            return "Float";
        case 9:
            return "Double";
        case 0x0A:
            return "BCD";
        case 0x12:
            return "fixed-length STRING";
        case 0x13:
            return "zero-terminated STRING";
        case 0x14:
            return "pascal STRING";
        case 0x16:
            return "GROUP";
        default:
            return "unknown";
        }
    }

    /**
     * checks if this field is a group field. Group fields
     * are overlays on top of existing fields and as such
     * may contain text or binary.
     * @return true if this field is a group field.
     */
    public boolean isGroup() {
        return fieldType == 0x16;
    }

    /**
     * checks if this field fits in the given group field.
     * @param group the group field.
     * @return true if it fits.
     */
    public boolean isInGroup(FieldDefinitionRecord group) {
        return ((group.getOffset() <= offset) && ((group.getOffset() + group.getLength()) >= (offset + length)));
    }

}
