package com.sk.weichat.util;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by dty on 2015/11/8.
 */
public class downloadTask extends Thread {
    private int blockSize, downloadSizeMore;
    private int threadNum = 5;
    String urlStr, threadNo, fileName;
    private int downloadedSize = 0;
    private int fileSize = 0;
    public downloadTask(String urlStr, int threadNum, String fileName) {
        this.urlStr = urlStr;
        this.threadNum = threadNum;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        FileDownloadThread[] fds = new FileDownloadThread[threadNum];
        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            //获取下载文件的总大小
            fileSize = conn.getContentLength();
            //计算每个线程要下载的数据量
            blockSize = fileSize / threadNum;
            // 解决整除后百分比计算误差
            downloadSizeMore = (fileSize % threadNum);
            File file = new File(fileName);
            for (int i = 0; i < threadNum; i++) {
                //启动线程，分别下载自己需要下载的部分
                FileDownloadThread fdt = new FileDownloadThread(url, file,
                        i * blockSize, (i + 1) * blockSize - 1);
                fdt.setName("Thread" + i);
                fdt.start();
                fds[i] = fdt;
            }
            boolean finished = false;
            while (!finished) {
                // 先把整除的余数搞定
                downloadedSize = downloadSizeMore;
                finished = true;
                for (int i = 0; i < fds.length; i++) {
                    downloadedSize += fds[i].getDownloadSize();
                    if (!fds[i].isFinished()) {
                        finished = false;
                    }
                }
            }
        } catch (Exception e) {

        }

    }
}