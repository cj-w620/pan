package com.easypan.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

//将这个注解写在类上之后，就会忽略类中不存在的字段。
//这个注解还可以指定要忽略的字段，例如@JsonIgnoreProperties({ “password”, “secretKey” })
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingsDto implements Serializable {
    //默认邮件标题
    private String registerEmailTitle = "easy云盘 邮箱验证码";
    
    //默认邮件内容
    private String registerEmailContent = "您好，您的邮箱验证码是：%s，15分钟内有效";
    
    //使用空间大小，单位为兆（M）
    private Integer userInitUseSpace = 5;

    public String getRegisterEmailTitle() {
        return registerEmailTitle;
    }

    public void setRegisterEmailTitle(String registerEmailTitle) {
        this.registerEmailTitle = registerEmailTitle;
    }

    public String getRegisterEmailContent() {
        return registerEmailContent;
    }

    public void setRegisterEmailContent(String registerEmailContent) {
        this.registerEmailContent = registerEmailContent;
    }

    public Integer getUserInitUseSpace() {
        return userInitUseSpace;
    }
    
    public void setUserInitUseSpace(Integer userInitUseSpace) {
        this.userInitUseSpace = userInitUseSpace;
    }
}
