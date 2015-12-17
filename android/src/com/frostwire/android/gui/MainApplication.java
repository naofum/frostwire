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

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.view.ViewConfiguration;
import com.andrew.apollo.cache.ImageCache;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.SystemPaths;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.util.HttpResponseCache;
import com.frostwire.android.util.ImageLoader;
import com.frostwire.android.util.SystemUtils;
import com.frostwire.bittorrent.BTContext;
import com.frostwire.bittorrent.BTEngine;
import com.frostwire.jlibtorrent.DHT;
import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.swig.swig_posix_file_functions;
import com.frostwire.jlibtorrent.swig.swig_posix_stat;
import com.frostwire.logging.Logger;
import com.frostwire.search.CrawlPagedWebSearchPerformer;
import com.frostwire.util.DirectoryUtils;
import com.frostwire.util.FileSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * @author gubatron
 * @author aldenml
 */
public class MainApplication extends Application {

    private static final Logger LOG = Logger.getLogger(MainApplication.class);

    private static FileSystem FILE_SYSTEM = FileSystem.DEFAULT;

    @Override
    public void onCreate() {
        super.onCreate();

        try {

            setupFileSystem();

            ignoreHardwareMenu();
            installHttpCache();

            ConfigurationManager.create(this);

            setupBTEngine();

            NetworkManager.create(this);
            Librarian.create(this);
            Engine.create(this);

            ImageLoader.getInstance(this);
            CrawlPagedWebSearchPerformer.setCache(new DiskCrawlCache(this));
            CrawlPagedWebSearchPerformer.setMagnetDownloader(null); // this effectively turn off magnet downloads

            LocalSearchEngine.create();

            cleanTemp();

            Librarian.instance().syncMediaStore();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to initialized main components", e);
        }
    }

    @Override
    public void onLowMemory() {
        ImageCache.getInstance(this).evictAll();
        ImageLoader.getInstance(this).clear();
        super.onLowMemory();
    }

    private void ignoreHardwareMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field f = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (f != null) {
                f.setAccessible(true);
                f.setBoolean(config, false);
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    private void installHttpCache() {
        try {
            HttpResponseCache.install(this);
        } catch (IOException e) {
            LOG.error("Unable to install global http cache", e);
        }
    }

    private void setupBTEngine() {
        BTEngine.ctx = new BTContext();
        BTEngine.getInstance().reloadBTContext(SystemPaths.getTorrents(),
                SystemPaths.getTorrentData(),
                SystemPaths.getLibTorrent(this),
                0, 0, "0.0.0.0", false, false);
        BTEngine.ctx.optimizeMemory = true;
        BTEngine.ctx.fs = FILE_SYSTEM; // TODO: Review this logic and code
        BTEngine.getInstance().start();

        boolean enable_dht = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_ENABLE_DHT);
        DHT dht = new DHT(BTEngine.getInstance().getSession());
        if (!enable_dht) {
            dht.stop();
        } else {
            // just make sure it's started otherwise.
            // (we could be coming back from a crash on an unstable state)
            dht.start();
        }
    }

    private void cleanTemp() {
        try {
            File tmp = SystemPaths.getTemp();
            DirectoryUtils.deleteFolderRecursively(tmp);

            if (tmp.mkdirs()) {
                new File(tmp, ".nomedia").createNewFile();
            }
        } catch (Throwable e) {
            LOG.error("Error during setup of temp directory", e);
        }
    }

    private void setupFileSystem() {
        if (!SystemUtils.hasLollipopOrNewer() || SystemUtils.hasMarshmallowOrNewer()) {
            return;
        }

        final Context context = this;

        FILE_SYSTEM = new LollipopFileSystem(context);

        LibTorrent.setPosixFileFunctions(new swig_posix_file_functions() {
            @Override
            public int open(String pathname, int flags, int mode) {
                LOG.debug("posix open: " + pathname);
                Uri uri = LollipopFileSystem.getDocumentUri(context, new File(pathname));
                if (uri != null) {
                    try {
                        ContentResolver cr = context.getContentResolver();
                        return cr.openFileDescriptor(uri, "rw").getFd();
                    } catch (FileNotFoundException e) {
                        LOG.error("Can open file descriptor via SAF", e);
                    }
                }

                return super.open(pathname, flags, mode);
            }

            @Override
            public int mkdir(String pathname, int mode) {
                LOG.debug("posix mkdir: " + pathname);
                if (FILE_SYSTEM.mkdir(new File(pathname))) {
                    return 0;
                } else {
                    return super.mkdir(pathname, mode);
                }
            }

            @Override
            public int rename(String oldpath, String newpath) {
                LOG.debug("posix rename: " + oldpath + " -> " + newpath);
                return super.rename(oldpath, newpath);
            }

            @Override
            public int remove(String pathname) {
                LOG.debug("posix remove: " + pathname);
                return super.remove(pathname);
            }

            @Override
            public int stat(String path, swig_posix_stat buf) {
                LOG.debug("posix stat: " + path);
                return super.stat(path, buf);
            }
        });
    }
}
