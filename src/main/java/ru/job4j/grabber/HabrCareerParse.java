package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.model.Post;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    private static final int MAX_PAGES_COUNT = 5;

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    @Override
    public List<Post> list(String link) {
        List<Post> posts = new ArrayList<>();
        try {
            for (int i = 1; i <= MAX_PAGES_COUNT; i++) {
                Connection connection = Jsoup.connect(String.format("%s%s", link, i));
                Document document = connection.get();
                Elements rows = document.select(".vacancy-card__inner");
                rows.forEach(row -> posts.add(getPostFromRow(row)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return posts;
    }

    public static void main(String[] args) throws IOException {
        HabrCareerParse parse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> posts = parse.list(PAGE_LINK);
        posts.forEach(System.out::println);
    }

    private String retrieveDescription(String link) {
        StringBuilder description = new StringBuilder();
        try {
            Connection connection = Jsoup.connect(link);
            Document document = connection.get();
            Element descriptionElement = document.select(".job_show_description__vacancy_description").first();
            descriptionElement.child(0).getAllElements().forEach(e -> description
                    .append(e.text()).append(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return description.toString();
    }

    private Post getPostFromRow(Element row) {
        Element titleElement = row.select(".vacancy-card__title").first();
        Element linkElement = titleElement.child(0);
        Element dateElement = row.select(".vacancy-card__date").first().child(0);
        String vacancyName = titleElement.text();
        String vacancyLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        String vacancyCreated = dateElement.attr("datetime");
        String vacancyDescription = retrieveDescription(vacancyLink);
        return new Post.Builder()
                .buildTitle(vacancyName)
                .buildLink(vacancyLink)
                .buildDescription(vacancyDescription)
                .buildCreated(dateTimeParser.parse(vacancyCreated))
                .build();
    }
}
