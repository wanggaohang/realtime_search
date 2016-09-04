package pers.wanggh.pojo;

/**
 * Created by wanggh on 16/1/27.
 * <br/>object to store posting position index info
 */
public class PostingIndex {
    public int offset;
    public int index;
    public int pool_num;

    public PostingIndex(int pool_num, int index, int offset) {
        this.pool_num = pool_num;
        this.index = index;
        this.offset = offset;
    }

    @Override
    public int hashCode() {
        return (pool_num << 30) | (index << 11) | offset;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof PostingIndex){
            if(((PostingIndex) obj).pool_num == pool_num &&
                    ((PostingIndex) obj).index == index &&
                    ((PostingIndex) obj).pool_num == pool_num){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "pool_num:" + pool_num +" ,index:" + index + ", offset:" + offset;
    }
}
