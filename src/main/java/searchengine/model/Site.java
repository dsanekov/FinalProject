package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


@Entity
@Setter
@Getter
@Table (name = "Site")
public class Site {
    @Id
    @GeneratedValue (strategy = GenerationType.AUTO)
    private int id;
    @Enumerated (EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private SiteStatus status;
    @Column (name = "status_time", nullable = false)
    private LocalDateTime statusTime;
    @Column (name = "last_error",columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;
    @OneToMany (mappedBy = "site", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<Page> pages;

    public Site(SiteStatus status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }
    public Site(){}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site that = (Site) o;
        return id == that.id && status == that.status &&
                statusTime.equals(that.statusTime) &&
                Objects.equals(lastError, that.lastError) &&
                url.equals(that.url) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, statusTime, lastError, url, name, pages);
    }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", status=" + status +
                ", statusTime=" + statusTime +
                ", lastError='" + lastError + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", pages=" + pages +
                '}';
    }
}
