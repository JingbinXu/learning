package com.bing.bingaicode.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.bing.bingaicode.model.entity.App;
import com.bing.bingaicode.mapper.AppMapper;
import com.bing.bingaicode.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author bing
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
