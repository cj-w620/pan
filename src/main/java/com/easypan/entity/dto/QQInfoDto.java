package com.easypan.entity.dto;

public class QQInfoDto {
    private Integer ret;    //返回码
    private String msg; //如果ret<0，会有相应的错误信息提示，返回数据全部用UTF-8编码
    private String nickName;
    private String figureurl_qq_1;  //大小为40x40像素的QQ头像URL
    private String figureurl_qq_2;  //大小为100x100像素的QQ头像URL，不是所有用户都有，但40x40的一定有
    private String gender;  //性别，如果获取不到则默认返回“男”
    
    public Integer getRet() {
        return ret;
    }
    
    public void setRet(Integer ret) {
        this.ret = ret;
    }
    
    public String getMsg() {
        return msg;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public String getNickName() {
        return nickName;
    }
    
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
    
    public String getFigureurl_qq_1() {
        return figureurl_qq_1;
    }
    
    public void setFigureurl_qq_1(String figureurl_qq_1) {
        this.figureurl_qq_1 = figureurl_qq_1;
    }
    
    public String getFigureurl_qq_2() {
        return figureurl_qq_2;
    }
    
    public void setFigureurl_qq_2(String figureurl_qq_2) {
        this.figureurl_qq_2 = figureurl_qq_2;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
}
