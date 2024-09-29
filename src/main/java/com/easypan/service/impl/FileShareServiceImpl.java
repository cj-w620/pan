package com.easypan.service.impl;

import java.util.Date;
import java.util.List;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.enums.ResponseCodeEnum;
import com.easypan.enums.ShareValidTypeEnums;
import com.easypan.exception.BusinessException;
import com.easypan.utils.DateUtils;
import com.easypan.utils.StringTools;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import com.easypan.entity.query.SimplePage;
import com.easypan.enums.PageSize;
import com.easypan.service.FileShareService;

import com.easypan.entity.po.FileShare;
import com.easypan.mappers.FileShareMapper;
import com.easypan.entity.query.FileShareQuery;
import com.easypan.entity.vo.PaginationResultVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description:FileShareServiceImpl
 * @auther:wjc
 * @date:2024/08/20
 */
@Service("fileShareService")
public class FileShareServiceImpl implements FileShareService{
	@Resource
	private FileShareMapper<FileShare,FileShareQuery> fileShareMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<FileShare> findListByParam(FileShareQuery query) {
		return this.fileShareMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(FileShareQuery query) {
		return this.fileShareMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<FileShare> findListByPage(FileShareQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(),count,pageSize);
		query.setSimplePage(page);
		List<FileShare> list = this.findListByParam(query);
		PaginationResultVO<FileShare> result = new PaginationResultVO<>(count,page.getPageSize(),page.getPageNo(),page.getPageTotal(),list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(FileShare bean) {
		return this.fileShareMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<FileShare> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.fileShareMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或修改
	 */
	public Integer addOrUpdateBatch(List<FileShare> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.fileShareMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据ShareId查询
	 */
	public FileShare getFileShareByShareId(String shareId) {
		return this.fileShareMapper.selectByShareId(shareId);
	}

	/**
	 * 根据ShareId更新
	 */
	public Integer updateFileShareByShareId(FileShare bean,String shareId) {
		return this.fileShareMapper.updateByShareId(bean,shareId);
	}

	/**
	 * 根据ShareId删除
	 */
	public Integer deleteFileShareByShareId(String shareId) {
		return this.fileShareMapper.deleteByShareId(shareId);
	}

	/**
	 * 分享文件
	 * @param share
	 */
	@Override
	public void saveShare(FileShare share) {
		ShareValidTypeEnums typeEnums = ShareValidTypeEnums.getByType(share.getValidType());
		//给的有效期参数错误：不走界面来的，out!
		if(typeEnums == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		//不是永久有效的，设置过期时间
		if (ShareValidTypeEnums.FOREVER != typeEnums){
			share.setExpireTime(DateUtils.getAfterDate(typeEnums.getDays()));
		}
		
		Date curDate = new Date();
		share.setShareTime(curDate);
		
		share.setShowCount(0);
		
		//用户未自定义提取码，系统生成
		if(StringTools.isEmpty(share.getCode())){
			share.setCode(StringTools.getRandomString(Constants.LENGTH_5));
		}
		
		share.setShareId(StringTools.getRandomString(Constants.LENGTH_20));
		this.fileShareMapper.insert(share);
	}

	/**
	 * （批量）取消文件分享
	 * @param shareIdArray	取消分享的文件的id数组
	 * @param userId	用户id
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteFileShareBatch(String[] shareIdArray, String userId) {
		Integer count = this.fileShareMapper.deleteFileShareBatch(shareIdArray,userId);
		if(count != shareIdArray.length){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
	}

	/**
	 * 校验提取码
	 * @param shareId
	 * @param code
	 * @return
	 */
	@Override
	public SessionShareDto checkShareCode(String shareId, String code) {
		FileShare share = this.fileShareMapper.selectByShareId(shareId);
		if(share == null || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))){
			throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
		}
		
		if(!share.getCode().equals(code)){
			throw new BusinessException("提取码错误");
		}
		
		//更新浏览次数
		/*
		* 如果改变写法，先查count，再+1，再去数据库里更新，这样在并发情况下是容易出错的。
		* 因为当两个用户几乎同时点击，调用了接口，查到的count都是0，但一个用户调用结束后，数据变成了1，这时第二个用户他查询的数据还是0，因为他先于这个用户
		* 更新前拿到数据，这样第二个用户调用完后，count还是1，这就是不对的了。
		* 所以，我们用“乐观锁”（？），在数据库层面修改数量，这样在高并发情况下，也不会有错。
		* */
		this.fileShareMapper.updateShareShowCount(shareId);
		
		//封装SessionShareDto并返回
		SessionShareDto sessionShareDto = new SessionShareDto();
		sessionShareDto.setShareId(shareId);
		sessionShareDto.setShareUserId(share.getUserId());
		sessionShareDto.setFileId(share.getFileId());
		sessionShareDto.setExpireTime(share.getExpireTime());
		
		return sessionShareDto;
	}
}