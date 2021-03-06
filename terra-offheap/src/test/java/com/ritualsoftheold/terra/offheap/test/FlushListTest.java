package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.offheap.util.IntFlushList;

public class FlushListTest {

    private IntFlushList list;
    
    @Before
    public void init() {
        list = new IntFlushList(10, 2);
    }
    
    @Test
    public void addTest() {
        // This will also force list to enlarge backing array
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        
        // Test that they were put there correctly
        int[] array = list.getArray();
        for (int i = 0; i < 100; i++) {
            assertEquals(i, array[i]);
        }
    }
}
