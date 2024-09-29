package com.easypan.controller;


import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.enums.FileCategoryEnum;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.enums.FileFolderTypeEnums;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.easypan.entity.po.FileInfo;
import com.easypan.service.FileInfoService;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.ResponseVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Description:FileInfoController
 * @auther:wjc
 * @date:2024/07/07
 */
@RestController
@RequestMapping("/file")
public class FileInfoController extends CommonFileController {
	@Resource
	private FileInfoService fileInfoService;

	/**
	 * 根据条件分页查询
	 */
	@RequestMapping("/loadDataList")
	@GlobalInterceptor
	public ResponseVO loadDataList(HttpSession session,FileInfoQuery query,String category) {
		//前端传来"image"，通过该code获取categoryEnum
		FileCategoryEnum categoryEnum = FileCategoryEnum.getByCode(category);
		if(categoryEnum != null){	//填充文件类别，3
			query.setFileCategory(categoryEnum.getCategory());
		}
		query.setUserId(getUserInfoFromSession(session).getUserId());
		query.setOrderBy("last_update_time desc");	//根据最后更新时间倒序排
		query.setDelFlag(FileDelFlagEnums.USING.getFlag());
		PaginationResultVO result = fileInfoService.findListByPage(query);
		
		return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
	}
	
	/*分片在前端做，不是后端做。*/
	//
	@RequestMapping("/uploadFile")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO uploadFile(HttpSession session,
								 String fileId,
								 MultipartFile file,
								 @VerifyParam(required = true) String fileName,	//一般的，可以从file里拿的，但我们做的是分片，要前端传
								 @VerifyParam(required = true) String filePid, //父级目录
								 @VerifyParam(required = true) String fileMd5, //文件md5值 要实现秒传，那么整个文件传过来再转md5是不对的，所以前端做md5，传给后端md5值
								 @VerifyParam(required = true) Integer chunkIndex, //当前分片顺序
								 @VerifyParam(required = true) Integer chunks  //总分片数
								 ) {
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
		return getSuccessResponseVO(resultDto);
	}
	
	/**
	 * 显示封面
	 * @param response
	 * @param imageFolder
	 * @param imageName
	 */
	@RequestMapping("/getImage/{imageFolder}/{imageName}")
	@GlobalInterceptor(checkParams = true)
	public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder,@PathVariable("imageName") String imageName){
		super.getImage(response,imageFolder,imageName);
	}
	
	@RequestMapping("/ts/getVideoInfo/{fileId}")
	@GlobalInterceptor(checkParams = true)
	public void getVideoInfo(HttpServletResponse response, HttpSession session,@PathVariable("fileId") String fileId){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		super.getFile(response,fileId, webUserDto.getUserId());
	}
	
	/**
	 * 获取文件信息
	 * @param response
	 * @param session
	 * @param fileId
	 */
	@RequestMapping("/getFile/{fileId}")
	@GlobalInterceptor(checkParams = true)
	public void getFile(HttpServletResponse response, HttpSession session,@PathVariable("fileId") String fileId){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		super.getFile(response,fileId, webUserDto.getUserId());
	}
	
	/**
	 * 新建目录
	 * @param session	session
	 * @param filePid	父级目录
	 * @param fileName	目录名
	 * @return
	 */
	@RequestMapping("/newFoloder")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO newFoloder(HttpSession session,
								 @VerifyParam(required = true) String filePid,
								 @VerifyParam(required = true) String fileName){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		FileInfo fileInfo = this.fileInfoService.newFolder(filePid, webUserDto.getUserId(),fileName);
		return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
	}
	
	/**
	 * 获取目录信息  （进入某文件后，显示文件导航栏）
	 * @param session
	 * @param path
	 * @return
	 */
	@RequestMapping("/getFolderInfo")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO newFoloder(HttpSession session,
								 @VerifyParam(required = true) String path){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		
		return super.getFolderInfo(path, webUserDto.getUserId());
	}
	
	/**
	 * 文件/目录重命名
	 * @param session
	 * @param fileId
	 * @param fileName
	 * @return
	 */
	@RequestMapping("/rename")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO rename(HttpSession session,
								 @VerifyParam(required = true) String fileId,
								 @VerifyParam(required = true) String fileName){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		FileInfo fileInfo = fileInfoService.rename(fileId, webUserDto.getUserId(),fileName);
		/*有些信息不能返回给前端，以防有调用接口获取信息的行为*/
		return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
	}


	/**
	 * 加载当前目录下所有文件
	 * @param session
	 * @param filePid	父目录id （该接口目的就是加载该目录下的所有文件）
	 * @param currentFileIds	
	 * @return
	 */
	@RequestMapping("/loadAllFolder")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO loadAllFolder(HttpSession session,
							 @VerifyParam(required = true) String filePid, 
									String currentFileIds){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		
		FileInfoQuery infoQuery = new FileInfoQuery();
		infoQuery.setUserId(webUserDto.getUserId());
		infoQuery.setFilePid(filePid);
		infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
		if(!StringTools.isEmpty(currentFileIds)){	//TODO ???
			infoQuery.setExcludeFileIdArray(currentFileIds.split(","));
		}
		infoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
		infoQuery.setOrderBy("create_time desc");
		List<FileInfo> fileInfoList = fileInfoService.findListByParam(infoQuery);
		return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FileInfoVO.class));
	}

	/**
	 * 批量移动文件
	 * @param session
	 * @param fileIds	移动的文件的id
	 * @param filePid	移动目的地目录id
	 * @return
	 */
	@RequestMapping("/changeFileFolder")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO changeFileFolder(HttpSession session,
							 @VerifyParam(required = true) String fileIds,
							 @VerifyParam(required = true) String filePid){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		fileInfoService.changeFileFolder(fileIds,filePid, webUserDto.getUserId());
		return getSuccessResponseVO(null);
	}

	/*
	* 下载文件分两个接口：createDownloadUrl（创建下载链接）	download（真正的下载文件）
	* 因为有些浏览器带插件，下载自动迅雷，那这时在迅雷上没有登录信息，就下载不了。
	* 为了解决这个问题，我们将下载分为两个，第一个需要校验登录，给出下载凭证。
	* 第二个接口不需要校验登录，拿着下载凭证去下载即可。
	* */
	/**
	 * 创建下载链接
	 * @param session
	 * @param fileId	文件id
	 * @return
	 */
	@RequestMapping("/createDownloadUrl/{fileId}")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO createDownloadUrl(HttpSession session,
									   @VerifyParam(required = true) @PathVariable("fileId") String fileId){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		return super.createDownloadUrl(fileId, webUserDto.getUserId());
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
	public void download(HttpServletRequest request,HttpServletResponse response,
							   @VerifyParam(required = true) @PathVariable("code") String code) throws Exception {
		super.download(request,response,code);
	}

	@RequestMapping("/delFile")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO delFile(HttpSession session,
							  @VerifyParam(required = true) String fileIds){
		SessionWebUserDto webUserDto = getUserInfoFromSession(session);
		fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
		return getSuccessResponseVO(null);
	}
}