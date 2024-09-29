package com.easypan.component;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.mappers.UserInfoMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;
    
    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;
    
    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    
    public SysSettingsDto getSysSettingDto(){
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if(sysSettingsDto == null){
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING,sysSettingsDto);
        }
        return sysSettingsDto;
    }
    
    public void saveSysSettingsDto(SysSettingsDto sysSettingsDto){
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING,sysSettingsDto);
    }
    
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto){
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE+userId,userSpaceDto,Constants.REDIS_KEY_EXPIRES_DAY);
    }
    
    public UserSpaceDto resetUserSpaceUse(String userId){
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
        userSpaceDto.setUseSpace(useSpace);
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId,userSpaceDto,Constants.REDIS_KEY_EXPIRES_DAY);
        return userSpaceDto;
    }
    
    public UserSpaceDto getUserSpaceUse(String userId){
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE+userId);
        if(spaceDto == null){
            spaceDto = new UserSpaceDto();
            Long useSpace = fileInfoMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(useSpace);
            //TODO 疑问点：若用户是redis中存储过期了，来拿没有，所以查询数据库数据重新创建UserSpaceDto，那useSpace和totalSpace不都应该跟数据库中的数据一致吗？把total又用初始化的给，那如果用户升级过空间，岂不是过期后，在redis中空间又变回初始的了。
            spaceDto.setTotalSpace(getSysSettingDto().getUserInitUseSpace() * Constants.MB);
            saveUserSpaceUse(userId,spaceDto);
        }
        return spaceDto;
    }
    
    public void saveFileTempSize(String userId,String fileId,Long fileSize){
        Long currentSize = getFileTempSize(userId,fileId);
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId+fileId,currentSize + fileSize,Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
    }
    
    //获取临时文件大小
    public Long getFileTempSize(String userId,String fileId){
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }
    
    private Long getFileSizeFromRedis(String key){
        Object sizeObj = redisUtils.get(key);
        if(sizeObj == null){
            return 0L;
        }
        if(sizeObj instanceof Integer){
            return ((Integer) sizeObj).longValue();
        }else if(sizeObj instanceof Long){
            return (Long) sizeObj;
        }
        return 0L;
    }
    
    //临存下载code
    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto){
        //key： easypan:download:{code}  value：downloadFileDto   time：5min
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD + code,downloadFileDto,Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }
    
    //获取下载code
    public DownloadFileDto getDownloadCode(String code){
        return (DownloadFileDto)redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
    }
    
}
