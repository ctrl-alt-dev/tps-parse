/*
 *  Copyright 2016 E.Hooijmeijer
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

import org.junit.Test;

import nl.cad.tpsparse.bin.RandomAccess;

/**
 * @author E.Hooijmeijer
 *
 */
public class TpsBlockTest {

    @Test
    public void shouldReadTwoBlocks() {
        RandomAccess block = new RandomAccess(new byte[4 * 256]);
        block.setLeLong(0);
        block.setLeLong(0x0200);

        block.jumpAbs(0x0200);
        block.setLeLong(0x0200);
        block.setLeLong(0x0100);

        block.jumpAbs(0);
        TpsBlock data = new TpsBlock(block, 0, 0x0300, false);
        assertEquals(2, data.getPages().size());

        assertEquals(2, data.getPages().size());
        assertEquals(0x200, data.getPages().get(0).getPageSize());
        assertEquals(0x100, data.getPages().get(1).getPageSize());
    }

    @Test
    public void shouldReadTwoBlocksWithGap() {
        RandomAccess block = new RandomAccess(new byte[4 * 256]);
        block.setLeLong(0);
        block.setLeLong(0x0100);

        block.jumpAbs(0x0200);
        block.setLeLong(0x0200);
        block.setLeLong(0x0100);

        block.jumpAbs(0);
        TpsBlock data = new TpsBlock(block, 0, 0x0300, false);

        assertEquals(2, data.getPages().size());
        assertEquals(0x100, data.getPages().get(0).getPageSize());
        assertEquals(0x100, data.getPages().get(1).getPageSize());
    }

    @Test
    public void shouldSkipPartiallyOverwrittenBlock() {
        RandomAccess block = new RandomAccess(new byte[4 * 256]);
        block.setLeLong(0);
        block.setLeLong(0x0300);

        block.jumpAbs(0x0100); // Inside the previous block!
        block.setLeLong(0x0100);
        block.setLeLong(0x0200);

        block.jumpAbs(0);
        TpsBlock data = new TpsBlock(block, 0, 0x0300, false);
        assertEquals(1, data.getPages().size());

        assertEquals(1, data.getPages().size());
        assertEquals(0x100, data.getPages().get(0).getAddr());
        assertEquals(0x200, data.getPages().get(0).getPageSize());
    }

}
