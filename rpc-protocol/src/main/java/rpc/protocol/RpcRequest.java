package rpc.protocol;

import lombok.*;

import java.io.Serializable;

/**
 * @author yhw
 * @version 1.0
 * @description   自定义请求类型
 **/
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1905122041950251207L;

    //请求序号
    private String requestId;

    //接口全类名
    private String interfaceName;
    //方法名
    private String methodName;
    //方法参数
    private Object[] parameters;
    //参数类型
    private Class<?>[] paramTypes;


    public String getRpcServiceName() {
        return this.getInterfaceName();
    }
}

