package searchengine.model;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.repositories.PageRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;

public class PageLinksExtractor extends RecursiveTask <Set<String>>  {
    private Page page;
    private Site site;
    @Autowired
    private PageRepository pageRepository;

    public PageLinksExtractor(Page page,Site site, PageRepository pageRepository) {
        this.page = page;
        this.site = site;
        this.pageRepository = pageRepository;
    }

    @Override
    protected Set<String> compute(){
        TreeSet<String> set = new TreeSet<>();
        List<PageLinksExtractor> taskList = new ArrayList<>();
        for(String path : page.getChildLinks()) {
                try {
                    if (!DBConnection.thisPageExists(path)) {
                        int code = 0;
                        String content = "";
                        Page newPage = new Page(site,path,code,content);
                        pageRepository.save(newPage);
                        set.add(path);
                        PageLinksExtractor task = new PageLinksExtractor(newPage,site,pageRepository);
                        task.fork();
                        taskList.add(task);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
        }
        for (PageLinksExtractor task : taskList) {
            set.addAll(task.join());
        }
        return set;
    }
}
