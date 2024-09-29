package com.easypan.service;

import java.util.List;

import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.entity.vo.PaginationResultVO;
/**
 * @Description:InfoService
 * @auther:wjc
 * @date:2024/06/28
 */
public interface UserInfoService {
	/**
	 * 根据条件查询列表
	 */
	List<UserInfo> findListByParam(UserInfoQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserInfoQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query);

	/**
	 * 新增
	 */
	Integer add(UserInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserInfo> listBean);

	/**
	 * 批量新增或修改
	 */
	Integer addOrUpdateBatch(List<UserInfo> listBean);

	/**
	 * 根据UserId查询
	 */
	UserInfo getInfoByUserId(String userId);

	/**
	 * 根据UserId更新
	 */
	 Integer updateInfoByUserId(UserInfo bean, String userId);

	/**
	 * 根据UserId删除
	 */
	 Integer deleteInfoByUserId(String userId);

	/**
	 * 根据Email查询
	 */
	UserInfo getInfoByEmail(String email);

	/**
	 * 根据Email更新
	 */
	 Integer updateInfoByEmail(UserInfo bean, String email);

	/**
	 * 根据Email删除
	 */
	 Integer deleteInfoByEmail(String email);

	/**
	 * 根据QqOpenId查询
	 */
	UserInfo getInfoByQqOpenId(String qqOpenId);

	/**
	 * 根据QqOpenId更新
	 */
	 Integer updateInfoByQqOpenId(UserInfo bean, String qqOpenId);

	/**
	 * 根据QqOpenId删除
	 */
	 Integer deleteInfoByQqOpenId(String qqOpenId);

	/**
	 * 根据NickName查询
	 */
	UserInfo getInfoByNickName(String nickName);

	/**
	 * 根据NickName更新
	 */
	 Integer updateInfoByNickName(UserInfo bean, String nickName);

	/**
	 * 根据NickName删除
	 */
	 Integer deleteInfoByNickName(String nickName);
	
	/**
	 * 注册
	 */
	void register(String email,String nickName,String emailCode,String password);
	
	/**
	 * 登录
	 */
	SessionWebUserDto login(String email,String password);
	
	/**
	 * 重置密码
	 */
	void resetPwd(String email,String password,String emailCode);
	
	/**
	 * qq登录
	 */
	SessionWebUserDto qqLogin(String code);

	/**
	 * 修改用户状态
	 */
	void updateUserStatus(String userId,Integer status);

	/**
	 * 修改用户空间
	 */
	void changeUserSpace(String userId,Integer changeSpace);
}