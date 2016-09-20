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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.util.Utils;

/**
 * @author E.Hooijmeijer
 *
 */
public class BlockUtilities {

    /**
     * loads a file into 0x40 byte blocks.
     */
    public List<Block> loadFile(File in, boolean encrypted) throws IOException {
        List<Block> results = new ArrayList<>();
        RandomAccess rx = new RandomAccess(Utils.readFully(in));
        while (!rx.isAtEnd()) {
            results.add(new Block(rx, encrypted));
        }
        return results;
    }

    /**
     * finds blocks with the same (encrypted) content in the file.
     * TPS uses EBC mode, so identical encrypted blocks will map to the same plaintext.
     * This is useful because identical blocks are generally empty space 
     * whose plaintext contents are known in advance.
     */
    public NavigableMap<Block, List<Block>> findIdenticalBlocks(List<Block> blocks) {
        NavigableMap<Block, List<Block>> same = new TreeMap<>();
        List<Block> done = new ArrayList<>();
        for (int t = 0; t < blocks.size(); t++) {
            Block a = blocks.get(t);
            if (done.contains(a)) {
                continue;
            }
            done.add(a);
            for (int y = t; y < blocks.size(); y++) {
                Block b = blocks.get(y);
                if (a.sameValue(b)) {
                    if (done.contains(b)) {
                        continue;
                    }
                    if (!same.containsKey(a)) {
                        same.put(a, new ArrayList<>());
                    }
                    same.get(a).add(b);
                    done.add(b);
                }
            }
        }
        return same;
    }
}