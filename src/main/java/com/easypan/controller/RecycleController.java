package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.service.FileInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("recycleController")
@RequestMapping("/recycle")
public class RecycleController extends ABaseController{
    
    @Resource
    private FileInfoService fileInfoService;

    /**
     * 加载回收站列表
     * @param session   session
     * @param pageNo    当前页数
     * @param pageSize  每页数量
     * @return
     */
    @RequestMapping("/loadRecycleList")
    @GlobalInterceptor
    public ResponseVO loadRecycleList(HttpSession session,Integer pageNo,Integer pageSize){
        FileInfoQuery query = new FileInfoQuery();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        query.setOrderBy("recovery_time desc");
        PaginationResultVO<FileInfo> result = this.fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * （批量）恢复回收站文件
     * @param session
     * @param fileIds
     * @return
     */
    @RequestMapping("/recoverFile")
    @GlobalInterceptor
    public ResponseVO recoverFile(HttpSession session,
                                  @VerifyParam(required = true) String fileIds){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.recoverFileBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor
    public ResponseVO delFile(HttpSession session,
                                  @VerifyParam(required = true) String fileIds){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.delFileBatch(webUserDto.getUserId(), fileIds,false);
        return getSuccessResponseVO(null);
    }
}
