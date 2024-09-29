package com.easypan.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:邮箱验证码Mapper
 * @auther:wjc
 * @date:2024/06/30
 */
public interface EmailCodeMapper<T, P> extends BaseMapper {
	/**
	 * 根据EmailAndCode查询
	 */
	 T selectByEmailAndCode(@Param("email") String email, @Param("code") String code);

	/**
	 * 根据EmailAndCode更新
	 */
	 Integer updateByEmailAndCode(@Param("bean") T t, @Param("email") String email, @Param("code") String code);

	/**
	 * 根据EmailAndCode删除
	 */
	 Integer deleteByEmailAndCode(@Param("email") String email, @Param("code") String code);
	
	/**
	 * 清空指定邮箱的验证码
	 * @param email	指定邮箱
	 */
	void disableEmailCode(@Param("email") String email);
}