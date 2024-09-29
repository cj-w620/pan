package com.easypan.controller;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.enums.ResponseCodeEnum;

import com.easypan.entity.vo.ResponseVO;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ABaseController {
    private static final Logger logger = LoggerFactory.getLogger(ABaseController.class);
    
    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    //封装成功返回VO
    protected <T> ResponseVO getSuccessResponseVO(T t){
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }
    
    /**
     * 读取文件
     * @param response
     * @param filePath
     */
    protected void readFile(HttpServletResponse response,String filePath){
        //检查路径
        if(!StringTools.pathIsOk(filePath)){
            return;
        }
        OutputStream out = null;
        FileInputStream in = null;
        try{
            File file = new File(filePath);
            if(!file.exists()){
                return;
            }
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(byteData)) != -1){
                out.write(byteData,0,len);
            }
            out.flush();
        }catch (Exception e){
            logger.error("读取文件异常",e);
        }finally {
            if(out != null){
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("IO异常",e);
                }
            }
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO异常",e);
                }
            }
        }
    }
    
    /**
     * 拿到用户信息（登录时存入session中的）
     * @param session
     * @return
     */
    protected SessionWebUserDto getUserInfoFromSession(HttpSession session){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return sessionWebUserDto;
    }

    /**
     * 拿到分享信息
     * @param session
     * @param shareId
     * @return
     */
    protected SessionShareDto getSessionShareFromSession(HttpSession session, String shareId){
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY+shareId);
        return sessionShareDto;
    }
    
    
    /**
     * 转换返回类型，分页查询出的数据，若不需要返回全部属性，则将其转换为有效属性实体类
     * @param result
     * @param classz
     * @return
     * @param <S>   数据库查询出的属性对应实体类
     * @param <T>   要转换为的、返回的、有效属性的 实体类
     */
    protected <S,T> PaginationResultVO<T> convert2PaginationVO(PaginationResultVO<S> result,Class<T> classz){
        PaginationResultVO<T> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList(result.getList(),classz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }
    
}
