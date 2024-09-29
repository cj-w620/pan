package com.easypan.service;

import java.io.Serializable;
import java.util.List;

import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description:FileInfoService
 * @auther:wjc
 * @date:2024/07/07
 */
public interface FileInfoService {
	/**
	 * 根据条件查询列表
	 */
	List<FileInfo> findListByParam(FileInfoQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(FileInfoQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<FileInfo> findListByPage(FileInfoQuery query);

	/**
	 * 新增
	 */
	Integer add(FileInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<FileInfo> listBean);

	/**
	 * 批量新增或修改
	 */
	Integer addOrUpdateBatch(List<FileInfo> listBean);

	/**
	 * 根据FileIdAndUserId查询
	 */
	FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);

	/**
	 * 根据FileIdAndUserId更新
	 */
	 Integer updateFileInfoByFileIdAndUserId(FileInfo bean,String fileId, String userId);

	/**
	 * 根据FileIdAndUserId删除
	 */
	 Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId);
	
	/**
	 *	文件分片上传
	 */
	UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file,
							   String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);
	
	/**
	 * 新建目录
	 */
	//目录也是FileInfo对象
	FileInfo newFolder(String filePid,String userId,String folderName);
	
	/**
	 *	文件重命名
	 */
	FileInfo rename(String fileId,String userId,String fileName);
	
	/**
	 * 批量移动文件
	 */
	void changeFileFolder(String fileIds,String filePid,String userId);

	/**
	 * （批量）删除文件 （移至回收站）
	 */
	void removeFile2RecycleBatch(String userId,String fileIds);

	/**
	 * （批量）恢复回收站文件
	 */
	void recoverFileBatch(String userId,String fileIds);

	/**
	 * （批量）彻底删除文件
	 */
	void delFileBatch(String userId,String fileIds,Boolean adminOp);

	/**
	 *	
	 */
	void checkRootFilePid(String rootFilePid,String userId,String fileId);

	/**
	 *	保存文件到网盘
	 */
	void saveShare(String shareRootFilePid,String shareFileIds,String myFolderId,String shareUserId,String currentUserId);
}