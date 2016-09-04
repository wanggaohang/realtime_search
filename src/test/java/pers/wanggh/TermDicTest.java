package pers.wanggh;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by wanggh on 16/1/28.
 */
public class TermDicTest extends TestCase {
    public void testPutGet(){
        TermDic td = new TermDic(8, 0.5f);
        List<Posting> plist = new ArrayList<>();
        int[] doc_ids = {0, 100, 0xffffff};
        int[] term_poss = {0, 100, 0xff};
        for(int doc_id :doc_ids) {
            for (int term_pos : term_poss) {
                Posting p = new Posting(doc_id, term_pos);
                plist.add(p);
                td.put("term", p.doc_id, p.term_pos);
                for(int i=0;i<3;i++){
                    td.put("term"+i, p.doc_id, p.term_pos);
                }
            }
        }

        Iterator<Integer> interator = td.getIterator("term");
        assertTrue(interator.hasNext());
        Posting posting2 = null;
        int i = plist.size() - 1;
        while (interator.hasNext()){
            posting2 = PostingUtil.splitPosting(interator.next());
            assertTrue(posting2.equals(plist.get(i)));
            i--;
        }
        assertTrue(i == -1);
    }

    public void testSize(){
        TermDic td = new TermDic(32, 0.5f);
        assertTrue(td.size() == 0);
        for(int i=0;i<20;i++){
            td.put("term"+i, 1, 1);
        }
        assertTrue(td.size() == 20);
    }

}
