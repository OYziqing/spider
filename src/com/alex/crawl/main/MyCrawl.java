package com.alex.crawl.main;

import com.alex.crawl.page.HttpCrawl;
import com.alex.crawl.util.CrawlThread;
import com.alex.crawl.util.MysqlConnecter;
import com.huaban.analysis.jieba.JiebaSegmenter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zjx
 * @Title: ${file_name}
 * @Package ${package_name}
 * @Description: ${todo}
 * @date 2018/6/410:41
 */
public class MyCrawl {
    private ArrayList<String> urlList = new ArrayList<>();
    private HttpCrawl httpCrawl = new HttpCrawl();
    private int index=0;
    private MysqlConnecter connector = new MysqlConnecter();
    public static void main(String[] args){
        String[]urls = {
                "https://blog.csdn.net/qq_14950717/article/details/78747129",
                "https://blog.csdn.net/xiamizy/article/details/38225993",
                "https://blog.csdn.net/bluishglc/article/details/80423323",
                "https://blog.csdn.net/trigl/article/details/50968079",
                "https://blog.csdn.net/pamchen/article/details/61196099",
                "https://blog.csdn.net/yeyingss/article/details/73087625",
                "https://blog.csdn.net/column/details/evermysql.html",
                "https://blog.csdn.net/column/details/linuxresearch.html",
                "https://blog.csdn.net/21cnbao/article/details/56275456",
                "https://www.cnblogs.com/moonlightL/p/7891803.html"
        };
        for(int i=7;i<8;i++) {
            MyCrawl crawl = new MyCrawl();
            crawl.urlList.add(urls[i]);
            CrawlThread crawlThread = new CrawlThread(crawl);
            Thread thread = new Thread(crawlThread);
            thread.start();
        }
    }
    public void urlReader(){
        if(index<urlList.size()) {
            String url = urlList.get(index);
            index++;
            //查询该url是否在blogs中存在
            int c = connector.isExist("blogs","url",url);
            if(c>0){
                System.out.println("blogs exists "+url);
                return;
            }
            System.out.println("index:"+index);
            String html = httpCrawl.getHtml(url);
            Document doc = Jsoup.parse(html);
            Elements newsList = doc.select("a");
            Elements readCount = doc.select(".float-right");
            Elements titleElement = doc.select(".title-article");
            Elements tagsElements = doc.select(".tags-box").select("a");
            String count="";
            String title="";
            String tags = "";
            int hasReadCount=0;
            for(Element ele: readCount){
                if(ele.text().contains("阅读数")){
                    count = ele.text().split("：")[1];
                    hasReadCount++;
                }
            }
            if(titleElement!=null && titleElement.size()>0){
                title = titleElement.get(0).text();
                hasReadCount++;
            }
            Elements article = doc.select("article");
            String content = "";
            if(article!=null && article.size()>0) {
                content = article.get(0).html();
                hasReadCount++;
            }
            if(tagsElements!=null && tagsElements.size()>0){
                for(Element ta : tagsElements){
                    tags +=ta.text()+";";
                }
                tags = tags.substring(0,tags.length()-1);
            }
            content = getChinese(content);
            content = title+"#"+content;
            if(hasReadCount==3) {
                String sqlInsert = "insert into blogs(title,content,viewCount,url,tags) values('" + title + "','" + content + "',"+count+",'"+url+"','"+tags+"');";
                String sqlSelect = "select * from blogs t where t.url='"+url+"'";
                //System.out.println(sqlInsert);
                try {
                    ArrayList<Map<String,String>> ret = connector.select(sqlSelect,"blogs");
                    if(ret!=null && ret.size()>0){
                        System.out.println(url+" 已经存在！");
                    }else {
                        connector.insert(sqlInsert);
                        System.out.println(url+" 保存成功！");
                    }
                }catch(Exception e){
                    System.out.println("插入失败："+sqlInsert);
                }
            }

            for (Element element : newsList) {
                String nextUrl = element.attr("href");
                String title1 = element.text();
                if(nextUrl.contains("csdn") && !nextUrl.contains("month")) {
                    int a  = connector.isExist("urls","url",nextUrl);
                    int b = connector.isExist("blogs","url",nextUrl);
                    if(a==0 && b==0) {
                        String sqlInsert = "insert into urls(url) values('" + nextUrl + "')";
                        connector.insert(sqlInsert);
                        System.out.println("save to urls "+nextUrl);
                    }else if(a>0){
                        System.out.println("urls exists "+nextUrl);
                    }else if(b>0){
                        System.out.println("blogs exists "+nextUrl);
                    }
                    //urlList.add(nextUrl);
                }
            }
        }else{
            index=0;
            getURLs();
            System.out.println("没有可用网址");
        }
    }
    public void getURLs(){
        /*
        for(int i=0;i<urlList.size();i++){
            int c = connector.isExist("url",urlList.get(i));
            if(c>0){
                continue;
            }else{
                String sqlInsert = "insert into urls(url) values('"+urlList.get(i)+"')";
                connector.insert(sqlInsert);
            }
        }*/
        String sqlDelete = "delete from urls where url in (";
        String urls = "";
        for(int i=0;i<urlList.size();i++){
            urls+="'"+urlList.get(i)+"',";
        }
        sqlDelete+= urls.substring(0,urls.length()-1)+")";
        int c = connector.delete(sqlDelete);
        System.out.println(sqlDelete+","+c);
        urlList.clear();
        String sqlSelect = "select distinct id,url,flag from urls limit 100";
        ArrayList<Map<String,String>> ret = connector.select(sqlSelect,"urls");
        for(int i=0;i<ret.size();i++){
            System.out.println(ret.get(i).get("url")+" add to urlList");
            if(!urlList.contains(ret.get(i).get("url"))) {
                urlList.add(ret.get(i).get("url"));
            }
        }
    }
    public void getKeyWord(String content){
        JiebaSegmenter segmenter = new JiebaSegmenter();
        System.out.println(segmenter.sentenceProcess(content));
        //jieba.analyse.extract_tags(content, topK=topK, withWeight=withWeight);
    }
    /*1、至少匹配一个汉字的写法。
            2、这两个unicode值正好是Unicode表中的汉字的头和尾。
            3、"[]"代表里边的值出现一个就可以，后边的“+”代表至少出现1次，合起来即至少匹配一个汉字。
            */
    public static String getChinese(String paramValue) {
        String regex = "([\u4e00-\u9fa5]+)";
        String str = "";
        Matcher matcher = Pattern.compile(regex).matcher(paramValue);
        while (matcher.find()) {
            str+= matcher.group(0);
        }
        //去掉单引号
        str = str.replaceAll("'","#");
        return str;
    }

