package rpc.netty;


import rpc.protocol.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UnprocessedRequests {

    //记录还没得到响应的请求的ID以及对应用来存储响应的CompletableFuture
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }


    public void complete(RpcResponse<Object> rpcResponse) {
        //当完成时，从未处理队列中删除掉
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            //自己写数据
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
