/*
 *  Copyright 2013 E.Hooijmeijer
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
package nl.cad.tpsparse.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.cad.tpsparse.decrypt.Key;

/**
 * @author E.Hooijmeijer
 */
public final class Utils {

    private Utils() {
        // Utility Class.
    }

    /**
     * reads an inputstream fully and returns the bytes.
     * @param in the inputstream.
     * @return the bytes.
     * @throws IOException if reading failed.
     */
    public static final byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int rd = 0;
        while ((rd = in.read(buffer)) >= 0) {
            out.write(buffer, 0, rd);
        }
        return out.toByteArray();
    }

    public static final byte[] readFully(File file) throws IOException {
        if (file.length() > ((long) Integer.MAX_VALUE)) {
            throw new IOException("File size exceeds maximum Java Array Size.");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length());
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] buffer = new byte[128 * 1024];
            int rd = 0;
            while ((rd = in.read(buffer)) >= 0) {
                out.write(buffer, 0, rd);
            }
        } finally {
            in.close();
        }
        return out.toByteArray();
    }

    /**
     * decrypts the bytes using the given password.
     * @param the bytes to decrypt.
     * @param the password.
     * @return the decrypted file, according to the password.
     */
    public static final byte[] decrypt(byte[] bytes, String password) {
        Key key = new Key(password);
        key.init();
        return key.decrypt(bytes, 0, bytes.length);
    }

    public static String reportMemoryUsage() {
        Runtime r = Runtime.getRuntime();
        return String.valueOf((r.totalMemory() - r.freeMemory()) / (1024 * 1024)) + " MB";
    }
}
