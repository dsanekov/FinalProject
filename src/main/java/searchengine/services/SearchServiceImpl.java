package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchObject;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.parser.ClearHtmlTegs;
import searchengine.parser.LemmaFinder;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService{
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    @Override
    public List<SearchObject> searchAllSites(String query, int offset, int limit) throws IOException {
        System.out.println("Начинаем поиск по всем сайтам");
        Iterable<Site> siteIterable = siteRepository.findAll();
        List<SearchObject> resultList = new ArrayList<>();
        List<Lemma> foundLemmaList = new ArrayList<>();
        Set<String> textLemmaSet = LemmaFinder.getInstance().getLemmaSet(query);
        for (Site site : siteIterable) {
            foundLemmaList.addAll(getLemmaListFromSite(textLemmaSet, site));
        }
        for(Site site : siteIterable){
            foundLemmaList = deleteFrequentLemmas(foundLemmaList,site);
        }

        List<SearchObject> searchData = new ArrayList<>();
        for (Lemma l : foundLemmaList) {
            for(String lemmaQuery : textLemmaSet) {
                if (l.getLemma().equals(lemmaQuery)) {
                    searchData.addAll(getSearchDtoList(foundLemmaList, textLemmaSet, offset, limit));
                    searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
                    if (searchData.size() > limit) {
                        for (int i = offset; i < limit; i++) {
                            resultList.add(searchData.get(i));
                        }

                        System.out.println("Поиск по всем сайтам завершен");
                        return resultList;
                    }
                }
            }
        }
        System.out.println("Поиск по всем сайтам завершен");
        return searchData;
    }

    @Override
    public List<SearchObject> searchOnSite(String query, String url, int offset, int limit) throws IOException {
        System.out.println("Начинаем поиск по сайту - " + url);
        Site site = siteRepository.findSiteByUrl(url);
        Set<String> textLemmaSet = LemmaFinder.getInstance().getLemmaSet(query);
        List<Lemma> foundLemmaList = getLemmaListFromSite(textLemmaSet, site);
        return getSearchDtoList(deleteFrequentLemmas(foundLemmaList,site), textLemmaSet, offset, limit);
    }
    private List<Lemma> deleteFrequentLemmas(List<Lemma> foundLemmaList, Site site){
        long allPagesCount = pageRepository.countBySiteId(site);
        float percentOFPages = 0.2f;
        for(Lemma lemma : foundLemmaList){
            if(indexRepository.countByLemmaId(lemma.getId()) > allPagesCount*percentOFPages){
                System.out.println("Удаляем частую лемму");
                foundLemmaList.remove(lemma);
            }
        }
        return foundLemmaList;
    }

    private List<Lemma> getLemmaListFromSite(Set<String> lemmas, Site site) {
        List<Lemma> lemmaList = lemmaRepository.findLemmaListBySetAndSite(lemmas, site.getId());
        List<Lemma> result = new ArrayList<>(lemmaList);
        result.sort(Comparator.comparingInt(Lemma::getFrequency));
        return result;
    }

    private List<SearchObject> getSearchData(Hashtable<Page, Float> pageList, Set<String> textLemmaList) throws IOException {
        List<SearchObject> result = new ArrayList<>();

        for (Page page : pageList.keySet()) {
            String uri = page.getPath();
            String content = page.getContent();
            Site pageSite = page.getSiteId();
            String site = pageSite.getUrl();
            String siteName = pageSite.getName();
            Float absRelevance = pageList.get(page);

            StringBuilder clearContent = new StringBuilder();
            String title = ClearHtmlTegs.clear(content, "title");
            String body = ClearHtmlTegs.clear(content, "body");
            clearContent.append(title).append(" ").append(body);
            String snippet = getSnippet(clearContent.toString(), textLemmaList);

            result.add(new SearchObject(site, siteName, uri, title, snippet, absRelevance));
        }
        return result;
    }

    private String getSnippet(String content, Set<String> lemmaSet) throws IOException {

        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmaSet) {
            lemmaIndex.addAll(LemmaFinder.getInstance().findLemmaIndexInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < lemmaIndex.size() && lemmaIndex.get(nextPoint) - end > 0 && lemmaIndex.get(nextPoint) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint += 1;
            }
            i = nextPoint - 1;
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start) != -1) {
            prevPoint = content.lastIndexOf(" ", start);
        } else prevPoint = start;
        if (content.indexOf(" ", end + 30) != -1) {
            lastPoint = content.indexOf(" ", end + 30);
        } else lastPoint = content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }

    private List<SearchObject> getSearchDtoList(List<Lemma> lemmaList, Set<String> textLemmaList, int offset, int limit) throws IOException {
        List<SearchObject> result = new ArrayList<>();
        if (lemmaList.size() >= textLemmaList.size()) {
            List<Page> foundPageList = pageRepository.findByLemmaList(lemmaList);
            List<Index> foundIndexList = indexRepository.findIndexListByLemmasAndPages(lemmaList, foundPageList);
            Hashtable<Page, Float> sortedPageByAbsRelevance = getPageAbsRelevance(foundPageList, foundIndexList);
            List<SearchObject> dataList = getSearchData(sortedPageByAbsRelevance, textLemmaList);

            if (offset > dataList.size()) {
                return new ArrayList<>();
            }

            if (dataList.size() > limit) {
                for (int i = offset; i < limit; i++) {
                    result.add(dataList.get(i));
                }
                return result;
            } else return dataList;
        } else
            return result;
    }

    private Hashtable<Page, Float> getPageAbsRelevance(List<Page> pageList, List<Index> indexList) {
        HashMap<Page, Float> pageWithRelevance = new HashMap<>();
        for (Page page : pageList) {
            float relevant = 0;
            for (Index index : indexList) {
                if (index.getPage() == page) {
                    relevant += index.getRank();
                }
            }
            pageWithRelevance.put(page, relevant);
        }
        HashMap<Page, Float> pageWithAbsRelevance = new HashMap<>();
        for (Page page : pageWithRelevance.keySet()) {
            float absRelevant = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pageWithAbsRelevance.put(page, absRelevant);
        }

        return pageWithAbsRelevance
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, Hashtable::new));
    }


}
