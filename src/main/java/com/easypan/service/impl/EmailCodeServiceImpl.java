package com.easypan.service.impl;

import java.util.Date;
import java.util.List;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.utils.StringTools;
import com.sun.mail.smtp.SMTPSendFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

import com.easypan.entity.query.SimplePage;
import com.easypan.enums.PageSize;
import com.easypan.service.EmailCodeService;

import com.easypan.entity.po.EmailCode;
import com.easypan.mappers.EmailCodeMapper;
import com.easypan.entity.query.EmailCodeQuery;
import com.easypan.entity.vo.PaginationResultVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description:EmailCodeServiceImpl
 * @auther:wjc
 * @date:2024/06/30
 */
@Service("emailCodeService")
public class EmailCodeServiceImpl implements EmailCodeService{
	
	private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);
	@Resource
	private EmailCodeMapper<EmailCode,EmailCodeQuery> emailCodeMapper;

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	
	@Resource
	private JavaMailSender javaMailSender;
	
	@Resource
	private AppConfig appConfig;
	
	@Resource
	private RedisComponent redisComponent;
	
	/**
	 * 根据条件查询列表
	 */
	public List<EmailCode> findListByParam(EmailCodeQuery query) {
		return this.emailCodeMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(EmailCodeQuery query) {
		return this.emailCodeMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<EmailCode> findListByPage(EmailCodeQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(),count,pageSize);
		query.setSimplePage(page);
		List<EmailCode> list = this.findListByParam(query);
		PaginationResultVO<EmailCode> result = new PaginationResultVO<>(count,page.getPageSize(),page.getPageNo(),page.getPageTotal(),list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(EmailCode bean) {
		return this.emailCodeMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<EmailCode> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.emailCodeMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<EmailCode> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.emailCodeMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据EmailAndCode查询
	 */
	public EmailCode getEmailCodeByEmailAndCode(String email, String code) {
		return this.emailCodeMapper.selectByEmailAndCode(email, code);
	}

	/**
	 * 根据EmailAndCode更新
	 */
	public Integer updateEmailCodeByEmailAndCode(EmailCode bean,String email, String code) {
		return this.emailCodeMapper.updateByEmailAndCode(bean,email, code);
	}

	/**
	 * 根据EmailAndCode删除
	 */
	public Integer deleteEmailCodeByEmailAndCode(String email, String code) {
		return this.emailCodeMapper.deleteByEmailAndCode(email, code);
	}
	
	/**
	 * 发送邮箱验证码
	 * @param email
	 * @param type
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void sendEmailCode(String email, Integer type){
		if(type == Constants.ZERO){	//0是注册
			UserInfo userInfo = userInfoMapper.selectByEmail(email);
			if(userInfo != null){
				throw new BusinessException("邮箱已存在");
			}
		}
		
		//生成随机验证码
		String code = StringTools.getRandomNumber(Constants.LENGTH_5);
		
		//发送验证码
		sendMailCode(email,code);
		
		//为防止多次发送，存在多个验证码的情况，每次存入前，先将当前邮箱之前的验证码置为无效。
		emailCodeMapper.disableEmailCode(email);
		
		//邮箱验证码存入数据库
		EmailCode emailCode = new EmailCode();
		emailCode.setCode(code);
		emailCode.setEmail(email);
		emailCode.setStatus(Constants.ZERO); //0：未使用
		emailCode.setCreateTime(new Date());
		emailCodeMapper.insert(emailCode);
	}
	
	private void sendMailCode(String toEmail,String code){
		try{
			MimeMessage message = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message,true);
			helper.setFrom(appConfig.getSendUserName()); //谁发
			helper.setTo(toEmail);	//发给谁
			
			SysSettingsDto sysSettingsDto = redisComponent.getSysSettingDto();
			helper.setSubject(sysSettingsDto.getRegisterEmailTitle());	//邮件标题
			helper.setText(String.format(sysSettingsDto.getRegisterEmailContent(),code));	//邮件内容
			
			helper.setSentDate(new Date());	//发送时间
			javaMailSender.send(message);
		}catch (Exception e){
			logger.error("邮件发送失败，检查redis是否启动/收件人邮箱是否存在",e);
			throw new BusinessException("邮件发送失败");
		}
		
	}
	
	/**
	 * 校验邮箱验证码
	 * @param email
	 * @param code
	 */
	@Override
	public void checkCode(String email, String code) {
		EmailCode emailCode = emailCodeMapper.selectByEmailAndCode(email,code);
		if(emailCode == null){
			throw new BusinessException("邮箱验证码错误");
		}
		
		//15分钟有效
		if(emailCode.getStatus() == 1 || System.currentTimeMillis() - emailCode.getCreateTime().getTime() > Constants.LENGTH_15 * 1000 * 60){
			throw new BusinessException("该邮箱验证码已失效");
		}
		
		//将该邮箱的验证码都设为过期
		emailCodeMapper.disableEmailCode(email);
	}
}