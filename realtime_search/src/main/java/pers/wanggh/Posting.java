package pers.wanggh.pojo;

/**
 * Created by wanggh on 16/1/27.
 * <br/>object to store posting info
 */
public class Posting {
    public int doc_id;
    public int term_pos;

    public Posting(int doc_id, int term_pos) {
        this.doc_id = doc_id;
        this.term_pos = term_pos;
    }

    @Override
    public int hashCode() {
        return (doc_id << 8) | term_pos;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Posting){
            if(((Posting) obj).doc_id == doc_id &&
                    ((Posting) obj).term_pos == term_pos){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "(doc_id:" + doc_id +" ,term_pos:" + term_pos + ")";
    }
}
