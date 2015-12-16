/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This utility class is intended to provide the same
 * features of the common FileUtils but using a FileSystem.
 *
 * @author gubatron
 * @author aldenml
 */
public final class Files {

    private Files() {
    }

    public static boolean mkdirs(FileSystem fs, File file) {
        /* If the terminal directory already exists, answer false */
        if (fs.exists(file)) {
            return false;
        }

        /* If the receiver can be created, answer true */
        if (fs.mkdir(file)) {
            return true;
        }

        String parentDir = file.getParent();
        /* If there is no parent and we were not created, answer false */
        if (parentDir == null) {
            return false;
        }

        /* Otherwise, try to create a parent directory and then this directory */
        return mkdirs(fs, new File(parentDir)) && fs.mkdir(file);
    }

    public static void writeByteArrayToFile(FileSystem fs, File file, byte[] data) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(fs, file);
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static OutputStream openOutputStream(FileSystem fs, File file) throws IOException {
        if (fs.exists(file)) {
            if (fs.isDirectory(file)) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (fs.canWrite(file) == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!Files.mkdirs(fs, parent) && !fs.isDirectory(parent)) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return fs.outputStream(file);
    }
}
