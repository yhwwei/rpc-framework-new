package rpc.test.service;

/**
 * @author yhw
 * @version 1.0
 **/
public class HelloService implements Hello{
    @Override
    public String hello(String str) {
        return "hello1111"+str;
    }
}
