package com.bing.bingaicode.service;

import jakarta.servlet.http.HttpServletResponse;

import java.net.http.HttpRequest;

public interface ProjectDownloadService {
     /**
      * 下载项目为压缩包
      *
      * @param projectPath
      * @param downloadFileName
      * @param response
      */
     void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
