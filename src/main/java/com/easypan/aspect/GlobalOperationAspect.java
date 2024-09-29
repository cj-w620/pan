package com.easypan.aspect;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
import com.easypan.utils.StringTools;
import com.easypan.utils.VerifyUtils;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component("globalOperationAspect")
public class GlobalOperationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);
    
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";
    
    @Pointcut("@annotation(com.easypan.annotation.GlobalInterceptor)")
    private void requestInterceptor(){
    
    }
    
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point)throws BusinessException{
        try{
            //service
            Object target = point.getTarget();
            //拿到前端传递的参数值
            Object[] arguments = point.getArgs();
            //通过方法名、该方法所有参数类型集合拿到方法
            //方法名
            String methodName = point.getSignature().getName();
            //参数类型集合
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName,parameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if(interceptor == null){
                return;
            }
            /**
             * 校验登录
             */
            if(interceptor.checkLogin() || interceptor.checkAdmin()){
                checkLogin(interceptor.checkAdmin());
            }
            
            /**
             * 校验参数
             */
            if(interceptor.checkParams()){
                validateParams(method,arguments);
            }
        } catch (BusinessException e){
            logger.error("全局拦截异常",e);
            throw e;
        } catch (Exception e){
            logger.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        } catch (Throwable e){
            logger.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }
    
    private void checkLogin(Boolean checkAdmin){
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        
        if(userDto == null){    //无登录信息
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        
        if(checkAdmin && !userDto.getAdmin()){  //需要是管理员，但你不是，你还访问，404去吧
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }
    
    private void validateParams(Method m,Object[] arguments) throws BusinessException{
        //拿到所有参数
        Parameter[] parameters = m.getParameters();
        for(int i = 0;i < parameters.length;i++){   //遍历参数
            //参数
            Parameter parameter = parameters[i];
            //参数值
            Object value = arguments[i];
            //看当前参数是否加了VerifyParam注解
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if(verifyParam == null){    //没加，不用校验，下一个
                continue;
            }
            //基本数据类型
            if(TYPE_STRING.equals(parameter.getParameterizedType().getTypeName()) || TYPE_LONG.equals(parameter.getParameterizedType().getTypeName())){
                checkValue(value,verifyParam);
            }else { //如果传递的是对象
                checkObjValue(parameter,value);
            }
        }
    }
    
    private void checkObjValue(Parameter parameter,Object value){
        try{
            String typeName = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();
            for(Field field : fields){
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if(fieldVerifyParam == null){
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue,fieldVerifyParam);
            }
        }catch (BusinessException e){
            logger.error("校验参数失败",e);
            throw e;
        } catch (Exception e){
            logger.error("校验参数失败",e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
    
    private void checkValue(Object value,VerifyParam verifyParam) throws BusinessException{
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();
        
        /**
         * 校验空
         */
        //逻辑：
        // verifyParam.required()为true：你是必须的
        // isEmpty为true：你是空的
        //需要你但你是空的，报错
        if(isEmpty && verifyParam.required()){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        /**
         * 校验长度
         */
        if(!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        /**
         * 校验正则
         */
        if(!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(),String.valueOf(value))){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
    
}
