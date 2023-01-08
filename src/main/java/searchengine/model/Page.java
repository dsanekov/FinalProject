package searchengine.model;
import javax.persistence.*;
import java.util.TreeSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Entity
@Table(name = "Page", indexes = {@Index(columnList = "path, site_id", name = "path_index")})
public class Page {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne (cascade = CascadeType.ALL)
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)")
    private String path;
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }


    public TreeSet<String> getChildLinks (){
        TreeSet<String> childLinks = new TreeSet<>();
        try {
            Document doc = Jsoup.connect(path).timeout(10000).ignoreHttpErrors(true).maxBodySize(0).get();//TODO изменить запрос по инструкции! чекни файл
            Thread.sleep(150);
            Elements link = doc.select("a");
            for (Element e : link) {
                String linkAddress = e.attr("abs:href");
                if(linkAddress.startsWith(path)
                        && !linkAddress.contains("pdf")
                        && !linkAddress.contains("#")
                        && !linkAddress.contains("?")
                        && linkAddress.charAt(linkAddress.length()-1) == '/') {
                    childLinks.add(linkAddress);
                    System.out.println(linkAddress);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return childLinks;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
