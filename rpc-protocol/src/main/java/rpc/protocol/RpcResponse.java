package rpc.protocol;

import lombok.*;
import rpc.enums.RpcResponseCodeEnum;

import java.io.Serializable;

/**
 * @author yhw
 * @version 1.0
 **/

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcResponse<T> implements Serializable {

    private static final long serialVersionUID = 715745410605631233L;

    //表示对哪个请求的响应
    private String requestId;
    //返回状态码
    private Integer code;
    //提示消息
    private String message;
    //数据实体
    private T data;


    //远程调用成功
    public static <T> RpcResponse<T> success(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(RpcResponseCodeEnum.SUCCESS.getCode());
        response.setMessage(RpcResponseCodeEnum.SUCCESS.getMessage());
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    //返回远程调用失败消息
    public static <T> RpcResponse<T> fail(RpcResponseCodeEnum rpcResponseCodeEnum) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(rpcResponseCodeEnum.FAILURE.getCode());
        response.setMessage(rpcResponseCodeEnum.FAILURE.getMessage());
        return response;
    }

}