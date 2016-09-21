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
	 * finds blocks with the same (encrypted) content in the file. TPS uses EBC
	 * mode, so identical encrypted blocks will map to the same plaintext. This
	 * is useful because identical blocks are generally empty space whose
	 * plaintext contents are known in advance.
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

	/**
	 * The Header Index End block is one of the few blocks in TPS with contents
	 * that are predictable and is located at a fixed location. Typically it is
	 * filled with identical values (the size of the file, minus the size of the
	 * header and right shifted for 8 bits.
	 * 
	 * @param file the full file.
	 * @param encrypted if you want the encrypted block or the plaintext.
	 * @return the block.
	 */
	public Block getHeaderIndexEndBlock(List<Block> file, boolean encrypted) {
		Block block = null;
		if (encrypted) {
			block = file.get(0x1C0 / 0x40);
			if (block.getOffset() != 0x1C0) {
				throw new IllegalArgumentException();
			}
		} else {
			int value = (file.get(file.size() - 1).getOffset() + 0x100 - 0x200) >>> 8;
			int[] values = new int[16];
			for (int t = 0; t < values.length; t++) {
				values[t] = value;
			}
			block = new Block(0x01C0, values, false);
		}
		return block;
	}
}