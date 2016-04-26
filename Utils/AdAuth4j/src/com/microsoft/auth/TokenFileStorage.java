package com.microsoft.auth;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Created by shch on 4/24/2016.
 */
public class TokenFileStorage {
    final static Logger log = Logger.getLogger(AcquireTokenHandlerBase.class.getName());
    private static final String CacheDir = ".msauth4j";
    private static final String CacheFileName = "msauth4j.cache";
    private Path filePath;
    private final Object lock = new Object();

    public TokenFileStorage() throws Exception {
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

    private FileLock acquireLock(RandomAccessFile raf) throws Exception {
        // in case of multiprocess file access
        FileLock lock = null;
        int tryCount = 3;
        long sleepSec = 10;
        while(tryCount > 0) {
            try {
                lock = raf.getChannel().tryLock();
                break;
            } catch(OverlappingFileLockException ex) {
                log.warn(String.format("The file has been locked by another process - waiting %s sec to release [%d attempt(s) left].", sleepSec, tryCount));
                Thread.sleep(Duration.ofSeconds(sleepSec).toMillis());
                tryCount--;
            }
        }
        return lock;
    }

    public byte[] read() throws Exception {
        RandomAccessFile in = new RandomAccessFile(filePath.toString(), "rw");
        try {
            // in case of multiprocess file access
            FileLock lock = acquireLock(in);
            if(lock != null) {
                log.info("Locking file cache for reading...");
                try {
                    int length = (int)new File(filePath.toString()).length();
                    byte[] data = new byte[length];
                    log.info("Reading data...");
                    in.read(data);
                    return data;
                } finally {
                    log.info("Unocking file cache");
                    lock.release();
                }
            } else {
                throw new IOException("Can't lock file token cache for reading");
            }
        } finally {
            in.close();
        }
    }

    public void write(byte[] data) throws Exception {
        RandomAccessFile out = new RandomAccessFile(filePath.toString(), "rw");
        try {
            // in case of multiprocess file access
            FileLock lock = acquireLock(out);
            if(lock != null) {
                log.info("Locking file cache for writing");
                try {
                    log.info("Writing file...");
                    out.write(data);
                } finally {
                    log.info("Unocking file cache");
                    lock.release();
                }
            } else {
                throw new IOException("Can't lock file token cache for writing");
            }
        } finally {
            out.close();
        }
    }
}
