package com.easypan.service;

import java.io.Serializable;
import java.util.List;

import com.easypan.entity.po.EmailCode;
import com.easypan.entity.query.EmailCodeQuery;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.exception.BusinessException;

/**
 * @Description:EmailCodeService
 * @auther:wjc
 * @date:2024/06/30
 */
public interface EmailCodeService {
	/**
	 * 根据条件查询列表
	 */
	List<EmailCode> findListByParam(EmailCodeQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(EmailCodeQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<EmailCode> findListByPage(EmailCodeQuery query);

	/**
	 * 新增
	 */
	Integer add(EmailCode bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<EmailCode> listBean);

	/**
	 * 批量新增或修改
	 */
	Integer addOrUpdateBatch(List<EmailCode> listBean);

	/**
	 * 根据EmailAndCode查询
	 */
	EmailCode getEmailCodeByEmailAndCode(String email, String code);

	/**
	 * 根据EmailAndCode更新
	 */
	 Integer updateEmailCodeByEmailAndCode(EmailCode bean,String email, String code);

	/**
	 * 根据EmailAndCode删除
	 */
	 Integer deleteEmailCodeByEmailAndCode(String email, String code);
	
	/**
	 *	发送邮箱验证码
	 */
	 void sendEmailCode(String email,Integer type) throws BusinessException;
	
	/**
	 *	检验邮箱验证码
	 */
	void checkCode(String email,String code);
}