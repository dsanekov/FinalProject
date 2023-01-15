package searchengine.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ClearHtmlTegs {

    public static String clear(String content, String selector) {
        StringBuilder html = new StringBuilder();
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select(selector);
        for (Element el : elements) {
            html.append(el.html());
        }
        return Jsoup.parse(html.toString()).text();
    }
}
