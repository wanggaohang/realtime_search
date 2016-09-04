package pers.wanggh;

import junit.framework.TestCase;

/**
 * Created by wanggh on 16/1/28.
 */
public class UtilTest extends TestCase {
    public void testPostingUtil(){
        int[] pool_nums = {0,1,2,3};
        int[] indexs = {0,100,0x7ffff};
        int[] offsets = {0,100,0x7ff};
        for(int pool_num : pool_nums){
            for(int index : indexs){
                for(int offset : offsets){
                    int posting_index = PostingUtil.composePostingIndex(pool_num, index, offset);
                    PostingIndex pi = PostingUtil.splitPostingIndex(posting_index);
                    assertTrue(pi.equals(new PostingIndex(pool_num, index, offset)));
                    assertTrue(pi.pool_num == pool_num);
                    assertTrue(pi.index == index);
                    assertTrue(pi.offset == offset);
                }
            }
        }

        int[] doc_ids = {0, 100, 0xffffff};
        int[] term_poss = {0, 100, 0xff};
        for(int doc_id :doc_ids){
            for(int term_pos : term_poss){
                int posting = PostingUtil.composePosting(doc_id, term_pos);
                Posting p = PostingUtil.splitPosting(posting);
                assertTrue(p.equals(new Posting(doc_id, term_pos)));
            }
        }
    }

    public void testPostingUtilException(){
        int[] pool_nums = {-1,5,0};
        int[] indexs = {-1,0x7ffff+1,0};
        int[] offsets = {-1,0x7ff+1,0};
        for(int pool_num : pool_nums){
            for(int index : indexs){
                for(int offset : offsets){
                    if(pool_num==0 && index==0 && offset==0) continue;
                    try {
                        PostingUtil.composePostingIndex(pool_num, index, offset);
                    } catch(Exception e){
                        assertTrue(e instanceof IllegalArgumentException);
                    }
                }
            }
        }

        int[] doc_ids = {-1, 0xffffff+1, 0};
        int[] term_poss = {-1, 0xff+1, 0};
        for(int doc_id :doc_ids){
            for(int term_pos : term_poss){
                if(doc_id==0 && term_pos==0) continue;
                try {
                    PostingUtil.composePosting(doc_id, term_pos);
                } catch(Exception e){
                    assertTrue(e instanceof IllegalArgumentException);
                }
            }
        }
    }
}
