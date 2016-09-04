package pers.wanggh;

/**
 * Created by wanggh on 16/1/28.
 */
public class DocProcess {

    private volatile long maxDoc = 0;

    public void process(String doc){

    }

    public void segmenter(){

    }

    public void process(){
        maxDoc++;
    }
}
