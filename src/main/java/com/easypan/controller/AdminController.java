package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisComponent;

import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.query.UserInfoQuery;

import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.entity.vo.UserInfoVO;
import com.easypan.service.FileInfoService;

import com.easypan.service.UserInfoService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @Description:AdminController
 * @auther:wjc
 * @date:2024/08/21
 */
@RestController("adminController")
@RequestMapping("/admin")
public class AdminController extends CommonFileController {
	@Resource
	private FileInfoService fileInfoService;

	@Resource
    private RedisComponent redisComponent;
    
    @Resource
    private UserInfoService userInfoService;

    /**
     * 获取系统设置
     * @return
     */
    @RequestMapping("/getSysSettings")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO getSysSettings(){
        return getSuccessResponseVO(redisComponent.getSysSettingDto());
    }

    /**
     * 保存系统设置
     * @param registerEmailTitle    默认邮件标题
     * @param registerEmailContent  默认邮件内容
     * @param userInitUseSpace  使用空间大小，单位为兆（M）
     * @return  
     */
    @RequestMapping("/saveSysSettings")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO saveSysSetting(@VerifyParam(required = true) String registerEmailTitle,
                                     @VerifyParam(required = true) String registerEmailContent,
                                     @VerifyParam(required = true) Integer userInitUseSpace){
        SysSettingsDto sysSettingsDto = new SysSettingsDto();
        sysSettingsDto.setRegisterEmailTitle(registerEmailTitle);
        sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
        sysSettingsDto.setRegisterEmailContent(registerEmailContent);
        redisComponent.saveSysSettingsDto(sysSettingsDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 加载用户列表
     * @param userInfoQuery
     * @return
     */
    @RequestMapping("/loadUserList")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO loadUserList(UserInfoQuery userInfoQuery){
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(convert2PaginationVO(resultVO, UserInfoVO.class));
    }

    /**
     * 调整用户状态
     * @param userId
     * @param status
     * @return
     */
    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO updateUserStatus(@VerifyParam(required = true) String userId,
                                       @VerifyParam(required = true) Integer status){
        userInfoService.updateUserStatus(userId,status);
        return getSuccessResponseVO(null);
    }

    /**
     * 调整用户空间
     * @param userId
     * @param changeSpace
     * @return
     */
    @RequestMapping("/updateUserSpace")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO updateUserSpace(@VerifyParam(required = true) String userId,
                                       @VerifyParam(required = true) Integer changeSpace){
        userInfoService.changeUserSpace(userId,changeSpace);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO loadFileList(FileInfoQuery query) {
        query.setOrderBy("last_update_time desc");	//根据最后更新时间倒序排
        query.setQueryNickName(true);   //做关联子查询，查发布人
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(result);
    }

    /**
     * 导航栏信息
     * @param path
     * @return
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO loadFileList(@VerifyParam(required = true) String path) {
        return super.getFolderInfo(path,null);
    }

    /**
     * 获取文件信息
     * @param response
     * @param fileId
     * @param userId
     */
    @RequestMapping("/getFile/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public void getFile(HttpServletResponse response,
                        @PathVariable("fileId") String fileId,
                        @PathVariable("userId") String userId){
        super.getFile(response,fileId,userId);
    }

    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("fileId") String fileId,
                             @PathVariable("userId") String userId){
        super.getFile(response,fileId, userId);
    }

    /**
     * 创建下载链接
     * @param userId    文件id
     * @param fileId    用户id
     * @return
     */
    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO createDownloadUrl(@PathVariable("userId") String userId,
                                        @PathVariable("fileId") String fileId){
        return super.createDownloadUrl(fileId,userId);
    }

    /**
     * 下载
     * @param request
     * @param response
     * @param code
     * @throws Exception
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @VerifyParam(required = true) @PathVariable("code") String code) throws Exception {
        super.download(request,response,code);
    }

    /**
     * 删除文件
     * @param fileIdAndUserIds  {fileId}_{userId},{fileId}_{userId}...
     * @return
     */
    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO delFile(@VerifyParam(required = true) String fileIdAndUserIds){
        String[] fileIdAndeUserIdArray = fileIdAndUserIds.split(",");
        for(String fileIdAndeUserId : fileIdAndeUserIdArray){
            String[] itemArray = fileIdAndeUserId.split("_");
            fileInfoService.delFileBatch(itemArray[0],itemArray[1],true);
        }
        return getSuccessResponseVO(null);
    }
}