package rpc.enums;

import lombok.*;

/**
 * @author yhw
 * @version 1.0
 **/
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
public enum RpcResponseCodeEnum {
    SUCCESS(200,"remote call successfully completed"),
    FAILURE(500,"remote call is failed");
    private int code;
    private String message;
}
