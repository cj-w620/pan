package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.query.FileShareQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.service.FileInfoService;
import com.easypan.service.FileShareService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("shareController")
@RequestMapping("/share")
public class ShareController extends ABaseController{
    
    @Resource
    private FileShareService fileShareService;

    /**
     * 加载分享列表
     * @param session
     * @param query
     * @return
     */
    @RequestMapping("/loadShareList")
    @GlobalInterceptor
    //可以直接传pageNo，pageSize；也可以传FileShareQuery，会自动封装。
    public ResponseVO loadShareList(HttpSession session, FileShareQuery query){
        
        query.setOrderBy("share_time desc");
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        query.setUserId(webUserDto.getUserId());
        query.setQueryFileName(true);
        PaginationResultVO result = fileShareService.findListByPage(query);
        System.out.println(result);
        return getSuccessResponseVO(result);
    }

    /**
     * 分享文件
     * @param session   session
     * @param fileId    文件id
     * @param validType 有效期类型
     * @param code  提取码 （前端没传就自动生成）
     * @return
     */
    @RequestMapping("/shareFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO shareFile(HttpSession session,
                                @VerifyParam(required = true) String fileId,
                                @VerifyParam(required = true) Integer validType,
                                String code){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileShare share = new FileShare();
        share.setCode(code);
        share.setValidType(validType);
        share.setUserId(webUserDto.getUserId());
        share.setFileId(fileId);
        fileShareService.saveShare(share);
        return getSuccessResponseVO(share);
    }

    /**
     * （批量）取消文件分享
     * @param session
     * @param shareIds
     * @return
     */
    @RequestMapping("/cancelShare")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO cancelShare(HttpSession session,
                                @VerifyParam(required = true) String shareIds){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileShareService.deleteFileShareBatch(shareIds.split(","), webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
