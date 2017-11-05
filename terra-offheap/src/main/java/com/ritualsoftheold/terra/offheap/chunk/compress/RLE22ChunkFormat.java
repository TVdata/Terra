package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

public class RLE22ChunkFormat implements ChunkFormat {

    public static final RLE22ChunkFormat INSTANCE = new RLE22ChunkFormat();
    
    @Override
    public boolean convert(long from, long to, int type) {
        switch (type) {
            case ChunkType.UNCOMPRESSED:
                RunLengthCompressor.decompress(from, to);
                break;
        }
        
        return false;
    }

    @Override
    public void processQueries(long chunk, int chunkLen, ChunkBuffer.Allocator alloc, long queue, int size) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids,
            int beginIndex, int endIndex) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ChunkFormat.SetAllResult setAllBlocks(short[] data, ChunkBuffer.Allocator allocator) {
        return null; // TODO
    }

    @Override
    public int getChunkType() {
        return ChunkType.RLE_2_2;
    }

}
