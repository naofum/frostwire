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

package com.frostwire.android.gui;

import android.content.Context;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import com.frostwire.logging.Logger;
import com.frostwire.util.FileSystem;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 */
public final class LollipopFileSystem implements FileSystem {

    private static final Logger LOG = Logger.getLogger(LollipopFileSystem.class);

    private final Context context;

    public LollipopFileSystem(Context context) {
        this.context = context;
    }

    @Override
    public boolean exists(File file) {
        DocumentFile f = getDocumentFile(context, file);
        return f != null && f.exists();
    }

    @Override
    public boolean isDirectory(File file) {
        DocumentFile f = getDocumentFile(context, file);
        return f != null && f.isDirectory();
    }

    @Override
    public boolean isFile(File file) {
        DocumentFile f = getDocumentFile(context, file);
        return f != null && f.isFile();
    }

    @Override
    public boolean canRead(File file) {
        DocumentFile f = getDocumentFile(context, file);
        return f != null && f.canRead();
    }

    @Override
    public boolean canWrite(File file) {
        DocumentFile f = getDocumentFile(context, file);
        return f != null && f.canWrite();
    }

    @Override
    public boolean mkdir(File file) {
        DocumentFile f = getDocumentFile(context, file.getParentFile());
        if (f != null) {
            try {
                f = f.createDirectory(file.getName());
                if (f != null && f.exists()) {
                    return true;
                }
            } catch (Throwable e) {
                LOG.error("Error creating directory: " + file, e);
            }
        }

        return false;
    }

    @Override
    public OutputStream outputStream(File file) throws IOException {
        DocumentFile f = getDocumentFile(context, file);
        if (f != null) {
            return context.getContentResolver().openOutputStream(f.getUri());
        } else {
            throw new IOException("Can't build a document file from: " + file);
        }
    }

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5). If the file is not
     * existing, it is created.
     *
     * @param file The file.
     * @return The DocumentFile
     */
    private static DocumentFile getDocumentFile(Context context, File file) {
        String baseFolder = getExtSdCardFolder(file, context);

        if (baseFolder == null) {
            return DocumentFile.fromFile(file);
        }

        String volumeId = getVolumeId(context, baseFolder);
        if (volumeId == null) {
            return DocumentFile.fromFile(file);
        }

        String fullPath = file.getAbsolutePath();
        String relativePath = fullPath.substring(baseFolder.length() + 1);

        relativePath = relativePath.replace("/", "%2F");
        String uri = "content://com.android.externalstorage.documents/tree/" + volumeId + "%3A" + relativePath;
        Uri treeUri = Uri.parse(uri);

        return DocumentFile.fromTreeUri(context, treeUri);
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
     * null is returned.
     */
    private static String getExtSdCardFolder(final File file, Context context) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                    return extSdPaths[i];
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    private static String[] getExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : ContextCompat.getExternalFilesDirs(context, "external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index >= 0) {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                } else {
                    LOG.warn("ext sd card path wrong: " + file.getAbsolutePath());
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    private static String getVolumeId(Context context, final String volumePath) {
        try {
            StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);

                if (path != null) {
                    if (path.equals(volumePath)) {
                        return (String) getUuid.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
