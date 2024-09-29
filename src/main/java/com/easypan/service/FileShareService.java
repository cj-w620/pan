package com.easypan.service;

import java.io.Serializable;
import java.util.List;

import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.query.FileShareQuery;
import com.easypan.entity.vo.PaginationResultVO;
/**
 * @Description:FileShareService
 * @auther:wjc
 * @date:2024/08/20
 */
public interface FileShareService {
	/**
	 * 根据条件查询列表
	 */
	List<FileShare> findListByParam(FileShareQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(FileShareQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<FileShare> findListByPage(FileShareQuery query);

	/**
	 * 新增
	 */
	Integer add(FileShare bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<FileShare> listBean);

	/**
	 * 批量新增或修改
	 */
	Integer addOrUpdateBatch(List<FileShare> listBean);

	/**
	 * 根据ShareId查询
	 */
	FileShare getFileShareByShareId(String shareId);

	/**
	 * 根据ShareId更新
	 */
	 Integer updateFileShareByShareId(FileShare bean,String shareId);

	/**
	 * 根据ShareId删除
	 */
	 Integer deleteFileShareByShareId(String shareId);

	/**
	 * 分享文件
	 * @param fileShare
	 */
	void saveShare(FileShare fileShare);

	/**
	 * （批量）取消文件分享
	 */
	void deleteFileShareBatch(String[] shareIdArray,String userId);

	/**
	 * 校验提取码
	 */
	SessionShareDto checkShareCode(String shareId,String code);
}