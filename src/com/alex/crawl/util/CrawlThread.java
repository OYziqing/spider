package com.alex.crawl.util;

import com.alex.crawl.main.MyCrawl;

/**
 * @author zjx
 * @Title: ${file_name}
 * @Package ${package_name}
 * @Description: ${todo}
 * @date 2018/6/417:17
 */
public class CrawlThread implements Runnable{
    private MyCrawl myCrawl;

    public CrawlThread(MyCrawl myCrawl){
        this.myCrawl = myCrawl;
    }
    @Override
    public void run() {
        int n=10000;
        int i=0;
        while(i<n){
            i++;
            myCrawl.urlReader();
        }
    }
}