    /*
    public void xx(String ){
        for (int i = 0; i < max_iter; ++i)
        {
            Map<String, Float> m = new HashMap<String, Float>();
            float max_diff = 0;
            for (Map.Entry<String, Set<String>> entry : words.entrySet())
            {
                String key = entry.getKey();
                Set<String> value = entry.getValue();
                m.put(key, 1 - d);
                for (String other : value)
                {
                    int size = words.get(other).size();
                    if (key.equals(other) || size == 0) continue;
                    m.put(key, m.get(key) + d / size * (score.get(other) == null ? 0 : score.get(other)));
                }
                max_diff = Math.max(max_diff, Math.abs(m.get(key) - (score.get(key) == null ? 0 : score.get(key))));
            }
            score = m;
            if (max_diff <= min_diff) break;
        }
        排序后的投票
    }*/
    /*
    public static String TFIDF (String title,String content, int topK){

        FilterRecognition filterRecognition = new FilterRecognition();
        filterRecognition.insertStopWords(stopWords);
        filterRecognition.insertStopWord("事儿", "有没有", "前有", "后有", "更多");
        filterRecognition.insertStopNatures("d", "p", "m", "r", "w", "a", "j", "l","null","num");

        List<Term> terms = NlpAnalysis.parse(content).recognition(filterRecognition).getTerms();
        //词的总数
        int totalWords= terms.size();
        Map<String, Integer> wordsCount = new HashMap<String, Integer>();
        //根据词的长度加权
        int maxWordLen = 0;

        for(Term term:terms){
            Integer count = wordsCount.get(term.getName());
            count = count == null ? 0 : count;
            wordsCount.put(term.getName(), count+1);
            if(maxWordLen<term.getName().length()){
                maxWordLen = term.getName().length();
            }
        }

        //计算tf
        Map<String, Double> tf = new HashMap<String, Double>();
        for(String word:wordsCount.keySet()){
            tf.put(word, (double)wordsCount.get(word)/(totalWords+1));
        }

        //保留词的长度
        Set<Integer> perWordLen = new HashSet<Integer>();
        //计算每个词的词长权重
        Map<String, Double> lenWeight = new HashMap<String, Double>();
        for( String key:tf.keySet()){
            lenWeight.put(key, (double)key.length()/maxWordLen);
            perWordLen.add(key.length());
        }

        //标题中出现的关键词
        List<Term> titleTerms = NlpAnalysis.parse(title).recognition(filterRecognition).getTerms();
        Map<String, String> titleWords = new HashMap<String, String>();
        for(Term term:titleTerms){
            titleWords.put(term.getName(), term.getNatureStr());
        }
        //计算idf
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for(int len:perWordLen){
            int sum = 0;
            for(String w:wordsCount.keySet()){
                if(w.length()==len){
                    sum += wordsCount.get(w);
                }
            }
            map.put(len, sum);
        }
        Map<String, Double> idf = new HashMap<String, Double>();
        for(String w:wordsCount.keySet()){
            Integer integer = wordsCount.get(w);
            int len = w.length();
            Integer totalSim = map.get(len);
            idf.put(w, Math.log(((double)totalSim/integer)+1));
        }
        //计算每个词的在文章中的权重

        Map<String, Double> wordWeight = new HashMap<String, Double>();

        for(Term term:terms){
            String word = term.getName();
            String nature = term.getNatureStr();
            if(word.length()<2){
                continue;
            }
            if(wordWeight.get(word)!=null){
                continue;
            }
            Double aDouble = tf.get(word);
            Double aDouble1 = idf.get(word);
            double weight = 1.0;
            if(titleWords.keySet().contains(word)){
                weight += 3.0;
            }
            weight += (double)word.length()/maxWordLen;
            switch (nature){
                case "en":
                    weight += 3.0;
                case "nr":
                    weight += 6.0;
                case "nrf":
                    weight += 6.0;
                case "nw" :
                    weight += 3.0;
                case "nt":
                    weight += 6.0;
                case "nz":
                    weight += 3.0;
                case "kw":
                    weight += 3.0;
                case "ns":
                    weight += 3.0;
                default:
                    weight += 1.0;
            }

            wordWeight.put(word,aDouble*aDouble1*weight);
        }

        Map<String, Double> stringDoubleMap = MapUtil.sortByValue(wordWeight);

        List<String> topKSet = new ArrayList<String>();

        int i = 0;
        for(String word:stringDoubleMap.keySet()){
            if(i >= topK){
                break;
            }
            topKSet.add(word+" ``
                    +stringDoubleMap.get(word));
            i++;
        }
        return StringUtils.join(topKSet, "\t");
    }*/
}
