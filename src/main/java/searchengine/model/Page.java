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
    @ManyToOne ()
    @JoinColumn (name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(length = 16777215, columnDefinition = "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }
    public Page(){}


    public TreeSet<String> getChildLinks (){
        TreeSet<String> childLinks = new TreeSet<>();
        try {
            Document doc = Jsoup.connect(path)
                     .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .get();
            Thread.sleep(150);
            Elements link = doc.select("a");
            for (Element e : link) {
                String linkAddress = e.attr("abs:href");
                if(linkAddress.startsWith(path)
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
