package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.*;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final SiteRepository siteRepository;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService, SiteRepository siteRepository) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
        this.siteRepository = siteRepository;
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
                                         @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) throws IOException {
        if(query.isEmpty()){
            return new ResponseEntity<>(new UnsuccessfulResponse(false,"Задан пустой поисковый запрос"),HttpStatus.BAD_REQUEST);
        }
        if(!site.isEmpty() && siteRepository.findSiteByUrl(site) == null){
            return new ResponseEntity<>(new UnsuccessfulResponse(false,"Указанная страница не найдена"),HttpStatus.NOT_FOUND);
        }
        if(!site.isEmpty() && siteRepository.findSiteByUrl(site) != null){
            List<SearchObject> searchObjectList = searchService.searchOnSite(query, site, offset, limit);
            return new ResponseEntity<>(new SearchResponse(true,searchObjectList.size(),searchObjectList),HttpStatus.OK);
        }

        List<SearchObject> searchObjectList = searchService.searchAllSites(query, offset, limit);
        if(searchObjectList == null){
            return new ResponseEntity<>(new UnsuccessfulResponse(false,"Указанная страница не найдена"),HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(new SearchResponse(true,searchObjectList.size(),searchObjectList),HttpStatus.OK);
    }
}
