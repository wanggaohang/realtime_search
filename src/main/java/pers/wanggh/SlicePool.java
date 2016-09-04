package pers.wanggh;


import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wanggh on 16/1/27.
 */
public class SlicePool {
    private final int[] SLICE_SIZE = {4,32,128,2048};
    private final List<Map<Integer, int[]>> slice_pools = new ArrayList<>(4);
    private final AtomicInteger[] slice_indexs = new AtomicInteger[4];

    public SlicePool() {
        slice_pools.add(new HashMap<>());
        slice_pools.add(new HashMap<>());
        slice_pools.add(new HashMap<>());
        slice_pools.add(new HashMap<>());
        slice_indexs[0] = new AtomicInteger(0);
        slice_indexs[1] = new AtomicInteger(0);
        slice_indexs[2] = new AtomicInteger(0);
        slice_indexs[3] = new AtomicInteger(0);
    }

    /**
     * apply for new slice
     * @param pool_num has four kind of pool_num:
     *                 <ul>
     *                    <li>0: length is 4</li>
     *                    <li>1: length is 32</li>
     *                    <li>2: length is 128</li>
     *                    <li>3: length is 2048</li>
     *                 </ul>
     * @return
     */
    public int applySlice(int pool_num){
        checkPoolNum(pool_num);
        int ind = slice_indexs[pool_num].incrementAndGet();
        slice_pools.get(pool_num).put(ind, new int[SLICE_SIZE[pool_num]]);
        return ind;
    }

    public void setValue(int posting, int pool_num, int index, int offset){
        checkPoolNum(pool_num);
        int[] slice = slice_pools.get(pool_num).get(index);
        Preconditions.checkNotNull(slice);
        Preconditions.checkElementIndex(offset, slice.length);
        slice[offset] = posting;
    }

    public int getValue(int pool_num, int index, int offset){
        checkPoolNum(pool_num);
        int[] slice = slice_pools.get(pool_num).get(index);
        Preconditions.checkNotNull(slice);
        Preconditions.checkElementIndex(offset, slice.length);
        return slice[offset];
    }

    private void checkPoolNum(int pool_num){
        Preconditions.checkArgument(pool_num>=0 && pool_num <4,
                "pool_num is must in (0,1,2,3), current is %s", pool_num);
    }

    public int getSilceLength(int pool_num){
        checkPoolNum(pool_num);
        return SLICE_SIZE[pool_num];
    }
}
