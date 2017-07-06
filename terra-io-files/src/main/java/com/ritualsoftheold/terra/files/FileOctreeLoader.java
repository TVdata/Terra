package com.ritualsoftheold.terra.files;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.concurrent.locks.LockSupport;

import com.ritualsoftheold.terra.offheap.io.OctreeLoader;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * Loads octrees from separate files in one directory.
 * Holds persistent, private memory mappings.
 * 
 * Note that the files are loaded in private mode, thus
 * no automated saving is done.
 * 
 *
 */
public class FileOctreeLoader implements OctreeLoader {
    
    private static final Memory mem = OS.memory();

    private Path dir;
    private long fileSize;
    
    public FileOctreeLoader(Path dir, long fileSize) {
        this.dir = dir;
        this.fileSize = fileSize;
    }
    
    @Override
    public long loadOctrees(byte index, long address) {
        Path file = dir.resolve(index + ".terra");
        
        try {
            if (!Files.exists(file)) { // Create new file if necessary
                RandomAccessFile f = new RandomAccessFile(file.toFile(), "rwd");
                f.setLength(fileSize);
                f.close();
            }
            long dataAddr = OS.map(FileChannel.open(file, StandardOpenOption.READ), MapMode.PRIVATE, 0, fileSize);
            long addr = mem.allocate(fileSize);
            mem.copyMemory(dataAddr, addr, fileSize);
            return addr;
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    public void saveOctrees(byte index, long addr) {
        Path file = dir.resolve(index + ".terra");
        try {
            long mappedAddr = OS.map(FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE), MapMode.READ_WRITE, 0, fileSize);
            mem.copyMemory(addr, mappedAddr, fileSize); // Copy data to file
            OS.unmap(mappedAddr, fileSize);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    public int countGroups() {
        int count = 0;
        try {
            for (Path file : Files.newDirectoryStream(dir)) {
                count++;
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return count;
    }

}
