/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.adauth;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        log.log(Level.FINEST, "filePath = '" + filePath + "'");
        
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
