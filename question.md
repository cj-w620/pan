# 待解决问题
## redis写入乱码


# 疑问
## 关于异常处理BusinessException
BusinessException继承Exception，在方法中throw new BusinessException，就会报错，Unhandled exception: com.easypan.exception.BusinessException

改为继承RuntimeException后，就不会报错了，为啥？？？？？
一开始以为是Application启动类没有加上自定义异常处理的注解，可查了资料发现，自定义全局异常处理注解是加在那个异常处理类上的。