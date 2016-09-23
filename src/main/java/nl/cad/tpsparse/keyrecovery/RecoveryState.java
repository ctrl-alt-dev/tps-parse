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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeSet;

import nl.cad.tpsparse.decrypt.Key;

/**
 * @author E.Hooijmeijer
 */
public class RecoveryState implements Comparable<RecoveryState> {

    private static final BlockUtilities util = new BlockUtilities();

    private RecoveryState parent;

    private PartialKey key;

    private Block encryptedHeaderBlock;
    private Block plaintextHeaderBlock;

    private List<Block> b0b0Blocks;

    private List<Block> sequentialBlocks;

    private RecoveryState(Block encryptedHeaderBlock, Block plaintextHeaderBlock) {
        this.key = new PartialKey();
        this.encryptedHeaderBlock = encryptedHeaderBlock;
        this.plaintextHeaderBlock = plaintextHeaderBlock;
        this.b0b0Blocks = new ArrayList<>();
        this.sequentialBlocks = new ArrayList<>();
    }

    private RecoveryState(RecoveryState state) {
        this.parent = state;
        this.key = state.key;
        this.encryptedHeaderBlock = state.encryptedHeaderBlock;
        this.plaintextHeaderBlock = state.plaintextHeaderBlock;
        this.b0b0Blocks = new ArrayList<>(state.b0b0Blocks);
        this.sequentialBlocks = new ArrayList<>(state.sequentialBlocks);
    }

    private RecoveryState(RecoveryState state, PartialKey key, Block partiallyDecrypted) {
        this.parent = state;
        this.key = key;
        this.encryptedHeaderBlock = partiallyDecrypted;
        this.plaintextHeaderBlock = state.plaintextHeaderBlock;
        this.b0b0Blocks = new ArrayList<>(state.b0b0Blocks);
        this.sequentialBlocks = new ArrayList<>(state.sequentialBlocks);
    }

    public static RecoveryState start(Block encryptedHeaderBlock, Block plaintextHeaderBlock) {
        return new RecoveryState(encryptedHeaderBlock, plaintextHeaderBlock);
    }

    /**
     * Scans the keyIndex to find a value that decrypts the header block.
     * Takes all 64 bits into consideration. 
     * @param keyIdx the index to scan.
     * @return any solutions (many, with false positives).
     */
    public List<RecoveryState> indexScan(int keyIdx) {
        NavigableMap<PartialKey, Block> results = key.keyIndexScan(keyIdx, encryptedHeaderBlock, plaintextHeaderBlock);
        List<RecoveryState> result = new ArrayList<>();
        for (Map.Entry<PartialKey, Block> entry : results.entrySet()) {
            result.add(new RecoveryState(this, entry.getKey(), entry.getValue()));
        }
        return result;
    }
    
