package searchengine.model;
import javax.persistence.*;
import javax.persistence.Index;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Entity
@Setter
@Getter
@Table(name = "page", indexes = {@Index(name = "path_list", columnList = "path")})
public class Page {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "site_id", referencedColumnName = "id")
    private Site siteId;
    @Column(columnDefinition = "VARCHAR(515)", nullable = false)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(length = 16777215, columnDefinition = "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<searchengine.model.Index> indexList = new ArrayList<>();

    public Page(Site site, String path, int code, String content) {
        this.siteId = site;
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
            Elements link = doc.select("body").select("a");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return id == page.id && code == page.code && Objects.equals(siteId, page.siteId) && Objects.equals(path, page.path) && Objects.equals(content, page.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, siteId, path, code, content);
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", site=" + siteId +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content='" + content + '\'' +
                '}';
    }
}
