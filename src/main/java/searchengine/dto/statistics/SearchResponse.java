package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;
@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchObject> data;

    public SearchResponse(boolean result, int count, List<SearchObject> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }
}
