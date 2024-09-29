package com.easypan.utils;

import com.easypan.entity.constants.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class StringTools {
    
    /**
     * 生成随机数
     * @param count
     * @return
     */
    public static final String getRandomNumber(Integer count){
        return RandomStringUtils.random(count,false,true);
    }
    
    public static final String getRandomString(Integer count){
        return RandomStringUtils.random(count,true,true);
    }
    
    //测试
    public static void main(String[] args) {
//        System.out.println(getRandomNumber(5));
        String fileName = "wjc.jpg";
        System.out.println(getFileNameNoSuffix(fileName));
        System.out.println(getFileSuffix(fileName));
    }
    
    /**
     * 检验字符串是否为空
     * @param str
     * @return
     */
    public static boolean isEmpty(String str){
        if(str == null || "".equals(str) || "null".equals(str) || "\u0000".equals(str)){
            return true;
        }else if("".equals(str.trim())){
            return true;
        }
        return false;
    }
    
    /**
     * md5加密
     * @param originalString
     * @return
     */
    public static String encodeByMd5(String originalString){
        return isEmpty(originalString) ? null : DigestUtils.md5Hex(originalString);
    }
    
    
    public static boolean pathIsOk(String path){
        if(StringTools.isEmpty(path)){
            return true;
        }
        if(path.contains("../") || path.contains("..\\")){
            return false;
        }
        return true;
    }
    
    /**
     * 文件重命名 （为 用户上传相同文件名文件时，自动改名）
     * @param fileName
     * @return
     */
    public static String rename(String fileName){
        String fileNameReal = getFileNameNoSuffix(fileName);
        String suffix = getFileSuffix(fileName);
        return fileNameReal + "_" + getRandomString(Constants.LENGTH_5) + suffix;
    }
    
    /**
     * 拿到文件名
     * @param fileName
     * @return
     */
    public static String getFileNameNoSuffix(String fileName){
        Integer index = fileName.lastIndexOf(".");
        if(index == -1){
            return fileName;
        }
        
        fileName = fileName.substring(0,index);
        return fileName;
    }
    
    /**
     * 拿到文件后缀（带.）
     * @param fileName
     * @return
     */
    public static String getFileSuffix(String fileName){
        Integer index = fileName.lastIndexOf(".");
        if(index == -1){
            return "";
        }
        String suffix = fileName.substring(index);
        return suffix;
    }
    
}
