package searchengine.parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

public class PageLinksExtractor extends RecursiveTask <List<Page>>  {
    private String path;
    private Site site;
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private static List<String> urlList = new CopyOnWriteArrayList<>();

    public PageLinksExtractor(String path,Site site, PageRepository pageRepository, SiteRepository siteRepository) {
        this.path = path;
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
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
            List<Page> pageListForSave = new ArrayList<>();
            for (Element e : link) {
                String url = e.attr("abs:href").trim();

                    if (urlList.contains(url)) {
                        continue;
                    }
                if(url.startsWith(path)
                        && !url.contains("?")
                        && (url.charAt(url.length()-1) == '/')
                        ) {
                    urlList.add(url);
                    System.out.println(url);
                    Page newPage = new Page(site,url,code,html);
                    pageListForSave.add(newPage);
                    pageList.add(newPage);
                    PageLinksExtractor task = new PageLinksExtractor(url,site,pageRepository,siteRepository);
                    task.fork();
                    taskList.add(task);
                }
            }
            pageRepository.saveAll(pageListForSave);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            for (PageLinksExtractor task : taskList) {
                pageList.addAll(task.join());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return pageList;
    }
    public void clearUrlList(){
        urlList = new CopyOnWriteArrayList<>();
    }

}
