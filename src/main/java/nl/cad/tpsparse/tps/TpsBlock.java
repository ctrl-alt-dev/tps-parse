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
package nl.cad.tpsparse.tps;

import java.util.ArrayList;
import java.util.List;

import nl.cad.tpsparse.bin.RandomAccess;
import nl.cad.tpsparse.bin.RunLengthEncodingException;

/**
 * A TpsBlock is the outermost container for data, it groups a number of TpsPages.
 * 
 * As far as I know a TpsBlock holds no information about the TpsPages it contains.
 * Currently the pages are identified by scanning for them.
 * 
 * The current algorithm works by beginning at the block start,
 * parsing the TpsPage and then seeking for the next TpsPage using its
 * offset in the file (always at a 0x0100 boundary and the value at that
 * address must have the same value as the offset). Far from perfect but
 * it seems to work.
 * 
 * @author E.Hooijmeijer
 */
public class TpsBlock {

    private List<TpsPage> pages = new ArrayList<TpsPage>();
    private int start;
    private int end;
    private RandomAccess rx;

    public TpsBlock(RandomAccess rx, int start, int end, boolean ignorePageErrors) {
        this.rx = rx;
        this.start = start;
        this.end = end;
        rx.pushPosition();
        rx.jumpAbs(start);
        try {
            // Some blocks are 0 length, they should be skipped.
            while (rx.position() < end) {
                if (isCompletePage()) {
                    try {
                        TpsPage page = new TpsPage(rx);
                        pages.add(page);
                    } catch (RunLengthEncodingException ex) {
                        if (ignorePageErrors) {
                            System.err.println("Ignored : " + ex.getMessage());
                        } else {
                            throw ex;
                        }
                    }
                } else {
                    rx.jumpRel(0x0100);
                }
                navigateToNextPage(rx);
            }
        } finally {
            rx.popPosition();
        }
    }

    private void navigateToNextPage(RandomAccess rx) {
        if ((rx.position() & 0xFF) == 0x00) {
            // Actually we might be already at a new page.
        } else {
            // Jump to the next probable page.
            rx.jumpAbs((rx.position() & 0xFFFFFF00) + 0x0100);
        }
        int addr = 0;
        if (!rx.isAtEnd()) {
            do {
                rx.pushPosition();
                addr = rx.leLong();
                rx.popPosition();
                // check if there is really a page here.
                // if so, the offset in the file must match the value.
                // if not, we continue.
                if (addr != rx.position()) {
                    rx.jumpRel(0x0100);
                }
            } while ((addr != rx.position()) && !rx.isAtEnd());
        }
    }

    /**
     * Sometimes a start of block is found but the block is half overwritten by another block.
     * This results in a RLE exception. This function checks if the page is complete (was not
     * partially overwritten) by checking there are no other start of blocks within the area
     * of the block.
     * @return true if there is a complete page at the current position.
     */
    private boolean isCompletePage() {
        int pageSize;
        rx.pushPosition();
        try {
            rx.leLong();
            pageSize = rx.leShort();
        } finally {
            rx.popPosition();
        }
        rx.pushPosition();
        try {
            int ofs = 0;
            while (ofs < pageSize) {
                ofs += 0x0100;
                rx.jumpRel(0x0100);
                if (ofs < pageSize) {
                    int addr;
                    rx.pushPosition();
                    try {
                        addr = rx.leLong();
                    } finally {
                        rx.popPosition();
                    }
                    if (addr == rx.position()) {
                        System.out.println("Incomplete Page");
                        return false;
                    }
                }
            }
        } finally {
            rx.popPosition();
        }
        return true;
    }

    public void flush() {
        for (TpsPage page : getPages()) {
            page.flush();
        }
    }

    public List<TpsPage> getPages() {
        return pages;
    }

    @Override
    public String toString() {
        return "TpsBlock(" + rx.toHex8(start) + ".." + rx.toHex8(end) + "," + pages.size() + ")";
    }

}
