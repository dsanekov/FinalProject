package searchengine.model;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;

public class PageLinksExtractor extends RecursiveTask <Set<String>>  {
    private Page page;

    public PageLinksExtractor(Page page) {
        this.page = page;
    }

    @Override
    protected Set<String> compute(){
        TreeSet<String> set = new TreeSet<>();
        List<PageLinksExtractor> taskList = new ArrayList<>();
        for(String path : page.getChildLinks()) {
            synchronized (Page.getExistingAddresses()) {
                try {
                    if (!DBConnection.thisSiteExists(path)) {
                        Page newPage = new Page(path);//todo пофиксить констурктор
                        set.add(path);
                        PageLinksExtractor task = new PageLinksExtractor(newPage);
                        task.fork();
                        taskList.add(task);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        for (PageLinksExtractor task : taskList) {
            set.addAll(task.join());
        }
        return set;
    }
}
