package com.microsoft.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;

/**
 * Created by shch on 4/24/2016.
 */
public class TokenFileStorage {
    private final static Logger log = Logger.getLogger(TokenFileStorage.class.getName());
    private static final String CacheDir = ".msauth4j";
    private static final String CacheFileName = "msauth4j.cache";
    private Path filePath;
    private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

    public TokenFileStorage(String baseDirPath) throws Exception {
        
        String baseDir = System.getProperty("user.home");
        
        if(!StringUtils.isNullOrEmpty(baseDirPath) && Files.exists(Paths.get(baseDirPath)) ) {
            baseDir = baseDirPath;
        }
        
        Path dirPath = Paths.get(baseDir, CacheDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectory(dirPath);
        }
        filePath = Paths.get(baseDir, CacheDir, CacheFileName);
        log.info("filePath = '" + filePath + "'");
        
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
    }
    
    public byte[] read() throws Exception {
        try {
            rwlock.readLock().lock();
            return Files.readAllBytes(filePath);

        } finally {
            rwlock.readLock().unlock();
        }
    }

    public void write(byte[] data) throws Exception {
        try {
            rwlock.writeLock().lock();
            Files.write(filePath, data);

        } finally {
            rwlock.writeLock().unlock();
        }
    }
}
