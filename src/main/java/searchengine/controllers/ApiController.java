package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.sql.SQLException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public void startIndexing() throws SQLException {
        indexingService.startIndexing();//старт обхода сайтов
    }
    @GetMapping("/stopIndexing")
    public void stopIndexing() throws SQLException {
        indexingService.stopIndexing();//остановка обхода сайтов
    }
    @PostMapping("/indexPage")
    public void indexPage(@RequestParam String url){
        indexingService.indexingByUrl(url);
    }
}
