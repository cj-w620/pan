package com.easypan.exception;

import com.easypan.enums.ResponseCodeEnum;

public class BusinessException extends RuntimeException{

    private ResponseCodeEnum codeEnum;  //状态码枚举

    private Integer code;   //状态码

    private String message; //提示信息

    public BusinessException(String message,Throwable e){
        super(message,e);
        this.message = message;
    }
  
    public BusinessException(String message){
        super(message);
        this.message = message;
    }

    public BusinessException(Throwable e){
        super(e);
    }

    public BusinessException(ResponseCodeEnum codeEnum){
        super(codeEnum.getMsg());
        this.codeEnum = codeEnum;
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
    }

    public BusinessException(Integer code,String message){
        super(message);
        this.code = code;
        this.message = message;
    }

    public ResponseCodeEnum getCodeEnum() {
        return codeEnum;
    }

    public void setCodeEnum(ResponseCodeEnum codeEnum) {
        this.codeEnum = codeEnum;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
