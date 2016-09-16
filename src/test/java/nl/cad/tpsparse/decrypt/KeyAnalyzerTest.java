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
package nl.cad.tpsparse.decrypt;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import nl.cad.tpsparse.decrypt.Key;
import nl.cad.tpsparse.decrypt.KeyAnalyzer;

public class KeyAnalyzerTest {

	@Test
	public void shouldGroupKeyAsOne() {
		Key k = new Key("a").init();
		KeyAnalyzer ka = new KeyAnalyzer();
		List<List<Integer>> groups = ka.getSwapGroups(k);
		assertEquals("[[0, 15, 14, 13, 11, 10, 9, 8, 12, 7, 6, 5, 4, 3, 2, 1]]", groups.toString());
	}

	@Test
	public void shouldGroupKey() {
		Key k = new Key("aaa").init();
		KeyAnalyzer ka = new KeyAnalyzer();
		List<List<Integer>> groups = ka.getSwapGroups(k);
		assertEquals("[[0, 12, 10, 8, 7, 6, 13, 5, 4, 15, 14, 11, 9, 3], [1], [2]]", groups.toString());
	}

	@Test
	public void shouldGroupKeyAnother() {
		Key k = new Key("12345678").init();
		KeyAnalyzer ka = new KeyAnalyzer();
		List<List<Integer>> groups = ka.getSwapGroups(k);
		assertEquals("[[0, 8, 13, 12, 15, 7, 9], [1], [2, 14], [3, 4, 11, 10, 6, 5]]", groups.toString());
	}

	@Test
	public void shouldRenderMatrix() {
		Key k = new Key("12345678").init();
		KeyAnalyzer ka = new KeyAnalyzer();
		assertNotNull(ka.renderKeySwapMatrix(k));
	}

	@Test
	public void shouldGetDependencies() {
		Key k = new Key("1").init();
		KeyAnalyzer ka = new KeyAnalyzer();
		assertEquals(
				"[[[V-f], [V-e], [V-d], [V-b], [V-a], [V-9], [V-8], [V-7], [V-6], [V-5], [V-4], [V-3], [V-2], [V-1]], [[H-0]], [[H-0]], [[H-0]], [[H-0]], [[H-0]], [[H-0]], [[H-0]], [[V-c], [H-0]], [[H-0]], [[H-0]], [[H-0]], [[H-8]], [[H-0]], [[H-0]], [[H-0]]]",
				ka.getDependencies(k).toString());
	}
}
