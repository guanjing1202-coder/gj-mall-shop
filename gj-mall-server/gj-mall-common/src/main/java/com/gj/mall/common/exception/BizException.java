package com.gj.mall.common.exception;

import com.gj.mall.common.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(ResultCode code) {
        super(code.getMessage());
        this.code = code.getCode();
    }

    public BizException(ResultCode code, String message) {
        super(message);
        this.code = code.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }
}
