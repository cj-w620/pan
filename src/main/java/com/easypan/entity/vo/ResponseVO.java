package com.easypan.entity.vo;

public class ResponseVO<T> {
    private String status;  //状态，自定义为"success"或"error"
    private Integer code;   //状态码
    private String info;    //提示信息
    private T data;     //返回数据

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
