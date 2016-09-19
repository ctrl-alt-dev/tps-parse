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

import org.junit.Test;

/**
 * @author E.Hooijmeijer
 */
public class BlockTest {

    @Test
    public void shouldCreate() {
        Block ba = new Block(0, new int[16], true);
        Block bb = new Block(10, ba);
        Block bc = new Block(ba);
        Block bd = ba.apply(0, 1, 1, 2);

        assertEquals(0, ba.getOffset());
        assertEquals(10, bb.getOffset());
        assertEquals(0, bc.getOffset());
        assertEquals(0, bd.getOffset());

        assertTrue(ba.isEncrypted());
        assertTrue(bb.isEncrypted());
        assertTrue(bc.isEncrypted());
        assertTrue(bd.isEncrypted());

        assertTrue(ba.equals(bc));
        assertTrue(bc.equals(ba));
        assertFalse(bb.equals(ba));
        assertFalse(bd.equals(ba));

        assertEquals(0, ba.getValues()[0]);
        assertEquals(1, bd.getValues()[0]);
        assertEquals(2, bd.getValues()[1]);

        assertEquals(-1, ba.compareTo(bd));
        assertEquals(1, bd.compareTo(ba));
    }
}
