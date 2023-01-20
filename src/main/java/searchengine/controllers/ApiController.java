package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.SuccessfulResponse;
import searchengine.dto.statistics.UnsuccessfulResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing(){
        if(indexingService.startIndexingAllSites()){
            return new ResponseEntity<>(new SuccessfulResponse(true),HttpStatus.OK);
        }
        return new ResponseEntity<>(new UnsuccessfulResponse(false,"Индексация уже запущена"),HttpStatus.BAD_REQUEST);
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing(){
        if(indexingService.stopIndexing()){
            return new ResponseEntity<>(new SuccessfulResponse(true),HttpStatus.OK);
        }
        return new ResponseEntity<>(new UnsuccessfulResponse(false,"Индексация не запущена"),HttpStatus.BAD_REQUEST);
    }
    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam String url){
        if(indexingService.startIndexingByUrl(url)){
            return new ResponseEntity<>(new SuccessfulResponse(true),HttpStatus.OK);
        }
        return new ResponseEntity<>(new UnsuccessfulResponse(false,"Данная страница находится за пределами сайтов, указанных в конфигурационном файле"),HttpStatus.BAD_REQUEST);
    }
    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query") String query,
                                         @RequestParam(name = "site",required = false, defaultValue = "") String site,
                                         @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                         @RequestParam(name = "limit", required = false, defaultValue = "20") int limit){
        if(query.isEmpty()){
            return new ResponseEntity<>(new UnsuccessfulResponse(false,"Задан пустой поисковый запрос"),HttpStatus.BAD_REQUEST);
        }
        if(site.isEmpty()){
            searchService.searchAllSites(query,offset,limit);
        }
        return new ResponseEntity<>(new SuccessfulResponse(true),HttpStatus.OK);
        //todo доделать ответ на запрос. Создать класс для ответа, как было со статистикой.
    }
}
