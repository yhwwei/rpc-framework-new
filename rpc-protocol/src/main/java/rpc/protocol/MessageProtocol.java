package rpc.protocol;

import java.util.Arrays;

/**
 * @author yhw
 * @version 1.0
 **/

//用来解决TCP粘包拆包问题
public class MessageProtocol {

    //规定len=0时，表示发送心跳包。
    private int len;
    private byte[] content;

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "MessageProtocol{" +
                "len=" + len +
                ", content=" + Arrays.toString(content) +
                '}';
    }
}
