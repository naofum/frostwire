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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This interface is to provide a limited functionality
 * abstraction layer in case you need to deal with highly
 * restricted environments (like Android).
 *
 * @author gubatron
 * @author aldenml
 */
public interface FileSystem {

    boolean exists(File file);

    boolean isDirectory(File file);

    boolean isFile(File file);

    boolean canRead(File file);

    boolean canWrite(File file);

    String getParent(File file);

    boolean mkdir(File file);

    OutputStream outputStream(File file) throws IOException;

    FileSystem DEFAULT = new FileSystem() {

        @Override
        public boolean exists(File file) {
            return file.exists();
        }

        @Override
        public boolean isDirectory(File file) {
            return file.isDirectory();
        }

        @Override
        public boolean isFile(File file) {
            return file.isFile();
        }

        @Override
        public boolean canRead(File file) {
            return file.canRead();
        }

        @Override
        public boolean canWrite(File file) {
            return file.canWrite();
        }


        @Override
        public boolean mkdir(File file) {
            return file.mkdir();
        }

        @Override
        public String getParent(File file) {
            return file.getParent();
        }

        @Override
        public OutputStream outputStream(File file) throws IOException {
            return new FileOutputStream(file);
        }
    };
}
