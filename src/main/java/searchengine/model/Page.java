package searchengine.model;

import javax.persistence.*;

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
