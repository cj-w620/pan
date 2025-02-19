package com.easypan.mappers;

import com.easypan.entity.po.FileInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description:文件信息Mapper
 * @auther:wjc
 * @date:2024/07/07
 */
public interface FileInfoMapper<T, P> extends BaseMapper {
	/**
	 * 根据FileIdAndUserId查询
	 */
	 T selectByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

	/**
	 * 根据FileIdAndUserId更新
	 */
	 Integer updateByFileIdAndUserId(@Param("bean") T t, @Param("fileId") String fileId, @Param("userId") String userId);

	/**
	 * 根据FileIdAndUserId删除
	 */
	 Integer deleteByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);
	
	
	/**
	 *	
	 */
	 Long selectUseSpace(@Param("userId") String userId);
	 
	 
	 void updateFileStatusWithOldStatus(@Param("fileId") String fileId, @Param("userId") String userId,@Param("bean") T t,@Param("oldStatus") Integer oldStatus);

	/**
	 * （批量）更新文件删除标记
	 */
	void updateFileDelFlagBatch(@Param("bean")FileInfo fileInfo, @Param("userId")String userId, @Param("filePidList")List<String> filePidList,
								@Param("fileIdList")List<String> fileIdList,@Param("oldDelFlag") Integer oldDelFlag);


	/**
	 * （批量）彻底删除文件
	 */
	void delFileBatch( @Param("userId")String userId, @Param("filePidList")List<String> filePidList,
								@Param("fileIdList")List<String> fileIdList,@Param("oldDelFlag") Integer oldDelFlag);

	/**
	 * 根据用户id删除文件
	 */
	void deleteFileByUserId(@Param("userId") String userId);
}