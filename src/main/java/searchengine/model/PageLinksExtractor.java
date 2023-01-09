package searchengine.model;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;

public class PageLinksExtractor extends RecursiveTask <Set<String>>  {
    private Page page;
    private Site site;

    public PageLinksExtractor(Page page,Site site) {
        this.page = page;
        this.site = site;
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
                        set.add(path);
                        PageLinksExtractor task = new PageLinksExtractor(newPage,site);
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
