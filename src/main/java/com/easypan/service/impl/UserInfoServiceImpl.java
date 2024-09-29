package com.easypan.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.QQInfoDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.enums.UserStatusEnum;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.service.EmailCodeService;
import com.easypan.utils.JsonUtils;
import com.easypan.utils.OKHttpUtils;
import com.easypan.utils.StringTools;
import org.apache.catalina.User;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import com.easypan.entity.query.SimplePage;
import com.easypan.enums.PageSize;
import com.easypan.service.UserInfoService;

import com.easypan.entity.po.UserInfo;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.entity.vo.PaginationResultVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description:InfoServiceImpl
 * @auther:wjc
 * @date:2024/06/28
 */
@Service("infoService")
public class UserInfoServiceImpl implements UserInfoService {
	
	private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);
	
	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	
	@Resource
	private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;
	
	@Resource
	private EmailCodeService emailCodeService;
	
	@Resource
	private RedisComponent redisComponent;
	
	@Resource
	private AppConfig appConfig;

	/**
	 * 根据条件查询列表
	 */
	public List<UserInfo> findListByParam(UserInfoQuery query) {
		return this.userInfoMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserInfoQuery query) {
		return this.userInfoMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(),count,pageSize);
		query.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(query);
		PaginationResultVO<UserInfo> result = new PaginationResultVO<>(count,page.getPageSize(),page.getPageNo(),page.getPageTotal(),list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据UserId查询
	 */
	public UserInfo getInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId更新
	 */
	public Integer updateInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean,userId);
	}

	/**
	 * 根据UserId删除
	 */
	public Integer deleteInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email查询
	 */
	public UserInfo getInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email更新
	 */
	public Integer updateInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean,email);
	}

	/**
	 * 根据Email删除
	 */
	public Integer deleteInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	/**
	 * 根据QqOpenId查询
	 */
	public UserInfo getInfoByQqOpenId(String qqOpenId) {
		return this.userInfoMapper.selectByQqOpenId(qqOpenId);
	}

	/**
	 * 根据QqOpenId更新
	 */
	public Integer updateInfoByQqOpenId(UserInfo bean, String qqOpenId) {
		return this.userInfoMapper.updateByQqOpenId(bean,qqOpenId);
	}

	/**
	 * 根据QqOpenId删除
	 */
	public Integer deleteInfoByQqOpenId(String qqOpenId) {
		return this.userInfoMapper.deleteByQqOpenId(qqOpenId);
	}

	/**
	 * 根据NickName查询
	 */
	public UserInfo getInfoByNickName(String nickName) {
		return this.userInfoMapper.selectByNickName(nickName);
	}

	/**
	 * 根据NickName更新
	 */
	public Integer updateInfoByNickName(UserInfo bean, String nickName) {
		return this.userInfoMapper.updateByNickName(bean,nickName);
	}

	/**
	 * 根据NickName删除
	 */
	public Integer deleteInfoByNickName(String nickName) {
		return this.userInfoMapper.deleteByNickName(nickName);
	}
	
	/**
	 *	注册
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void register(String email, String nickName, String emailCode, String password) {
		//校验邮箱账号是否已存在
		UserInfo emailUser = userInfoMapper.selectByEmail(email);
		if(emailUser != null){
			throw new BusinessException("当前邮箱账号已存在");
		}
		
		//校验昵称是否已存在
		UserInfo nickNameUser = userInfoMapper.selectByNickName(nickName);
		if(nickNameUser != null){
			throw new BusinessException("当前昵称已存在");
		}
		
		//校验邮箱验证码（错误会在checkCode()抛出异常）
		emailCodeService.checkCode(email,emailCode);
		
		//获取随机长度10位的userId
		String userId = StringTools.getRandomNumber(Constants.LENGTH_10);
		//封装用户信息
		UserInfo userInfo = new UserInfo();
		userInfo.setUserId(userId);
		userInfo.setNickName(nickName);
		userInfo.setEmail(email);
		userInfo.setPassword(StringTools.encodeByMd5(password));
		userInfo.setJoinTime(new Date());
		userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		//初始空间为0
		userInfo.setUseSpace(0L);
		
		SysSettingsDto sysSettingsDto = redisComponent.getSysSettingDto();
		//初始空间5MB
		userInfo.setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);
		userInfoMapper.insert(userInfo);
	}
	
	/**
	 * 登录
	 * @param email
	 * @param password
	 * @return
	 */
	@Override
	public SessionWebUserDto login(String email, String password) {
		UserInfo userInfo = userInfoMapper.selectByEmail(email);
		//校验账号密码
		//前端传来的密码经过md5加密，后端直接比对即可，不用去解密
		if(userInfo == null || !userInfo.getPassword().equals(password)){
			throw new BusinessException("账号或者密码错误");
		}
		
		//校验账号状态
		if(UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())){
			throw new BusinessException("账号已被禁用");
		}
		
		//更新账号登录时间
		UserInfo updateInfo = new UserInfo();
		updateInfo.setLastLoginTime(new Date());
		userInfoMapper.updateByUserId(updateInfo, userInfo.getUserId());
		
		//封装返回dto
		SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
		sessionWebUserDto.setNickName(userInfo.getNickName());
		sessionWebUserDto.setUserId(userInfo.getUserId());
		if(ArrayUtils.contains(appConfig.getAdminEmails().split(","),email)){
			sessionWebUserDto.setAdmin(true);
		}else {
			sessionWebUserDto.setAdmin(false);
		}
		
		//用户空间
		UserSpaceDto userSpaceDto = new UserSpaceDto();
		//查询当前用户已经上传文件大小总和
		Long useSpace = fileInfoMapper.selectUseSpace(userInfo.getUserId());
		userSpaceDto.setUseSpace(useSpace);
		userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
		redisComponent.saveUserSpaceUse(userInfo.getUserId(), userSpaceDto);
		
		return sessionWebUserDto;
	}
	
	/**
	 * 重置密码
	 * @param email
	 * @param password
	 * @param emailCode
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void resetPwd(String email, String password, String emailCode) {
		//校验邮箱是否存在
		UserInfo userInfo = userInfoMapper.selectByEmail(email);
		if(userInfo == null){
			throw new BusinessException("邮箱账号不存在");
		}
		//校验邮箱验证码
		emailCodeService.checkCode(email,emailCode);
		//更新密码
		UserInfo updateInfo = new UserInfo();
		updateInfo.setPassword(StringTools.encodeByMd5(password));
		userInfoMapper.updateByEmail(updateInfo,email);
	}
	
	/**
	 * qq登录	（与qq交互的核心在此）
	 * @param code
	 * @return
	 */
	@Override
	public SessionWebUserDto qqLogin(String code) {
		//第一步 通过回调code，获取accessToken
		String accessToken = getQQAccessToken(code);
		//第二步 获取qq openId
		String openId = getQQOpenId(accessToken);
		
		UserInfo user = this.userInfoMapper.selectByQqOpenId(openId);
		String avatar = null;
		if(user == null){	//证明该用户没有注册过，直接用qq登录了，帮他注册
			//自动注册
			//第三步 获取用户的qq基本信息
			QQInfoDto qqInfo = getQQUserInfo(accessToken,openId);
			
			user = new UserInfo();
			String nickName = qqInfo.getNickName();
			//保证昵称长度不超过20，数据库限定20.
			nickName = nickName.length() > Constants.LENGTH_20 ? nickName.substring(0,Constants.LENGTH_20) : nickName;
			//头像URL，不是每个用户都有2，但每个用户都有1
			avatar = StringTools.isEmpty(qqInfo.getFigureurl_qq_2()) ? qqInfo.getFigureurl_qq_1() : qqInfo.getFigureurl_qq_2();
			
			Date curDate = new Date();
			
			user.setQqOpenId(openId);
			user.setJoinTime(curDate);
			user.setNickName(nickName);
			user.setQqAvatar(avatar);
			user.setUserId(StringTools.getRandomString(Constants.LENGTH_10));
			user.setLastLoginTime(curDate);
			user.setStatus(UserStatusEnum.ENABLE.getStatus());
			user.setUseSpace(0L);
			user.setTotalSpace(redisComponent.getSysSettingDto().getUserInitUseSpace() * Constants.MB);
			userInfoMapper.insert(user);
			user = userInfoMapper.selectByQqOpenId(openId);
		}else {
			UserInfo updateInfo = new UserInfo();
			updateInfo.setLastLoginTime(new Date());
			avatar = user.getQqAvatar();
			userInfoMapper.updateByQqOpenId(updateInfo,openId);
		}
		
		SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
		sessionWebUserDto.setUserId(user.getUserId());
		sessionWebUserDto.setNickName(user.getNickName());
		sessionWebUserDto.setAvatar(avatar);
		if(ArrayUtils.contains(appConfig.getAdminEmails().split(","),user.getEmail() == null ? "" : user.getEmail())){
			sessionWebUserDto.setAdmin(true);
		}else {
			sessionWebUserDto.setAdmin(false);
		}
		//获取用户已使用的空间
		UserSpaceDto userSpaceDto = new UserSpaceDto();
		Long useSpace = fileInfoMapper.selectUseSpace(user.getUserId());
		userSpaceDto.setUseSpace(useSpace);
		userSpaceDto.setTotalSpace(user.getTotalSpace());
		redisComponent.saveUserSpaceUse(user.getUserId(),userSpaceDto);
		
		return sessionWebUserDto;
	}
	
	private String getQQAccessToken(String code){
		String accessToken = null;
		String url = null;
		try{
			url = String.format(appConfig.getQqUrlAccessToken(),appConfig.getQqAppId(),appConfig.getQqAppKey(),code, URLEncoder.encode(appConfig.getQqUrlRedirect(),"utf-8"));
		}catch (UnsupportedEncodingException e){
			logger.error("encode失败");
		}
		String tokenResult = OKHttpUtils.getRequest(url);
		if(tokenResult == null || tokenResult.indexOf(Constants.VIEW_OBJ_RESULT_KEY) != -1){
			logger.error("获取qqToken失败：{}",tokenResult);
			throw new BusinessException("获取qqToken失败");
		}
		String[] params = tokenResult.split("&");
		if(params != null && params.length > 0){
			for(String p : params){
				if(p.indexOf("access_token") != -1){
					accessToken = p.split("=")[1];
					break;
				}
			}
		}
		return accessToken;
	}
	
	private String getQQOpenId(String accessToken) throws BusinessException{
		//获取openId
		String url = String.format(appConfig.getQqUrlOpenId(),accessToken);
		String openIdResult = OKHttpUtils.getRequest(url);
		String tmpJson = this.getQQResp(openIdResult);
		if(tmpJson == null){
			logger.error("调qq接口获取openId失败：tmpJson：{}",tmpJson);
			throw new BusinessException("调qq接口获取openId失败");
		}
		Map jsonData = JsonUtils.convertJson2Obj(tmpJson,Map.class);
		if(jsonData == null || jsonData.containsKey(Constants.VIEW_OBJ_RESULT_KEY)){
			logger.error("调qq接口获取openId失败");
			throw new BusinessException("调qq接口获取openId失败");
		}
		return String.valueOf(jsonData.get("openid"));
	}
	
	private String getQQResp(String result){
		if(StringUtils.isNotBlank(result)){
			int pos = result.indexOf("callback");
			if(pos != -1){
				int start = result.indexOf("(");
				int end = result.lastIndexOf(")");
				String jsonStr = result.substring(start + 1,end -1);
				return jsonStr;
			}
		}
		return null;
	}
	
	private QQInfoDto getQQUserInfo(String accessToken,String qqOpenId) throws  BusinessException{
		String url = String.format(appConfig.getQqUrlUserInfo(),accessToken,appConfig.getQqAppId(),qqOpenId);
		String response = OKHttpUtils.getRequest(url);
		if(StringUtils.isNotBlank(response)){
			QQInfoDto qqInfo = JsonUtils.convertJson2Obj(response, QQInfoDto.class);
			if(qqInfo.getRet() != 0){
				logger.error("qqInfo:{}",response);
				throw new BusinessException("调qq接口获取用户信息异常");
			}
			return qqInfo;
		}
		throw new BusinessException("调qq接口获取用户信息异常");
	}

	/**
	 * 修改用户状态
	 * @param userId	用户id
	 * @param status	要修改的状态
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUserStatus(String userId, Integer status) {
		UserInfo userInfo = new UserInfo();
		userInfo.setStatus(status);
		if(UserStatusEnum.DISABLE.getStatus().equals(status)){	//禁用的用户
			//空间清0
			userInfo.setUseSpace(0L);
			//文件全部删除
			fileInfoMapper.deleteFileByUserId(userId);
		}
		userInfoMapper.updateByUserId(userInfo,userId);
	}

	/**
	 * 修改用户空间
	 * @param userId	用户id
	 * @param changeSpace	分配的空间
	 */
	@Override
	public void changeUserSpace(String userId, Integer changeSpace) {
		Long space = changeSpace * Constants.MB;
		this.userInfoMapper.updateUserSpace(userId,null,space);	//改的是总空间
		redisComponent.resetUserSpaceUse(userId);
	}
}