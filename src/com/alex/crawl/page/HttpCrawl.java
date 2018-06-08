package com.alex.crawl.page;


import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * @author zjx
 * @Title: ${file_name}
 * @Package ${package_name}
 * @Description: ${todo}
 * @date 2018/6/49:50
 */
public class HttpCrawl {
    public String getHtml(String url){
        String html="";
        //1.生成HttpClient对象并设置参数
        HttpClient httpClient = new DefaultHttpClient();
        //2.设置http超时连接,5s
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,5000);
        //3.生成HttpGet对象并设置参数
        try {
            HttpGet httpGet = new HttpGet(url);
            //4.设置get请求，5s
            httpGet.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,5000);
            HttpResponse response = httpClient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if(statusCode!=200){
                System.out.println(url+":请求失败。"+statusLine.getReasonPhrase());
            }
            HttpEntity httpEntity = response.getEntity();
            if(httpEntity!=null){
                html = readHtmlContentFromEntity(httpEntity);
            }
        }catch (Exception e){
            System.out.println("error:"+url);
            //e.printStackTrace();
        }
        return html;
    }
    private String readHtmlContentFromEntity(HttpEntity httpEntity) throws ParseException, IOException {
        String html = "";
        Header header = httpEntity.getContentEncoding();
        if(httpEntity.getContentLength() < 2147483647L){            //EntityUtils无法处理ContentLength超过2147483647L的Entity
            if(header != null && "gzip".equals(header.getValue())){
                html = EntityUtils.toString(new GzipDecompressingEntity(httpEntity));
            } else {
                html = EntityUtils.toString(httpEntity);
            }
        } else {
            InputStream in = httpEntity.getContent();
            if(header != null && "gzip".equals(header.getValue())){
                html = unZip(in, ContentType.getOrDefault(httpEntity).getCharset().toString());
            } else {
                html = readInStreamToString(in, ContentType.getOrDefault(httpEntity).getCharset().toString());
            }
            if(in != null){
                in.close();
            }
        }
        return html;
    }
    /**
     * 读取InputStream流
     * @param in InputStream流
     * @return 从流中读取的String
     * @throws IOException
     */
    private String readInStreamToString(InputStream in, String charSet) throws IOException {
        StringBuilder str = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, charSet));
        while((line = bufferedReader.readLine()) != null){
            str.append(line);
            str.append("\n");
        }
        if(bufferedReader != null) {
            bufferedReader.close();
        }
        return str.toString();
    }
    /**
     * 解压服务器返回的gzip流
     * @param in 抓取返回的InputStream流
     * @param charSet 页面内容编码
     * @return 页面内容的String格式
     * @throws IOException
     */
    private String unZip(InputStream in, String charSet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPInputStream gis = null;
        try {
            gis = new GZIPInputStream(in);
            byte[] _byte = new byte[1024];
            int len = 0;
            while ((len = gis.read(_byte)) != -1) {
                baos.write(_byte, 0, len);
            }
            String unzipString = new String(baos.toByteArray(), charSet);
            return unzipString;
        } finally {
            if (gis != null) {
                gis.close();
            }
            if(baos != null){
                baos.close();
            }
        }
    }
}
