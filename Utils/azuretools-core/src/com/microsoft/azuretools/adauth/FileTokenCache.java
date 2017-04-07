/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.adauth;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileTokenCache extends TokenCache {
    final static Logger log = Logger.getLogger(AcquireTokenHandlerBase.class.getName());
    private final Object lock = new Object();
    private static final String CacheDir = ".msauth4j";
    private static final String CacheFileName = "msauth4j.cache";
    private Path filePath;
    
    public FileTokenCache() throws IOException {
        String homeDir = System.getProperty("user.home");

        Path dirPath = Paths.get(homeDir, CacheDir);

        if (!Files.exists(dirPath)) {
            Files.createDirectory(dirPath);
        }
        
        filePath = Paths.get(homeDir, CacheDir, CacheFileName);
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
    }
    
    private FileLock acquireLock(RandomAccessFile raf) throws IOException, InterruptedException {
        // in case of multiprocess file access
        FileLock lock = null;
        int tryCount = 3;
        long sleepSec = 10;
        while(tryCount > 0) {
            try {
                lock = raf.getChannel().tryLock();
                break;
            } catch(OverlappingFileLockException ex) {
                log.log(Level.WARNING, String.format("The file has been locked by another process - waiting %s sec to release [%d attempt(s) left].", sleepSec, tryCount));
                Thread.sleep(sleepSec*1000);
                tryCount--;
            }
        }
        return lock;
    }
    
    private void read() throws IOException, InterruptedException {
        RandomAccessFile in = new RandomAccessFile(filePath.toString(), "rw");
        try {
            // in case of multiprocess file access
            FileLock lock = acquireLock(in);
            if(lock != null) {
                log.log(Level.FINEST, "Locking file cache for reading...");
                try {
                    int length = (int)new File(filePath.toString()).length();
                    byte[] data = new byte[length];
                    log.log(Level.FINEST, "Reading data...");
                    in.read(data);
                    deserialize(data);
                } finally {
                    log.log(Level.FINEST, "Unocking file cache");
                    lock.release();
                }
            } else {
                throw new IOException("Can't lock file token cache for reading");
            }
        } finally {
            in.close();
        }
    }
    
    private void write() throws IOException, InterruptedException {
        RandomAccessFile out = new RandomAccessFile(filePath.toString(), "rw");
        try {
            // in case of multiprocess file access
            FileLock lock = acquireLock(out);
            if(lock != null) {
                log.log(Level.FINEST, "Locking file cache for writing");
                try {
                    byte[] data = serialize();
                    log.log(Level.FINEST, "Writing file...");
                    out.write(data);
                } finally {
                    log.log(Level.FINEST, "Unocking file cache");
                    lock.release();
                }
            } else {
                throw new IOException("Can't lock file token cache for writing");
            }
        } finally {
            out.close();
        }
    }
//
//    @Override
//    void onBeforeAccess() throws IOException, InterruptedException {
//        synchronized (lock) {
//            read();
//        }
//    }
//
//    @Override
//    void onAfterAccess() {
//        synchronized (lock) {
//            if(getHasStateChanged()) {
//                write();
//                setHasStateChanged(false);
//            }
//        }
//    }
}