    /**
     * Scans the keyIndex to find any value that has a swap of the same index that decrypts the header block.
     * So, it scans 60 of the index's 64 bits.
     * Note that this does NOT indicate that the value found is correct (although it might be) but
     * merely indicates that this column swaps with itself. This happens a lot with columns 0 and 8
     * that are popular targets to swap to and also like to swap with themselves.    
     * @param keyIdx the index to scan.
     * @return any solutions (typically just 1).
     */
    public List<RecoveryState> indexSelfScan(int keyIdx) {
        NavigableMap<PartialKey, Block> results = key.keyIndexSelfScan(keyIdx, encryptedHeaderBlock, plaintextHeaderBlock);
        List<RecoveryState> result = new ArrayList<>();
        for (Map.Entry<PartialKey, Block> entry : results.entrySet()) {
            result.add(new RecoveryState(this, entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * reduces the number of solutions by attempting to decrypting blocks with duplicates to 0xB0B0B0B0 blocks. 
     * If at least one of those blocks is found the solution is kept.  
     * The blocks that decrypt to 0xB0B0 are saved in that keys recovery state for further decryption.
     * @param candidates the candidate solutions.
     * @param idx the index to evaluate.
     * @param blocks the blocks to find duplicates in.
     * @return the remaining candidate solutions.
     */
    public static List<RecoveryState> reduceFirstB0B0(List<RecoveryState> candidates, int idx, List<Block> blocks) {
        List<RecoveryState> results = new ArrayList<>();
        NavigableMap<Block, List<Block>> same = util.findIdenticalBlocks(blocks);
        for (RecoveryState tmp : candidates) {
            List<Block> b0b0s = new ArrayList<>();
            for (Block s : same.keySet()) {
                Block r = tmp.key.partialDecrypt(idx, s);
                if (util.isB0B0Part(r.getValues()[idx])) {
                    b0b0s.add(r);
                }
            }
            if (!b0b0s.isEmpty()) {
                RecoveryState recovery = new RecoveryState(tmp);
                recovery.b0b0Blocks.clear();
                recovery.b0b0Blocks.addAll(b0b0s);
                results.add(recovery);
            }
        }
        Collections.sort(results);
        return results;
    }

    /**
     * reduces the number of solutions by re-evaluating the found 0xB0B0 blocks to this index.
     * If at least one block still decrypts to 0xB0B0 the candidate is kept.
     * @param candidates the candidate solutions.
     * @param idx the index to evaluate.
     * @return the remaining candidates.
     */
    public static List<RecoveryState> reduceB0B0(List<RecoveryState> candidates, int idx) {
        List<RecoveryState> results = new ArrayList<>();
        for (RecoveryState tmp : new TreeSet<>(candidates)) {
            List<Block> b0b0s = new ArrayList<>();
            for (Block s : tmp.b0b0Blocks) {
                Block r = tmp.key.partialDecrypt(idx, s);
                if (util.isB0B0Part(r.getValues()[idx])) {
                    b0b0s.add(r);
                }
            }
            if (!b0b0s.isEmpty()) {
                RecoveryState recovery = new RecoveryState(tmp);
                recovery.b0b0Blocks.clear(); // previous B0B0's
                recovery.b0b0Blocks.addAll(b0b0s);
                results.add(recovery);
            }
        }
        Collections.sort(results);
        return results;
    }

    /**
     * reduces the number of candidate solutions by finding any blocks that decrypt to a sequence (like 0x40414243).
     * Blocks that are found to hold a sequence are added to the recovery state for further evaluation.
     * @param candidates the candidates.
     * @param idx the index to evaluate.
     * @param blocks the blocks that may contain sequences.
     * @return the remaining candidates.
     */
    public static List<RecoveryState> reduceFirstSequential(List<RecoveryState> candidates, int idx, List<Block> blocks) {
        List<RecoveryState> results = new ArrayList<>();
        for (RecoveryState tmp : new TreeSet<>(candidates)) {
            List<Block> seqs = new ArrayList<>();
            for (Block s : blocks) {
                Block r = tmp.key.partialDecrypt(idx, s);
                if (util.isSequencePart(r.getValues()[idx])) {
                    seqs.add(r);
                }
            }
            if (!seqs.isEmpty()) {
                RecoveryState recovery = new RecoveryState(tmp);
                recovery.sequentialBlocks.clear();
                recovery.sequentialBlocks.addAll(seqs);
                results.add(recovery);
            }
        }
        Collections.sort(results);
        return results;
    }

    /**
     * reduces the number of candidate solutions by finding any blocks that decrypt to a sequence (like 0x40414243).
     * @param candidates the candidates.
     * @param idx the index to evaluate.
     * @return the remaining candidates.
     */
    public static List<RecoveryState> reduceSequential(List<RecoveryState> candidates, int idx) {
        List<RecoveryState> results = new ArrayList<>();
        for (RecoveryState tmp : new TreeSet<>(candidates)) {
            List<Block> seqs = new ArrayList<>();
            for (Block s : tmp.sequentialBlocks) {
                Block r = tmp.key.partialDecrypt(idx, s);
                if (util.isSequencePart(r.getValues()[idx])) {
                    seqs.add(r);
                }
            }
            if (!seqs.isEmpty()) {
                RecoveryState recovery = new RecoveryState(tmp);
                recovery.sequentialBlocks.clear();
                recovery.sequentialBlocks.addAll(seqs);
                results.add(recovery);
            }
        }
        Collections.sort(results);
        return results;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RecoveryState) {
            RecoveryState cmp = (RecoveryState) obj;
            return cmp.key.equals(this.key) && cmp.b0b0Blocks.size() == this.b0b0Blocks.size() && cmp.sequentialBlocks.size() == this.sequentialBlocks.size();
        } else {
            return false;
        }
    }

    public RecoveryState getParent() {
        return parent;
    }

    @Override
    public int hashCode() {
        return key.hashCode() + 31 * b0b0Blocks.size() + 31 * 31 * sequentialBlocks.size();
    }

    @Override
    public int compareTo(RecoveryState o) {
        int cmp = o.b0b0Blocks.size() - this.b0b0Blocks.size();
        if (cmp == 0) {
            cmp = o.sequentialBlocks.size() - this.sequentialBlocks.size();
            if (cmp == 0) {
                cmp = this.key.compareTo(o.key);
            }
        }
        return cmp;
    }

    @Override
    public String toString() {
        return "RecoveryState(" + b0b0Blocks.size() + "," + sequentialBlocks.size() + "," + key.toString() + ")";
    }

    public Key getKey() {
        return key.toKey();
    }

}
