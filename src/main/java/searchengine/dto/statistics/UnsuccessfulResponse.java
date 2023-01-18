package searchengine.dto.statistics;

import lombok.Data;
import lombok.Value;

@Value
public class UnsuccessfulResponse {
    private boolean result;
    private String error;
}
