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

import java.io.File;

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
}
