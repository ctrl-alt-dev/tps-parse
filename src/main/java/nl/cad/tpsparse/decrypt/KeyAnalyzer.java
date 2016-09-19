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

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Determines the swap groups in a key, useful for static analysis. 
 */
public class KeyAnalyzer {

	/**
	 * Navigational direction.
	 */
	public static enum Direction {
		HOR, VERT;
	}

	/**
	 * Holds a dependency between two key columns. 
	 */
	public static class Dependency implements Comparable<Dependency> {
		private Direction dir;
		private int column;
		private int linked;

		public Dependency(Direction dir, int column, int linked) {
			this.dir = dir;
			this.column = column;
			this.linked = linked;
		}

		@Override
		public int compareTo(Dependency o) {
			int dif = o.column - this.column;
			if (dif == 0) {
				dif = this.dir.ordinal() - o.dir.ordinal();
			}
			return dif;
		}

		@Override
		public String toString() {
			return "[" + dir.name().substring(0, 1) + "-" + Integer.toHexString(column) + "]";
		}

		public boolean isHorizontal() {
			return Direction.HOR == dir;
		}

		public int getLinked() {
			return linked;
		}

	}

	private static class DependencyMatrix {
		
		private List<NavigableSet<Dependency>> depends = new ArrayList<NavigableSet<Dependency>>();

		public DependencyMatrix(int[] key) {
			for (int t = 0; t <= 15; t++) {
				depends.add(new TreeSet<Dependency>());
			}
			for (int t = 0; t <= 15; t++) {
				int a = t;
				int b = key[t] & 0x0F;
				if (a != b) {
					depends.get(a).add(new Dependency(Direction.HOR, b, a));
					depends.get(b).add(new Dependency(Direction.VERT, a, b));
				}
			}
		}

		public boolean isDone(int column) {
			return depends.get(column) == null;
		}

		/**
		 * @return true if this column is only dependent on its self.
		 */
		public boolean isSelfRef(int column) {
			return !isDone(column) && depends.get(column).isEmpty();
		}

		/**
		 * @return true if this column is only dependent one other column that
		 *         does not have any further deps.
		 */
		public boolean isPairedRef(int column) {
			if (isDone(column) || isSelfRef(column)) {
				return false;
			}
			NavigableSet<Dependency> dep = depends.get(column);
			if (dep.size() == 1) {
				return depends.get(dep.first().column).size() == 1;
			} else {
				return false;
			}
		}

		public int depth(int col) {
			if (isDone(col)) {
				throw new IllegalArgumentException();
			} else if (isSelfRef(col)) {
				return 1;
			} else if (isPairedRef(col)) {
				return 2;
			} else {
				ArrayList<Integer> did = new ArrayList<Integer>();
				did.add(col);
				for (Dependency d : depends.get(col)) {
					depth(d, did);
				}
				return did.size();
			}
		}
		
		public List<Integer> groupedColumns(int col) {
			ArrayList<Integer> did = new ArrayList<Integer>();
			did.add(col);
			for (Dependency d : depends.get(col)) {
				depth(d, did);
			} 
			return did;
		}

		private int depth(Dependency src, List<Integer> did) {
			if (did.contains(src.column)) {
				return did.size();
			}
			did.add(src.column);
			for (Dependency d:depends.get(src.column)) {
				depth(d, did);
			}
			return did.size();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int t = 0; t <= 15; t++) {
				sb.append(Integer.toHexString(t) + " : " + depends.get(t) +" "+depth(t)+ " \n");
			}
			return sb.toString();
		}

		public boolean isSinglePassRecoverable() {
			for (int t=15;t>=0;t--) {
				for (Dependency d:depends.get(t)) {
					if ((d.linked < d.column) && (d.isHorizontal())) {
						return false;
					}
				}
			}
			return true;
		}

	}
	
	/**
	 * returns the groups of key columns that are dependent on each other. 
	 * @param key the key
	 * @return the swap groups.
	 */
	public List<List<Integer>> getSwapGroups(Key key) {
		List<List<Integer>> results = new ArrayList<List<Integer>>();
		DependencyMatrix matrix = new DependencyMatrix(key.toIntArray());
		Set<Integer> did = new TreeSet<Integer>();
		for (int t=0;t<16;t++) {
			if (!did.contains(t)) {
				List<Integer> group = matrix.groupedColumns(t);
				did.addAll(group);
				results.add(group);
			}
		}
		return results;
	}
	
	/**
	 * @param key the key.
	 * @return the dependencies of each word of the key.
	 */
	public List<NavigableSet<Dependency>> getDependencies(Key key) {
		return new DependencyMatrix(key.toIntArray()).depends;
	}

	/**
	 * Some keys columns only swap with key columns that have a lesser index
	 * than themselves. If this is true for all columns in a key the key
	 * can be recovered in a single pass, one column at a time. This is
	 * true for about 0.1 % of the keys. Keys generated with short passwords
	 * or passwords with odd lengths have better odds. 
	 * @param key the key.
	 * @return true if there are no interdependencies between key columns.
	 */
	public boolean isSinglePassRecoverable(Key key) {
		return new DependencyMatrix(key.toIntArray()).isSinglePassRecoverable();
	}

	
	/**
	 * renders the key swap matrix to a string.
	 * @param key the key.
	 * @return the swaps.
	 */
	public String renderKeySwapMatrix(Key key) {
		StringBuilder sb=new StringBuilder();
		sb.append("  ");
		for (int t = 0x00; t < 0x10; t++) {
			sb.append(Integer.toHexString(t));
			sb.append(" ");
		}
		sb.append("\n");
		for (int t = 0x0F; t >= 0x00; t--) {
			int shuffle = key.getWord(t) & 0x0F;
			sb.append(Integer.toHexString(t));
			sb.append(" ");
			sb.append(sshuf(t, shuffle));
			sb.append("\n");
		}
		return sb.toString();
	}

	private String sshuf(int idx, int shuffle) {
		StringBuilder sb = new StringBuilder();
		for (int t = 0; t <= 0x0F; t++) {
			if ((t == idx) && (t == shuffle)) {
				sb.append("* ");
			} else if (t == idx) {
				sb.append("x ");
			} else if (t == shuffle) {
				sb.append("+ ");
			} else {
				sb.append(". ");
			}
		}
		return sb.toString();
	}
	
}
