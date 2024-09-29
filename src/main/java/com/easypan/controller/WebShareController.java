package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.entity.vo.ShareInfoVO;
import com.easypan.enums.FileCategoryEnum;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileInfoService;
import com.easypan.service.FileShareService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController("webShareController")
@RequestMapping("/showShare")
public class WebShareController extends CommonFileController{
    
    @Resource
    private FileShareService fileShareService;
    
    @Resource
    private FileInfoService fileInfoService;
    
    @Resource
    private UserInfoService userInfoService;
    

    /*
    * 用户点进分享链接，首先是getShareLoginInfo这个接口，该接口去session中拿分享信息，
    *   没拿到：数据返回null，前端调用getShareInfo接口获取分享信息，页面展示分享信息，让用户输入提取码
    *           输入提取码后，前端调用checkShareCode接口校验提取码，并把分析信息放入session中
    *   拿到：
    * 所以可以理解为，这个接口就是校验是否正确输入过提取码的，如果正确输入过，就可以拿到分享信息，不用再次输入，直接进分享页面。
    * 所以，第一个调的是getShareLoginInfo接口。
    * */
    /**
     * 获取分享登录信息
     * @param session
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId){
        //从session中拿分享信息
        SessionShareDto sessionShareDto = getSessionShareFromSession(session,shareId);
        //分享信息为空，返回数据为null，前端会去拿分享信息
        if(sessionShareDto == null){
            return getSuccessResponseVO(null);
        }
        
        //分享信息不为空，
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        //判断是否是当前用户分享的文件
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if(userDto != null && userDto.getUserId().equals(sessionShareDto.getShareUserId())){
            shareInfoVO.setCurrentUser(true);
        }else {
            shareInfoVO.setCurrentUser(false);
        }
        
        return getSuccessResponseVO(shareInfoVO);
    }
    
    
    /**
     * 获取分享信息
     * @param shareId   分享id
     * @return
     */
    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO getShareInfo( @VerifyParam(required = true) String shareId){
        return getSuccessResponseVO(getShareInfoCommon(shareId));
    }

    /**
     * 获取分享信息   （因为有多个方法会用到，所以我们抽出来作为一个方法）
     * @param shareId
     * @return
     */
    private ShareInfoVO getShareInfoCommon(String shareId){
        FileShare share = fileShareService.getFileShareByShareId(shareId);
        //分享id查不到分享实体 或者 当分享文件具有有效期且超时了 返回错误
        if(share == null || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))){
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        
        //转变为真正返回类
        ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
        //获取文件
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(),share.getUserId());
        //文件为空 或 不是使用中 （可能被用户删除了）
        if(fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())){
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        
        //封装返回类
        shareInfoVO.setFileName(fileInfo.getFileName());
        UserInfo userInfo = userInfoService.getInfoByUserId(share.getUserId());
        shareInfoVO.setNickName(userInfo.getNickName());
        shareInfoVO.setAvatar(userInfo.getQqAvatar());
        shareInfoVO.setUserId(userInfo.getUserId());
        return shareInfoVO;
    }

    /**
     * 校验提取码
     * @param session
     * @param shareId
     * @param code
     * @return
     */
    @RequestMapping("/checkShareCode")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO checkShareCode(HttpSession session,
                                     @VerifyParam(required = true) String shareId,
                                     @VerifyParam(required = true) String code){
        SessionShareDto sessionShareDto = this.fileShareService.checkShareCode(shareId, code);
        session.setAttribute(Constants.SESSION_SHARE_KEY + shareId,sessionShareDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 加载文件列表
     */
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO loadFileList(HttpSession session,
                                   @VerifyParam(required = true) String shareId,
                                   String filePid) {
        SessionShareDto sessionShareDto = checkShare(session,shareId);
        FileInfoQuery query = new FileInfoQuery();
        if(!StringTools.isEmpty(filePid) && !Constants.ZERO_STR.equals(filePid)){
            fileInfoService.checkRootFilePid(sessionShareDto.getFileId(),sessionShareDto.getShareUserId(),filePid);
            query.setFilePid(filePid);
        }else {
            query.setFileId(sessionShareDto.getFileId());
        }
        query.setUserId(sessionShareDto.getShareUserId());
        query.setOrderBy("last_update_time desc");	//根据最后更新时间倒序排
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);

        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 检查session中的分享信息是否存在 以及 是否超时
     * @param session
     * @param shareId
     * @return
     */
    private SessionShareDto checkShare(HttpSession session,String shareId){
        SessionShareDto shareSessionDto = getSessionShareFromSession(session,shareId);
        
        if(shareSessionDto == null){
            throw new BusinessException(ResponseCodeEnum.CODE_903);
        }
        
        if(shareSessionDto.getExpireTime() != null && new Date().after(shareSessionDto.getExpireTime())){
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        
        return shareSessionDto;
    }

    /**
     * 获取目录信息
     * @param session
     * @param shareId
     * @param path
     * @return
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO newFoloder(HttpSession session,
                                 @VerifyParam(required = true) String shareId,
                                 @VerifyParam(required = true) String path){
        SessionShareDto sessionShareDto = checkShare(session,shareId);
        return super.getFolderInfo(path, sessionShareDto.getShareUserId());
    }

    /**
     * 获取文件信息
     * @param response
     * @param fileId
     * @param shareId
     */
    @RequestMapping("/getFile/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void getFile(HttpServletResponse response,
                        HttpSession session,
                        @PathVariable("fileId") String fileId,
                        @PathVariable("shareId") String shareId){
        SessionShareDto sessionShareDto = checkShare(session,shareId);
        super.getFile(response,fileId,sessionShareDto.getShareUserId());
    }

    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void getVideoInfo(HttpServletResponse response,
                             HttpSession session,
                             @PathVariable("fileId") String fileId,
                             @PathVariable("shareId") String shareId){
        SessionShareDto sessionShareDto = checkShare(session,shareId);
        super.getFile(response,fileId,sessionShareDto.getShareUserId());
    }

    /**
     * 创建下载链接
     * @param session
     * @param shareId
     * @param fileId
     * @return
     */
    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO createDownloadUrl(HttpSession session,
                                        @PathVariable("shareId") String shareId,
                                        @PathVariable("fileId") String fileId){
        SessionShareDto sessionShareDto = checkShare(session,shareId);
        return super.createDownloadUrl(fileId,sessionShareDto.getShareUserId());
    }

    /**
     * 下载文件
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
     * 保存到网盘
     * @param session   session
     * @param shareId   分享id
     * @param shareFileIds  要保存的分享文件的id
     * @param myFolderId    保存到的目录（目标目录）的id
     * @return
     */
    @RequestMapping("/saveShare")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO saveShare(HttpSession session,
                                @VerifyParam(required = true) String shareId,
                                @VerifyParam(required = true) String shareFileIds,
                                @VerifyParam(required = true) String myFolderId){
        SessionShareDto shareSessionDto = checkShare(session,shareId);
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        //检查：不允许自己保存自己分享的文件
        if(shareSessionDto.getShareUserId().equals(webUserDto.getUserId())){
            throw new BusinessException("无法保存自己分享的文件到自己的网盘");
        }
        
        fileInfoService.saveShare(shareSessionDto.getFileId(),shareFileIds,myFolderId, shareSessionDto.getShareUserId(), webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }
    
    
    
}
