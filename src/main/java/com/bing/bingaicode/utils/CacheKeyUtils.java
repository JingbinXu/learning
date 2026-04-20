package com.bing.bingaicode.utils;

import cn.hutool.json.JSONUtil;
import org.apache.commons.codec.digest.DigestUtils;

public class CacheKeyUtils {

    /**
     * 根据对象生成缓存key (JSON + MD5)
     *
     * @param obj 要生成key的对象
     * @return MD5哈希后的缓存key
     */
    public static String generateKey(Object obj) {
        if(obj == null){
            return DigestUtils.md5Hex("null");
        }
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtils.md5Hex(jsonStr);
    }
}
