package com.minzheng.blog.strategy.impl;

import com.minzheng.blog.enums.FileExtEnum;
import com.minzheng.blog.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Objects;

/**
 * 本地上传策略
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service("remoteServerUploadStrategyImpl")
public class RemoteServerUploadStrategyImpl extends AbstractUploadStrategyImpl {

    /**
     * 本地路径
     */
    @Value("${upload.remote_server.path}")
    private String remoteServerPath;

    /**
     * 访问url
     */
    @Value("${upload.remote_server.url}")
    private String remoteServerUrl;

    @Override
    public Boolean exists(String filePath) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(remoteServerUrl + filePath);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("HEAD");
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void upload(String path, String fileName, InputStream inputStream) throws IOException {
        // 判断目录是否存在
        if (!exists(remoteServerPath + path)) {
            throw new IOException(remoteServerPath + path + "is not exist");
        }
        File directory = new File(remoteServerUrl + "://" + remoteServerPath + path);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new BizException("创建目录失败");
            }
        }
        // 写入文件
        File file = new File(remoteServerPath + path + fileName);
        String ext = "." + fileName.split("\\.")[1];
        switch (Objects.requireNonNull(FileExtEnum.getFileExt(ext))) {
            case MD:
            case TXT:
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                while (reader.ready()) {
                    writer.write((char) reader.read());
                }
                writer.flush();
                writer.close();
                reader.close();
                break;
            default:
                BufferedInputStream bis = new BufferedInputStream(inputStream);
                BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
                byte[] bytes = new byte[1024];
                int length;
                while ((length = bis.read(bytes)) != -1) {
                    bos.write(bytes, 0, length);
                }
                bos.flush();
                bos.close();
                bis.close();
                break;
        }
        inputStream.close();
    }


    @Override
    public String getFileAccessUrl(String filePath) {
        return remoteServerUrl + filePath;
    }

}
