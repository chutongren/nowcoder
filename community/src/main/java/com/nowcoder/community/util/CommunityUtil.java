package com.nowcoder.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;

public class CommunityUtil {
    //生成激活码（随机字符串） 上传文件生成随机文件名
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-", ""); //只要字母和数字就够了
    }
    //MD5加密 对pw加密 1只能加密不能解密 2每次加密都是变成一个值
    // hello + 3e4a8（salt） -> abc123def456abc
    public static String md5(String key){
        if(StringUtils.isBlank(key)){
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

}
