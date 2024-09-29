package com.easypan.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:分享信息Mapper
 * @auther:wjc
 * @date:2024/08/20
 */
public interface FileShareMapper<T, P> extends BaseMapper {
	/**
	 * 根据ShareId查询
	 */
	 T selectByShareId(@Param("shareId") String shareId);

	/**
	 * 根据ShareId更新
	 */
	 Integer updateByShareId(@Param("bean") T t, @Param("shareId") String shareId);

	/**
	 * 根据ShareId删除
	 */
	 Integer deleteByShareId(@Param("shareId") String shareId);

	/**
	 * （批量）取消文件分享
	 */
	 Integer deleteFileShareBatch(@Param("shareIdArray") String[] shareIdArray,@Param("userId") String userId);

	/**
	 * 更新浏览次数
	 */
	 void updateShareShowCount(@Param("shareId") String shareId);
}