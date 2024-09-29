package com.easypan.task;

import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.service.FileInfoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileCleanTask {
    @Resource
    private FileInfoService fileInfoService;
        
    @Scheduled(fixedDelay = 1000 * 60 * 3)  //3min
    public void execute(){
        //查询所有过期文件
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        fileInfoQuery.setQueryExpire(true);
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
        //转为map，以userId分类，key：userId      value：该userId下的所有文件
        Map<String,List<FileInfo>> fileInfoMap = fileInfoList.stream().collect(Collectors.groupingBy(FileInfo::getUserId));
        //遍历map
        for(Map.Entry<String,List<FileInfo>> entry : fileInfoMap.entrySet()){
            //拿到文件id，转为list
            List<String> fileIds = entry.getValue().stream().map(p -> p.getFileId()).collect(Collectors.toList());
            //批量删除
            fileInfoService.delFileBatch(entry.getKey(),String.join(",",fileIds),false);
        }
        
    }
}
