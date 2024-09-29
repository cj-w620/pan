package com.easypan.controller;

import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.FolderVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.enums.FileCategoryEnum;
import com.easypan.enums.FileFolderTypeEnums;
import com.easypan.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.net.URLEncoder;
import java.util.List;

public class CommonFileController extends ABaseController{
    
    @Resource
    private AppConfig appConfig;
    
    @Resource
    private FileInfoService fileInfoService;
    
    @Resource 
    private RedisComponent redisComponent;
    
    /**
     * 图片预览
     * @param response
     * @param imageFolder
     * @param imageName
     */
    protected void getImage(HttpServletResponse response,String imageFolder,String imageName){
        if(StringTools.isEmpty(imageFolder) || StringTools.isEmpty(imageName) || !StringTools.pathIsOk(imageFolder) || !StringTools.pathIsOk(imageName)){
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".","");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control","max-age=2592000");
        readFile(response,filePath);
    }
    
    /*
    *   用于预览文件
    * 视频预览，需单独处理。在云盘目录中点击视频后，需要播放视频进行视频预览。
    * 点击视频后，先发一个请求。目的是获取视频的m38u文件，有了这个文化，就能知道整体视频被切割成多少个分片，视频时长有多少。
    * 以便显示出视频时长，并发送获取切片视频请求。
    * 接下来会发送切片视频请求，后端就返回以".ts"结尾的切片视频，前端会根据m38u文件信息发送数个，后端也会返回数个，组成一个完整的视频在前端播放。
    * */
    protected void getFile(HttpServletResponse response,String fileId,String userId){
        
        String filePath = null;
        
       
        if(fileId.endsWith(".ts")){  //ts文件
            String[] tsArray = fileId.split("_");
            String realFileId = tsArray[0];
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId,userId);
            if(fileInfo == null){
                return;
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        }else{  //非ts文件
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId,userId);
            if(fileInfo == null){
                return;
            }
     
            if(FileCategoryEnum.VIDEO.getCategory().equals(fileInfo.getFileCategory())){    //视频文件
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            }else{  //非视频文件
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
            File file = new File(filePath);
            if(!file.exists()){
                return;
            }
        }
        
        
        readFile(response,filePath);
    }
    
    /**
     * 得到目录信息，为显示导航栏
     * @param path  根目录后，按"/"分割的路径   每个目录的path就是其fileId
     * @param userId
     * @return
     */
    protected ResponseVO getFolderInfo(String path,String userId){
        String[] pathArray = path.split("/");
        FileInfoQuery infoQuery = new FileInfoQuery();
        infoQuery.setUserId(userId);
        infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        infoQuery.setFileIdArray(pathArray);
        //按照传进来的顺序，去排序，拿到每层目录
        //sql: field(file_id,"qwerasdfzx","yuiohjklnm") ：查询出来的，会按file_id字段，值为"qwerasdfzx"在前，值为"yuiohjklnm"在后
        String orderBy = "field(file_id,\""+ StringUtils.join(pathArray,"\",\"") +"\")";
        infoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FolderVO.class));
    }

    /**
     * 创建下载链接
     * @param fileId    文件id
     * @param userId    用户id
     * @return
     */
    protected ResponseVO createDownloadUrl(String fileId,String userId){
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId,userId);
        //不走界面？out!
        if(fileInfo == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //不给下载目录哦
        if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        
        //获取随机code
        String code = StringTools.getRandomString(Constants.LENGTH_50);
        
        //封装fileDto，放入redis，有效期为5min
        DownloadFileDto fileDto = new DownloadFileDto();
        fileDto.setDownloadCode(code);
        fileDto.setFilePath(fileInfo.getFilePath());
        fileDto.setFileName(fileInfo.getFileName());
        redisComponent.saveDownloadCode(code,fileDto);
        
        return getSuccessResponseVO(code);
    }

    /**
     * 下载文件
     * @param request
     * @param response
     * @param code
     * @throws Exception
     */
    protected void download(HttpServletRequest request,HttpServletResponse response,String code) throws Exception{
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if(downloadFileDto == null){
            return;
        }
        
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if(request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0){  //IE浏览器
            fileName = URLEncoder.encode(fileName,"UTF-8");
        }else{
            fileName = new String(fileName.getBytes("UTF-8"),"ISO8859-1");
        }
        response.setHeader("Content-Disposition","attachment;filename=\""+fileName+"\"");
        readFile(response,filePath);
    }
    
    
    
}
