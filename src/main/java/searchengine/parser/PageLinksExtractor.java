package searchengine.parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.DBConnection;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

public class PageLinksExtractor extends RecursiveTask <List<Page>>  {
    private String path;
    private Site site;
    @Autowired
    private PageRepository pageRepository;
    private static final List<String> urlList = new CopyOnWriteArrayList<>();

    public PageLinksExtractor(String path,Site site, PageRepository pageRepository) {
        this.path = path;
        this.site = site;
        this.pageRepository = pageRepository;
    }

    @Override
    protected List<Page> compute(){
        List<Page> pageList = new ArrayList<>();
        List<PageLinksExtractor> taskList = new ArrayList<>();
        try {
            Thread.sleep(500);
            Document doc = Jsoup.connect(path)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .get();
            Elements link = doc.select("body").select("a");
            String html = doc.outerHtml();
            int code = doc.connection().response().statusCode();
            for (Element e : link) {
                String url = e.attr("abs:href");
                synchronized (urlList) {
                    if (urlList.contains(url)) {
                        continue;
                    }
                }
                if(url.startsWith(path)
                        && !url.contains("?")
                        && (url.charAt(url.length()-1) == '/')
                        && !urlList.contains(url)) {
                    urlList.add(url);
                    System.out.println(url);
                    Page newPage = new Page(site,url,code,html);
                    pageRepository.save(newPage);
                    pageList.add(newPage);
                    PageLinksExtractor task = new PageLinksExtractor(url,site,pageRepository);
                    task.fork();
                    taskList.add(task);
                }
            }

            for (PageLinksExtractor task : taskList) {
                pageList.addAll(task.join());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return pageList;
    }

    private boolean isPageInDB(String path){
        Iterable<Page> pageIterable = pageRepository.findAll();
        for(Page p : pageIterable){
            if(p.getPath().equals(path))
                return true;
        }
        return false;
    }

}
