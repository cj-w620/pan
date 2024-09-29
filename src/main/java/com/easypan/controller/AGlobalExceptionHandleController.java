package com.easypan.controller;

import com.easypan.enums.ResponseCodeEnum;

import com.easypan.entity.vo.ResponseVO;

import com.easypan.exception.BusinessException;

import com.sun.mail.smtp.SMTPSendFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.net.BindException;

@RestControllerAdvice
public class AGlobalExceptionHandleController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(AGlobalExceptionHandleController.class);

    @ExceptionHandler(value = Exception.class)
    Object handleException(Exception e, HttpServletRequest request){
        logger.error("请求错误，请求地址：{}，错误信息：{}",request.getRequestURI(),e);
        ResponseVO ajaxResponse = new ResponseVO();

        if(e instanceof NoHandlerFoundException){
            ajaxResponse.setCode(ResponseCodeEnum.CODE_404.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_404.getMsg());
            ajaxResponse.setStatus(STATUC_ERROR);
        }else if(e instanceof BusinessException){
            BusinessException biz = (BusinessException) e;
            ajaxResponse.setCode(biz.getCode());
            ajaxResponse.setInfo(biz.getMessage());
            ajaxResponse.setStatus(STATUC_ERROR);
        }else if(e instanceof BindException){
            ajaxResponse.setCode(ResponseCodeEnum.CODE_600.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_600.getMsg());
            ajaxResponse.setStatus(STATUC_ERROR);
        }else if(e instanceof DuplicateKeyException){
            ajaxResponse.setCode(ResponseCodeEnum.CODE_601.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_601.getMsg() + "拦截器提示：重复键信息，检查数据库");
            ajaxResponse.setStatus(STATUC_ERROR);
        }else {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_500.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_500.getMsg());
            ajaxResponse.setStatus(STATUC_ERROR);
        }
        return ajaxResponse;
    }
}
