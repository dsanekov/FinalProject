package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;

import java.util.List;
@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;


    public void startIndexing(){
        String[] result = { "true", "false" };
        String error = "Данная страница находится за пределами сайтов,указанных в конфигурационном файле";
        List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            //здесь надо форкать и проходить все страницы
        }
    }

    public void stopIndexing(){
        String[] result = { "true", "false" };
        String error = "Индексация не запущена";
    }
}
