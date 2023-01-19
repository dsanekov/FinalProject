package searchengine.dto.statistics;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class SearchObject {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

    public SearchObject(String site, String siteName, String uri, String title, String snippet, float relevance) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

}
