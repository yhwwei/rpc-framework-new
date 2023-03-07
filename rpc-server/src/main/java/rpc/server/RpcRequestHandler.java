package rpc.server;

import rpc.enums.RpcErrorMessageEnum;
import rpc.enums.RpcResponseCodeEnum;
import rpc.protocol.RpcRequest;
import rpc.protocol.RpcResponse;
import rpc.provider.ServiceProvider;
import rpc.utils.ReflectionUtil;

import java.lang.reflect.Method;

/**
 * @author yhw
 * @version 1.0
 * @description  负责将请求处理，生成响应
 **/
public class RpcRequestHandler {
    private final ServiceProvider serviceProvider;
    public RpcRequestHandler(ServiceProvider serviceProvider){
        this.serviceProvider = serviceProvider;
    }

    public RpcResponse handlerRequest(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        RpcResponse<Object> rpcResponse = null;
        try {
            System.out.println(service);
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            Object ret = ReflectionUtil.invoke(service, method, rpcRequest.getParameters());
            rpcResponse = RpcResponse.success(ret, rpcRequest.getRequestId());
            rpcResponse.setData(ret);
        } catch (NoSuchMethodException e) {
            //发生异常则表标记响应失败
            rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAILURE);
            rpcResponse.setMessage("RpcServer got error: "
                    +e.getClass().getName()+" :"+e.getMessage());
            throw new RuntimeException(e);
        }
        return rpcResponse;
    }
}
