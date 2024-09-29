package com.easypan.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.CreateImageCode;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.enums.VerifyRegexEnum;
import com.easypan.exception.BusinessException;
import com.easypan.service.EmailCodeService;

import com.easypan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.easypan.service.UserInfoService;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 */
@RestController("userInfoController")
public class AccountController extends ABaseController {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";
    
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Resource
    private UserInfoService userInfoService;
    
    @Resource
    private EmailCodeService emailCodeService;
    
    @Resource
    private AppConfig appConfig;
    
    @Resource
    private RedisComponent redisComponent;
    /**
     * 获取验证码
     *
     * @param type 0：登录注册 1：邮箱验证码发送
     */
    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }
    
    /**
     * 发送邮箱验证码
     *
     * @param type 0：注册  1：找回密码
     */
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO sendEmailCode(HttpSession session,
                                    @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                                    @VerifyParam(required = true) String checkCode,
                                    @VerifyParam(required = true) Integer type) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图片验证码错误");
            }
            emailCodeService.sendEmailCode(email, type);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }
    
    /**
     * 注册
     */
    @RequestMapping("/register")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO register(HttpSession session,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String nickName,
                               @VerifyParam(required = true, min = 8, max = 18) String password,
                               @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.register(email, nickName, emailCode, password);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    
    /**
     * 登录
     */
    @RequestMapping("/login")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO login(HttpSession session,
                            @VerifyParam(required = true) String email,
                            @VerifyParam(required = true) String checkCode,
                            @VerifyParam(required = true) String password) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码错误");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password);
            //用户信息dto放入session域中
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            logger.info("用户=="+sessionWebUserDto.getNickName()+"==登录");
            logger.info("登录时间："+dateFormat.format(new Date()));
            return getSuccessResponseVO(sessionWebUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    
    /**
     * 找回密码
     */
    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO resetPwd(HttpSession session,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true, min = 8, max = 18) String password,
                               @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
	
	/**
	 * 获取头像
	 * @param response
	 * @param userId
	 */
    @RequestMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void getAvatar(HttpServletResponse response,
                          @VerifyParam(required = true) @PathVariable("userId") String userId) {
		//头像子目录 /file/avatar/
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
		//完整头像目录	{projectFolder}/file/avatar/
        File folder = new File(appConfig.getProjectFolder() + avatarFolderName);
        if (!folder.exists()) {	//目录不存在，创建
            folder.mkdirs();
        }
		//头像文件地址	{projectFolder}/file/avatar/{userId}.jpg
        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
       
        File file = new File(avatarPath);
        if (!file.exists()) {	//没有当前用户设置的头像
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFAULT).exists()) { //没有默认头像，提示
                printNoDefaultImage(response);
            }
			//默认头像
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFAULT;
        }
        response.setContentType("image/jpg");
		//读取头像文件并写出
        readFile(response, avatarPath);
    }

    /**
     * 无默认头像时，提示
     * @param response
     */
    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        } finally {
            writer.close();
        }
    }
    
    
    /**
     * 获取用户空间
     * @param session
     * @return
     */
    @RequestMapping("/getUseSpace")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getUseSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId());
        return getSuccessResponseVO(spaceDto);
    }
    
    /**
     * 退出登录
     * @param session
     * @return
     */
    @RequestMapping("/logout")
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }
    
    /**
     * 上传头像
     * @param session
     * @return
     */
    @RequestMapping("/updateUserAvatar")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        //  D:/project/project_log/easypan/ + /file/
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        //头像存放目录
        //  D:/project/project_log/easypan//file/avatar
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        //头像文件，路径为“头像存放目录/[userId].jpg”
        //一个用户一个头像
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        
        if(!targetFileFolder.exists()){ //目录不存在，先创建
            targetFileFolder.mkdirs();
        }
        try {
            avatar.transferTo(targetFile);  //transferTo()：将接收到的文件传输到给定的目标文件。
        }catch (Exception e){
            logger.error("头像上传失败",e);
        }
        
        //自己本地上传头像后，就使用上传的头像，不用qq头像了，所以把qq头像地址设置为空。
        UserInfo userInfo = new UserInfo();
        userInfo.setQqAvatar("");
        userInfoService.updateInfoByUserId(userInfo, webUserDto.getUserId());
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY,webUserDto);
        
        return getSuccessResponseVO(null);
    }
    
    /**
     * 更新密码（登录后可以在里面改密码，忘记密码是在登录前的。）
     * @param session
     * @return
     */
    @RequestMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updatePassword(HttpSession session,
                                     @VerifyParam(required = true,regex = VerifyRegexEnum.PASSWORD,min = 8,max = 18) String password) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMd5(password));
        userInfoService.updateInfoByUserId(userInfo,sessionWebUserDto.getUserId());
        return getSuccessResponseVO(null);
    }
    
    /*qq登录具体流程：
    *   首先通过qq.url.authorization=https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s
    *   去做qq认证，会弹出二维码给你认证，然后跳转到redirect_uri页面去。给你一个code
    *   然后调用下面地址即可
    *   qq.url.access.token=https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s
    *
    * */
    
    /**
     * qq登录
     * @param session
     * @return
     */
    @RequestMapping("/qqlogin")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO qqlogin(HttpSession session,String callbackUrl) throws UnsupportedEncodingException {
        String state = StringTools.getRandomNumber(Constants.LENGTH_30);
        //callbackUrl可传可不传
        if(!StringTools.isEmpty(callbackUrl)){  //传了，放入session中
            session.setAttribute(state,callbackUrl);
        }
        String url = String.format(appConfig.getQqUrlAuthorization(),appConfig.getQqAppId(), URLEncoder.encode(appConfig.getQqUrlRedirect(),"utf-8"),state);
        return getSuccessResponseVO(url);
    }
    
    @RequestMapping("/qqlogin/callback")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO qqloginCallback(HttpSession session,
                                      @VerifyParam(required = true) String code,
                                      @VerifyParam(required = true) String state)  {
        SessionWebUserDto sessionWebUserDto = userInfoService.qqLogin(code);
        session.setAttribute(Constants.SESSION_KEY,sessionWebUserDto);
        Map<String,Object> result = new HashMap<>();
        result.put("callbackUrl",session.getAttribute(state));  
        result.put("userInfo",sessionWebUserDto);
        return getSuccessResponseVO(result);
    }
}