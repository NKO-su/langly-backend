import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URL;

public class RssTest {
    public static void main(String[] args) throws Exception {
        String feedUrl = "https://feeds.bbci.co.uk/news/rss.xml";

        URL url = new URL(feedUrl);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(url));

        for (SyndEntry entry : feed.getEntries()) {
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println("PubDate: " + entry.getPublishedDate());
            System.out.println("Description: "+ entry.getDescription().getValue());
            System.out.println("---");
        }
    }
}