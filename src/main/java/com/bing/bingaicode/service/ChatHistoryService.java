package com.bing.bingaicode.service;

import com.bing.bingaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.bing.bingaicode.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.bing.bingaicode.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author bing
 */
public interface ChatHistoryService extends IService<ChatHistory> {


    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    //加载对话记录到内存
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    //根据应用id删除对话记录
    boolean deleteByAppId(Long appId);


    //构造查询条件
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);
}
