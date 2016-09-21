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
package nl.cad.tpsparse.keyrecovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import org.junit.Before;
import org.junit.Test;

/**
 * @author E.Hooijmeijer
 *
 */
public class BlockUtilitiesTest {

    private BlockUtilities blockUtilities;

    @Before
    public void init() {
        blockUtilities = new BlockUtilities();
    }

    @Test
    public void shouldLoadBlocks() throws IOException {
        List<Block> blocks = blockUtilities.loadFile(new File("./src/test/resources/enc/encrypted-a.tps"), true);
        assertEquals(24, blocks.size());

        NavigableMap<Block, List<Block>> out = blockUtilities.findIdenticalBlocks(blocks);

        assertFalse(out.isEmpty());
        assertEquals(4, out.size());
    }

    @Test
    public void shouldGetHeaderIndexEndBlock() throws IOException {
        List<Block> blocks = blockUtilities.loadFile(new File("./src/test/resources/enc/encrypted-a.tps"), true);

        Block crypt = blockUtilities.getHeaderIndexEndBlock(blocks, true);
        Block plain = blockUtilities.getHeaderIndexEndBlock(blocks, false);

        assertEquals(0x1c0, crypt.getOffset());
        assertEquals(0x1c0, plain.getOffset());

        assertEquals(0x925cdcb4, crypt.getValues()[0]);
        assertEquals(0x00000004, plain.getValues()[0]);
    }

    @Test
    public void shouldGetSequenceBlock() {
        Block sequenceBlock = blockUtilities.generateSequenceBlock(0x5F5E5D5C);
        assertEquals(0x23222120, sequenceBlock.getValues()[0]);
        assertEquals(0x27262524, sequenceBlock.getValues()[1]);
        assertEquals(0x5B5A5958, sequenceBlock.getValues()[14]);
        assertEquals(0x5F5E5D5C, sequenceBlock.getValues()[15]);
    }

    @Test
    public void shouldGetSequenceBlockWithCycle() {
        Block sequenceBlock = blockUtilities.generateSequenceBlock(0x1F1E1D1C);
        assertEquals(0xE3E2E1E0, sequenceBlock.getValues()[0]);
        assertEquals(0xE7E6E5E4, sequenceBlock.getValues()[1]);
        assertEquals(0x1B1A1918, sequenceBlock.getValues()[14]);
        assertEquals(0x1F1E1D1C, sequenceBlock.getValues()[15]);
    }

    @Test
    public void shouldFindNoIdenticalBlocks() {
        List<Block> in = new ArrayList<>();
        in.add(new Block(0, new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, true));
        in.add(new Block(1, new int[] { 2, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, true));
        in.add(new Block(2, new int[] { 3, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, true));
        in.add(new Block(3, new int[] { 4, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, true));
        in.add(new Block(4, new int[] { 5, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, true));

        NavigableMap<Block, List<Block>> out = blockUtilities.findIdenticalBlocks(in);

        assertTrue(out.isEmpty());
    }

    @Test
    public void shouldFindIdenticalBlocks() {
        List<Block> in = new ArrayList<>();
        in.add(new Block(0, new int[16], true));
        in.add(new Block(1, new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, true));
        in.add(new Block(2, new int[16], true));
        in.add(new Block(3, new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 14 }, true));
        in.add(new Block(4, new int[16], true));

        NavigableMap<Block, List<Block>> out = blockUtilities.findIdenticalBlocks(in);

        assertFalse(out.isEmpty());
        assertEquals(1, out.size());
        assertEquals(2, out.get(out.firstKey()).size());
        assertEquals(in.get(0), out.firstKey());
    }

    @Test
    public void shouldFindMultipleIdenticalBlocks() {
        List<Block> in = new ArrayList<>();
        in.add(new Block(0, new int[16], true));
        in.add(new Block(1, new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, true));
        in.add(new Block(2, new int[16], true));
        in.add(new Block(3, new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, true));
        in.add(new Block(4, new int[16], true));

        NavigableMap<Block, List<Block>> out = blockUtilities.findIdenticalBlocks(in);

        assertFalse(out.isEmpty());
        assertEquals(2, out.size());
        assertEquals(2, out.get(out.firstKey()).size());
        assertEquals(1, out.get(out.lastKey()).size());
        assertEquals(in.get(0), out.firstKey());
        assertEquals(in.get(1), out.lastKey());
    }

}
