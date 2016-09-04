package pers.wanggh;

import com.google.common.base.Preconditions;

/**
 * Created by wanggh on 16/1/27.
 */
public class PostingUtil {
    /**
     * compose posting info to a int value. <br>
     * 0~8 bit is doc_id; 8~32 bit is term_pos;
     * @param doc_id
     * @param term_pos
     * @return
     */
    public static int composePosting(int doc_id, int term_pos){
        Preconditions.checkArgument(doc_id >=0 && doc_id <= 0xffffff,
                "doc_id must >=0 and <= 0xffffff(16777215), current is %s", doc_id);
        Preconditions.checkArgument(term_pos >=0 && term_pos <= 0xff,
                "term_pos must >=0 and  <= 0xff(255), current is %s", term_pos);
        return (doc_id << 8) | term_pos;
    }

    public static Posting splitPosting(int posting){
        return new Posting((posting >> 8) & 0xffffff, posting & 0xff);
    }

    /**
     * compose posting info to a int value.  <br>
     * 0~11 bit is offset; 12~29 bit is slice index; 31~32 is pool_num.
     * @param pool_num
     * @param index
     * @param offset
     * @return
     */
    public static int composePostingIndex(int pool_num, int index, int offset){
        Preconditions.checkArgument(pool_num>=0 && pool_num <4,
                "pool_num is must in (0,1,2,3), current is %s", pool_num);
        Preconditions.checkArgument(index>=0 && index <= 0x7ffff,
                "index must >=0 and <= 0x7ffff(524287), current is %s", index);
        Preconditions.checkArgument(offset>=0 && offset <= 0x7ff,
                "offset must >=0 and <= 0x7ff(2047), current is %s", offset);
        return (pool_num << 30) | (index << 11) | offset;
    }

    public static PostingIndex splitPostingIndex(int posting_index){
        return new PostingIndex((posting_index >> 30) & 0x3,
                (posting_index >> 11) & 0x7ffff,
                posting_index & 0x7ff);
    }
}
