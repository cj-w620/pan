package com.easypan.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.enums.*;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.utils.DateUtils;
import com.easypan.utils.ProcessUtils;
import com.easypan.utils.ScaleFilter;
import com.easypan.utils.StringTools;
import org.apache.commons.io.FileUtils;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import com.easypan.entity.query.SimplePage;
import com.easypan.service.FileInfoService;

import com.easypan.entity.po.FileInfo;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.PaginationResultVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 */
@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {

    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private AppConfig appConfig;

    @Resource
    @Lazy //防止循环依赖
    private FileInfoServiceImpl fileInfoService;

    /**
     * 根据条件查询列表
     */
    public List<FileInfo> findListByParam(FileInfoQuery query) {
        return this.fileInfoMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(FileInfoQuery query) {
        return this.fileInfoMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<FileInfo> list = this.findListByParam(query);
        PaginationResultVO<FileInfo> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(FileInfo bean) {
        return this.fileInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    public Integer addOrUpdateBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据FileIdAndUserId查询
     */
    public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
    }

    /**
     * 根据FileIdAndUserId更新
     */
    public Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId) {
        return this.fileInfoMapper.updateByFileIdAndUserId(bean, fileId, userId);
    }

    /**
     * 根据FileIdAndUserId删除
     */
    public Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.deleteByFileIdAndUserId(fileId, userId);
    }

    /**
     * 文件分片上传
     *
     * @param webUserDto 用户信息
     * @param fileId     文件id
     * @param file       文件
     * @param fileName   文件名
     * @param filePid    文件父级目录
     * @param fileMd5    文件md5值，由前端传来
     * @param chunkIndex 分片顺序
     * @param chunks     总分片数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        UploadResultDto resultDto = new UploadResultDto();
        Boolean uploadSuccess = true;
        File tempFileFolder = null;
        try {
            if (StringTools.isEmpty(fileId)) {
                fileId = StringTools.getRandomString(Constants.LENGTH_10);
            }
            resultDto.setFileId(fileId);
            Date curDate = new Date();
            UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
            //若当前是第一分片，需要判断md5值，如果数据库中存在，证明数据库里有这个文件，直接在数据库里copy一份，实现秒传
            //若没有，则需要上传，设置md5值。
            if (chunkIndex == 0) {
                FileInfoQuery infoQuery = new FileInfoQuery();
                infoQuery.setFileMd5(fileMd5);
                infoQuery.setSimplePage(new SimplePage(0, 1));    //查一条
                infoQuery.setStatus(FileStatusEnums.USING.getStatus());
                //查询数据库里是否有这个文件
                List<FileInfo> dbFileList = this.fileInfoMapper.selectList(infoQuery);
                //如果存在，不用再次上传，实现秒传，直接在数据库里copy一份给这个用户
                //秒传
                if (!dbFileList.isEmpty()) {
                    FileInfo dbFile = dbFileList.get(0);
                    //判断文件大小，若超过，不给传
                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    dbFile.setFileId(fileId);
                    dbFile.setFilePid(filePid);
                    dbFile.setUserId(webUserDto.getUserId());
                    dbFile.setCreateTime(curDate);
                    dbFile.setLastUpdateTime(curDate);
                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
                    dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
                    //文件名
                    fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
                    dbFile.setFileName(fileName);
                    this.fileInfoMapper.insert(dbFile);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
                    //更新用户使用空间
                    updateUserSpace(webUserDto, dbFile.getFileSize());
                    return resultDto;
                }
            }
            //判断磁盘空间
            Long currentTempSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            if (file.getSize() + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }
            //暂存临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }
            File newFile = new File(tempFileFolder + "/" + chunkIndex);
            file.transferTo(newFile);
            //保存临时大小
            redisComponent.saveFileTempSize(webUserDto.getUserId(), fileId, file.getSize());
            if (chunkIndex < chunks - 1) {  //不是最后分片
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
               
                return resultDto;
            }
           
            //最后一个分片上传完成，记录数据库，异步合并分片
            String month = DateUtils.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            //真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            //自动重命名
            fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
            //入数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(webUserDto.getUserId());
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnums.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnums.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.insert(fileInfo);
            //该文件总空间
            Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            updateUserSpace(webUserDto, totalSize);
            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            //事务提交后，再调合并方法。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(), webUserDto);
                }
            });
            return resultDto;
        } catch (BusinessException e) {
            logger.error("文件上传失败", e);
            uploadSuccess = false;
            throw e;
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            uploadSuccess = false;
        } finally {
            //如果有异常，删除当前临时目录
            if (!uploadSuccess) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    logger.error("删除临时目录失败", e);
                }
            }
        }
        return resultDto;
    }


    /**
     * 上传文件时，若已存在相同文件名，当前文件重命名
     */
    private String autoRename(String filePid, String userId, String fileName) {
        //文件名已存在，重命名
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setFileName(fileName);
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {    //已存在
            fileName = StringTools.rename(fileName);
        }
        //不存在，就不会改，原来的直接返回
        return fileName;
    }


    /**
     * 更新用户空间（使用空间）
     */
    private void updateUserSpace(SessionWebUserDto webUserDto, Long useSpace) {
        Integer count = userInfoMapper.updateUserSpace(webUserDto.getUserId(), useSpace, null);
        if (count == 0) {    //不满足where条件，即使用空间超过总空间，更新失败，返回0，也就是没更新。
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        //redis中也更新一下
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace() + useSpace);
        redisComponent.saveUserSpaceUse(webUserDto.getUserId(), spaceDto);
    }

    @Async  //异步
    public void transferFile(String fileId, SessionWebUserDto webUserDto) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnum = null;
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, webUserDto.getUserId());
        try {
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {     //文件不存在 或者 文件不是转码中的状态 直接out
                return;
            }
            //临时目录
            //D:/project/project_log/easypan//temp/
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            //[userId][fileId]
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            //D:/project/project_log/easypan//temp/[userId][fileId]
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            //.jpg
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());  //文件后缀
            //202409
            String month = DateUtils.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());  //文件月份
            
            //目标目录
            //D:/project/project_log/easypan//file/
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            //D:/project/project_log/easypan//file/202409
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            //真实文件名
            //[userId][fileId].jpg
            String realFileName = currentUserFolderName + fileSuffix;
            //D:/project/project_log/easypan//file/202409[userId][fileId].jpg
            targetFilePath = targetFolder.getPath() + "/" + realFileName; //目标文件位置
            //合并文件
            //fileFolder.getPath()：D:/project/project_log/easypan//temp/[userId][fileId]
            //targetFilePath：D:/project/project_log/easypan//file/202409/[userId][fileId].jpg
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);
            //视频文件切割
            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnum) {
                cutFile4Video(fileId, targetFilePath);
                //视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            } else if (FileTypeEnums.IMAGE == fileTypeEnum) {
                //生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                //D:/project/project_log/easypan//file//202409/[userId][fileId]_.jpg
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {   //如果图片大小太小，不会生成缩略图。那我们就copy一份原图。
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }
            }
        } catch (Exception e) {
            logger.error("文件转码失败，文件ID：{}，userId：{}", fileId, webUserDto.getUserId(), e);
            transferSuccess = false;
            throw new BusinessException("文件转码失败");
        } finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFileSize(new File(targetFilePath).length());
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(fileId, webUserDto.getUserId(), updateInfo, FileStatusEnums.TRANSFER.getStatus());
        }
    }

    /**
     * 合并分片
     *
     * @param dirPath    分片存放目录
     * @param toFilePath 目标目录
     * @param fileName   文件名
     * @param delSource  是否删除源文件
     */
    private void union(String dirPath, String toFilePath, String fileName, Boolean delSource) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("分片存放目录不存在");
        }
        File[] fileList = dir.listFiles();  //当前目录下的所有文件
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                File chunkFile = new File(dirPath + "/" + i);   //分片文件
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    logger.error("合并分片失败", e);
                    throw new BusinessException("合并分片失败");
                } finally {
                    readFile.close();
                }

            }
        } catch (Exception e) {
            logger.error("合并文件：{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            if (writeFile != null) {
                try {
                    writeFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (delSource && dir.exists()) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        //cmd命令
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
        //执行cmd命令
        //生成.ts
        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //生成索引文件.m3u8和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        //删除index.ts
        new File(tsPath).delete();
    }

    /**
     * 新建目录
     */
    @Override
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        //校验文件名：同个用户的，同级目录下，不能有相同文件名
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());
        //新建
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(folderName);
        fileInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        this.fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    /**
     * 检查文件名是否已存在，存在抛异常
     *
     * @param filePid    文件父级目录
     * @param userId     用户id
     * @param fileName   文件名
     * @param folderType 目录类型：文件 目录
     */
    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFolderType(folderType);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            throw new BusinessException("此目录下已经存在同名文件，请修改名称");
        }
    }

    /**
     * 文件重命名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        //首先检查文件是否存在
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        
        //检查要修改的文件名是否已存在
        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());
        
        //如果是文件，拼接文件后缀
        /*前端只允许修改文件名称，后缀无法修改。前端传来的fileName，只是名字，不带后缀，所以我们拼接上*/
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFileType())) {
            fileName = fileName + StringTools.getFileSuffix(fileName);
        }
        
        //修改入库
        Date curDate = new Date();
        FileInfo updateInfo = new FileInfo();
        updateInfo.setFileName(fileName);
        updateInfo.setLastUpdateTime(curDate);
        this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, fileId, userId);
       
        //修改后再次查询，检查是否有重复的文件名
        /*防止有多人同时上传，造成名字重复。加上事务，如果查询到了重复的，直接回滚*/
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserIdFuzzy(userId);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 1) {
            throw new BusinessException("文件名" + fileName + "已存在");
        }
        
        fileInfo.setFileName(fileName);
        return fileInfo;
    }

    /**
     * 批量移动文件
     * @param fileIds    移动的文件的id
     * @param filePid    移动目的地目录id
     * @param userId     用户id
     */
    @Override
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        //已经在该目录下，还要移动到这个目录，out
        if(fileIds.equals(filePid)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        /*filePid为0：在根目录下*/
        if(!Constants.ZERO_STR.equals(filePid)){    //非移动到根目录
            //检查是否合法路径
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(filePid,userId);
            if(fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())){
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        
        String[] fileIdArray = fileIds.split(",");
        
        //如果移动的文件与移动目的地目录下的文件有重名的，修改
        //查找移动目录下的所有文件
        FileInfoQuery query = new FileInfoQuery();
        query.setFilePid(filePid);
        query.setUserId(userId);
       
        /*
        等同于List<FileInfo> dbFileList = fileInfoService.findListByParam(query);
        用this调用的，没有交给Spring管理。fileInfoService就会交给Spring管理
        */
        List<FileInfo> dbFileList = this.findListByParam(query);
        
        Map<String,FileInfo> dbFileNameMap = dbFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(),(data1,data2) -> data2));
        
        //查询选中的文件
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        List<FileInfo> selectFileList = this.findListByParam(query);
        
        //将所选文件重命名
        for(FileInfo item : selectFileList){
            FileInfo rootFileInfo = dbFileNameMap.get(item.getFileName());
            //文件名已存在，重命名被还原的文件名
            FileInfo updateInfo = new FileInfo();
            if(rootFileInfo != null){
                String fileName = StringTools.rename(item.getFileName());
                updateInfo.setFileName(fileName);
            }
            updateInfo.setFilePid(filePid);
            this.fileInfoMapper.updateByFileIdAndUserId(updateInfo,item.getFileId(),userId);
        }
        
    }

    /**
     * （批量）删除文件到回收站
     * @param userId
     * @param fileIds
     */
    @Override
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        //查询要删除的文件
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        
        if(fileInfoList == null){   //校验要删除的文件是否存在
            return;
        }
        
        //查找要删除的文件，下的所有使用中的目录，递归查找
        List<String> delFilePidList = new ArrayList<>();
        for(FileInfo fileInfo : fileInfoList){
            findAllSubFolderFileList(delFilePidList,userId,fileInfo.getFileId(),FileDelFlagEnums.USING.getFlag());
        }
        
        //删除所选文件下的所有文件及目录
        //用父id删除，所以查找的是目录就可以了
        if(!delFilePidList.isEmpty()){
            FileInfo updateInfo = new FileInfo();
            //删除所选目录，下面的目录及文件都设置成删除状态，在回收站内不可见
            updateInfo.setDelFlag(FileDelFlagEnums.DEL.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(updateInfo,userId,delFilePidList,null,FileDelFlagEnums.USING.getFlag());
        }
   
        //删除所选文件及目录
        List<String> delFileList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setRecoveryTime(new Date());
        fileInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo,userId,null,delFileList,FileDelFlagEnums.USING.getFlag());
    }

    /**
     * 查找 指定用户、指定父目录、指定文件状态的目录 并将id存放至fileIdList
     * @param fileIdList    目标文件id集合
     * @param userId    用户id
     * @param fileId    父文件id，查找的是该文件下的符合条件的目录
     * @param delFlag   文件状态
     */
    private void findAllSubFolderFileList(List<String> fileIdList,String userId,String fileId,Integer delFlag){
        fileIdList.add(fileId);
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFilePid(fileId);
        query.setDelFlag(delFlag);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        /*fileInfoList为null的情况，遍历操作是否会报错？  不会，直接过去*/
        for(FileInfo fileInfo : fileInfoList){
            findAllSubFolderFileList(fileIdList,userId, fileInfo.getFileId(), delFlag);
        }
    }

    /**
     * （批量）恢复回收站文件
     * @param userId    用户id
     * @param fileIds   勾选文件id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        //回收站中要恢复的文件
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        
        //遍历选中文件，如果是目录，查找其下所有子目录，将id放入delFIleSubFolderFileIdList
        List<String> delFIleSubFolderFileIdList = new ArrayList<>();
        for(FileInfo fileInfo : fileInfoList){
            if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
                findAllSubFolderFileList(delFIleSubFolderFileIdList,userId,fileInfo.getFileId(),FileDelFlagEnums.DEL.getFlag());
            }
        }
        
        //查询所有根目录的文件
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setFilePid(Constants.ZERO_STR);
        List<FileInfo> allRootFileList = this.findListByParam(query);
        
        Map<String,FileInfo> rootFileMap = allRootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName,Function.identity(),(data1,data2) -> data2));
        
        //查询所选文件 将目录下的所有删除的文件更新为使用中
        if(!delFIleSubFolderFileIdList.isEmpty()){  //选中恢复的文件中有目录
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(fileInfo,userId,delFIleSubFolderFileIdList,null,FileDelFlagEnums.DEL.getFlag());
        }
        
        //将选中的文件更新为使用中，且父级目录到根目录
        List<String> recoverFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfo.setFilePid(Constants.ZERO_STR);
        fileInfo.setLastUpdateTime(new Date());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo,userId,null,recoverFileIdList,FileDelFlagEnums.RECYCLE.getFlag());
        
        //所选文件若跟根目录中文件重名，将所选文件重命名
        for(FileInfo item : fileInfoList){
            FileInfo rootFileInfo = rootFileMap.get(item.getFileName());
            //文件名已存在，重命名
            if(rootFileInfo != null){
                String fileName = StringTools.rename(item.getFileName());
                FileInfo updateFileInfo = new FileInfo();
                updateFileInfo.setFileName(fileName);
                this.fileInfoMapper.updateByFileIdAndUserId(updateFileInfo,item.getFileId(),userId);
            }
        }
    }

    /**
     * （批量）彻底删除文件
     * @param userId
     * @param fileIds
     * @param adminOp
     */
    @Override
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        //勾选删除的文件
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        
        List<String> delFileSubFileFolderFileList = new ArrayList<>();
        //所选文件中有目录，要将其下的所有子目录及文件一并删除
        //找到所选目录下的所有子目录id，将所有子目录id和当前目录id放入delFileSubFileFolderFileList
        for(FileInfo fileInfo : fileInfoList){
            if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
                findAllSubFolderFileList(delFileSubFileFolderFileList,userId,fileInfo.getFileId(),FileDelFlagEnums.DEL.getFlag());  
            }
        }
        
        //删除所选目录及其下所有文件和目录
        if(!delFileSubFileFolderFileList.isEmpty()){
            this.fileInfoMapper.delFileBatch(userId,delFileSubFileFolderFileList,null,adminOp ? null : FileDelFlagEnums.DEL.getFlag());
        }
        
        //删除所选文件
        this.fileInfoMapper.delFileBatch(userId,null,Arrays.asList(fileIdArray),adminOp ? null : FileDelFlagEnums.RECYCLE.getFlag());
        
        //数据库更新用户使用空间
        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUseSpace(useSpace);
        this.userInfoMapper.updateByUserId(userInfo,userId);
        
        //缓存更新用户使用空间
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(userId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId,userSpaceDto);
    }

    /**
     * 
     * @param rootFilePid
     * @param userId
     * @param fileId
     */
    @Override
    public void checkRootFilePid(String rootFilePid, String userId, String fileId) {
        if(StringTools.isEmpty(fileId)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        
        if(rootFilePid.equals(fileId)){
            return;
        }
        checkFilePid(rootFilePid, fileId,userId);
    }
    
    private void checkFilePid(String rootFilePid, String fileId, String userId){
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId,userId);
        if(fileInfo == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(Constants.ZERO_STR.equals(fileInfo.getFilePid())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        
        if(fileInfo.getFilePid().equals(rootFilePid)){
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFilePid(),userId);
    }

    /**
     * 保存文件到网盘
     * @param shareRootFilePid
     * @param shareFileIds
     * @param myFolderId
     * @param shareUserId
     * @param currentUserId
     */
    @Override
    public void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String currentUserId) {
        //要保存的所有文件
        String[] shareFileIdArray = shareFileIds.split(",");
        
        //查询要保存到的目录下的所有文件 与 要保存进来的文件 对比 看看有没有重名的 
        //如果有，要重命名
        //目标文件列表（要保存到的 当前用户的 目录下的所有文件）
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(currentUserId);
        fileInfoQuery.setFilePid(myFolderId);
        List<FileInfo> currentFileList = this.fileInfoMapper.selectList(fileInfoQuery);
        /*
        * 使用 Java Streams 将 currentFileList 列表中的 FileInfo 对象转换为一个 Map。这个 Map 的键是文件名，值是对应的 FileInfo 对象。
        * currentFileList.stream(): 从 currentFileList 列表中创建一个流。
        * .collect(Collectors.toMap(...)): 将流中的元素收集到一个 Map 中。
        * FileInfo::getFileName: 这是一个方法引用，用来从 FileInfo 对象中提取键（即文件名）。
        * Function.identity(): 这是一个返回流中每个元素本身的函数，用作值提取函数。
        * (data1, data2) -> data2: 这是一个合并函数，用来处理键重复的情况。在这个例子中，如果出现了相同的文件名，data2（后出现的 FileInfo 对象）会覆盖 data1（先出现的 FileInfo 对象）。
        * */
        Map<String,FileInfo> currentFileMap = currentFileList.stream().collect(Collectors.toMap(FileInfo::getFileName,Function.identity(),(data1,data2) -> data2));
        
        //勾选的要保存的文件列表
        fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(shareUserId);
        fileInfoQuery.setFileIdArray(shareFileIdArray);
        List<FileInfo> shareFileList = this.fileInfoMapper.selectList(fileInfoQuery);
        
        //如果有重名的，要进行重命名
        //copyFileList就是经过查重文件名，若有重名文件已重命名的，所有要保存的文件
        List<FileInfo> copyFileList = new ArrayList<>();
        Date curDate = new Date();
        for(FileInfo item : shareFileList){
            //遍历要保存的文件，在自己的文件列表下看看有没有重名文件
            FileInfo haveFile = currentFileMap.get(item.getFileName());
            if(haveFile != null){   //有重名文件
                item.setFileName(StringTools.rename(item.getFileName()));   //重命名要保存的重名文件
            }
            //将当前文件放到copyFileList中，并且，如果当前文件是目录，将其下所有文件及目录也一并放入copyFileList
            findAllSubFile(copyFileList,item,shareUserId,currentUserId,curDate,myFolderId);
        }
        //插入到数据库
        this.fileInfoMapper.insertBatch(copyFileList);
    }

    /**
     * 保存当前文件，如果当前文件是目录，一并保存其下所有文件及目录
     * @param copyFileList  要保存的文件列表
     * @param fileInfo  要保存的当前文件
     * @param sourceUserId  分享该文件的用户id
     * @param currentUserId 当前要保存文件的用户id
     * @param curDate   当前时间
     * @param newFilePid    要保存到的目录的id
     */
    private void findAllSubFile(List<FileInfo> copyFileList,FileInfo fileInfo,String sourceUserId,String currentUserId,Date curDate,String newFilePid){
        //当前文件id
        String sourceFileId = fileInfo.getFileId();
        //保存当前文件到copyFileList
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setFilePid(newFilePid);
        fileInfo.setUserId(currentUserId);
        String newFileId = StringTools.getRandomString(Constants.LENGTH_10);
        fileInfo.setFileId(newFileId);
        copyFileList.add(fileInfo);
        //如果当前文件是目录，将其下的目录和文件一并保存
        if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
            //用分享人id 和 当前文件id作为父目录的id 查询其下所有文件及目录 （即查询我们要保存的目录下的文件及目录）
            FileInfoQuery query = new FileInfoQuery();
            query.setFilePid(sourceFileId);
            query.setUserId(sourceUserId);
            List<FileInfo> sourceFileList = this.fileInfoMapper.selectList(query);
            //遍历，递归
            for(FileInfo item : sourceFileList){
                findAllSubFile(copyFileList,item,sourceUserId,currentUserId,curDate,newFileId);
            }
        }
    }
}