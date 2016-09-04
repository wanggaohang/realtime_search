package pers.wanggh;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.wanggh.pojo.PostingIndex;
import pers.wanggh.util.PostingUtil;

import java.util.Iterator;

/**
 * Created by wanggh on 16/1/28.<br/>
 * one thread write, multi-thread read
 */
public class TermDic {
    private static final Logger LOG = LoggerFactory.getLogger(TermDic.class);

    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 14;
    private static final float DEFAULT_LOAD_FACTOR = 0.5f;
    private final float loadFactor;

    private int size = 0;

    private int threshold;
    private int capacity;
    private int[] num_index;
    private int[] pointer_index;
    private String[] term_index;

    private final SlicePool slicePool;

    public TermDic(){
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public TermDic(int initialCapacity, float loadFactor){
        Preconditions.checkArgument(initialCapacity > 0, "initialCapacity must > 0", initialCapacity);
        Preconditions.checkArgument(loadFactor > 0, "loadFactor must > 0.0", loadFactor);
        threshold = (int)(initialCapacity * loadFactor);
        capacity = initialCapacity;
        this.loadFactor = loadFactor;

        num_index = new int[initialCapacity];
        pointer_index = new int[initialCapacity];
        term_index = new String[initialCapacity];

        slicePool = new SlicePool();
    }

    /**
     * change capacity size to new_capacity and rehash the keys to new slots
     * @param new_capacity
     */
    private void rehash(int new_capacity){
        Preconditions.checkArgument(new_capacity<Integer.MAX_VALUE && new_capacity>0,
                "new size must >0 and < Integer.MAX_VALUE", new_capacity);
        new_capacity = roundUpToPowerOf2(new_capacity);
        LOG.debug("expand capacity from {} to {}, and rehash", capacity, new_capacity);
        int[] num_index_new = new int[new_capacity];
        int[] pointer_index_new = new int[new_capacity];
        String[] term_index_new = new String[new_capacity];

        for(int i=0,l=term_index.length;i<l;i++){
            if(!Strings.isNullOrEmpty(term_index[i])){
                for (int probe = 0; probe < new_capacity; probe++) {
                    int pos = (hash(term_index[i]) + probe)  & (new_capacity - 1);
                    if(Strings.isNullOrEmpty(term_index_new[pos])){
                        num_index_new[pos] = num_index[i];
                        pointer_index_new[pos] = pointer_index[i];
                        term_index_new[pos] = term_index[i];
                        break;
                    }
                }
            }
        }
        synchronized (this){
            num_index = num_index_new;
            pointer_index = pointer_index_new;
            term_index = term_index_new;
            threshold = (int)(capacity * loadFactor);
            capacity = new_capacity;
        }
    }

    private void checkRehash(){
        if(size >= threshold){
            rehash(capacity * 2);
        }
    }

    private final int hash(String term){
        return term.hashCode();
    }

    private static int roundUpToPowerOf2(int number) {
        // assert number >= 0 : "number must be non-negative";
        return number >= MAXIMUM_CAPACITY
                ? MAXIMUM_CAPACITY
                : (number > 1) ? Integer.highestOneBit((number - 1) << 1) : 1;
    }

    public void put(String term, int doc_id, int term_pos){
        Preconditions.checkArgument(!Strings.isNullOrEmpty(term), "term can not empty");
        Preconditions.checkArgument(doc_id >= 0, "doc_id must >= 0, current is %s", doc_id);
        Preconditions.checkArgument(term_pos >= 0, "term_pos must >= 0, current is %s", term_pos);

        checkRehash();

        int posting = PostingUtil.composePosting(doc_id, term_pos);
        for (int probe = 0; probe < capacity; probe++) {
            int pos = (hash(term) + probe)  & (capacity - 1);
            if(Strings.isNullOrEmpty(term_index[pos])){
                // not have this term yet,init the posting list

                int index = slicePool.applySlice(0);
                slicePool.setValue(posting, 0, index, 0);
                pointer_index[pos] = PostingUtil.composePostingIndex(0, index, 0);
                num_index[pos] = 1;
                term_index[pos] = term;
                size++;
                break;
            }else if(term.equals(term_index[pos])){
                PostingIndex p_ind = PostingUtil.splitPostingIndex(pointer_index[pos]);
                int insert_offset = p_ind.offset + 1;
                if(insert_offset >= slicePool.getSilceLength(p_ind.pool_num)){
                    // this slice is full,apply another slice and store the current slice address
                    // in the first element of new slice as "previous slice pointer"

                    int previous_slice_pointer = PostingUtil.composePostingIndex(p_ind.pool_num, p_ind.index, p_ind.offset);
                    int new_pool_num = p_ind.pool_num < 3?p_ind.pool_num+1:3;
                    int new_index = slicePool.applySlice(new_pool_num);
                    LOG.debug("apply bigger slice, pool_num:{}, index:{}", new_pool_num, new_index);
                    // insert previous slice pointer in first element of new slice
                    slicePool.setValue(previous_slice_pointer, new_pool_num, new_index, 0);
                    // insert posting in second element of new slice
                    slicePool.setValue(posting, new_pool_num, new_index, 1);
                    // update the tail of posting list pointer
                    pointer_index[pos] = PostingUtil.composePostingIndex(new_pool_num, new_index, 1);
                }else{
                    slicePool.setValue(posting, p_ind.pool_num, p_ind.index, insert_offset);
                    pointer_index[pos] = PostingUtil.composePostingIndex(p_ind.pool_num, p_ind.index, insert_offset);
                }
                num_index[pos] += 1;
                break;
            }
        }
    }

    /**
     *
     * @return the size of terms
     */
    public int size() {
        return size;
    }

    /**
     *
     * @param term
     * @return the posting list Iterator of term, if the dic donot contains term,
     * Iterator.hasNext() is false
     */
    public Iterator<Integer> getIterator(String term){
        Preconditions.checkArgument(!Strings.isNullOrEmpty(term), "term can not empty");
        return new Itr(term, capacity, pointer_index, term_index);
    }

    private class Itr implements Iterator<Integer> {
        private String term;
        private int current_capacity;
        private int[] current_pointer_index;
        private String[] current_term_index;

        private PostingIndex p_ind;
        private int current_offset;

        Itr(String term, int current_capacity,
            int[] current_pointer_index,
            String[] current_term_index){
            this.term = term;
            // save the snapshot
            this.current_capacity = current_capacity;
            this.current_pointer_index = current_pointer_index;
            this.current_term_index = current_term_index;
        }

        private int termPos(){
            for (int probe = 0; probe < current_capacity; probe++) {
                int pos = (hash(term) + probe)  & (current_capacity - 1);
                if(Strings.isNullOrEmpty(current_term_index[pos])){
                    return -1;
                }else if(term.equals(current_term_index[pos])){
                    return pos;
                }
            }
            return -1;
        }

        @Override
        public boolean hasNext() {
            if(p_ind == null){
                int pos = termPos();
                if(pos < 0){
                    return false;
                }else{
                    p_ind = PostingUtil.splitPostingIndex(current_pointer_index[pos]);
                    current_offset = p_ind.offset;
                }
            }

            if(current_offset >= 0){
                return true;
            }

            return false;
        }

        @Override
        public Integer next() {
            if(p_ind == null){
                return null;
            }
            if(p_ind.pool_num > 0 && current_offset == 0){
                p_ind = PostingUtil.splitPostingIndex(
                        slicePool.getValue(p_ind.pool_num, p_ind.index, current_offset));
                current_offset = p_ind.offset;
            }
            int posting = slicePool.getValue(p_ind.pool_num, p_ind.index, current_offset);
            current_offset--;
            return posting;
        }
    }

//    public void printTermPostings(String term){
//        for (int probe = 0; probe < capacity; probe++) {
//            int pos = (hash(term) + probe)  & (capacity - 1);
//            if(Strings.isNullOrEmpty(term_index[pos])){
//                LOG.debug("empty");
//                break;
//            }else if(term.equals(term_index[pos])){
//                PostingIndex p_ind = PostingUtil.splitPostingIndex(pointer_index[pos]);
//                int offset = p_ind.offset;
//                StringBuffer sb = new StringBuffer();
//                while(p_ind.pool_num >= 0 && offset >= 0){
//                    if(p_ind.pool_num > 0 && offset == 0){
//                        p_ind = PostingUtil.splitPostingIndex(
//                                slicePool.getValue(p_ind.pool_num, p_ind.index, offset));
//                        offset = p_ind.offset;
//                        LOG.debug(" go to previous slice, PostingIndex:{}", p_ind);
//                    }
//                    sb.append(PostingUtil.splitPosting(slicePool.getValue(p_ind.pool_num, p_ind.index, offset)))
//                            .append(",");
//                    offset--;
//                }
//                LOG.debug(sb.toString());
//                break;
//            }
//        }
//    }

}
