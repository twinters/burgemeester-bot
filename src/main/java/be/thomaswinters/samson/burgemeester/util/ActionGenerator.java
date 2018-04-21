package be.thomaswinters.samson.burgemeester.util;

import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.text.fixers.CompositeFixer;
import be.thomaswinters.text.fixers.ISentenceFixer;
import be.thomaswinters.twitter.util.TwitterUtil;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import be.thomaswinters.wikihow.WikihowSearcher;
import be.thomaswinters.wikihow.data.PageCard;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ActionGenerator {
    private final WikiHowPageScraper wikiHow;
    private final WikihowSearcher wikiHowSearcher;
    private final ISentenceFixer fixer;

    public ActionGenerator(String language, WikihowSearcher wikiHowSearcher, UnaryOperator<String>... fixers) {
        this.wikiHow = new WikiHowPageScraper(language);
        this.wikiHowSearcher = wikiHowSearcher;
        this.fixer = new CompositeFixer(fixers);
    }

    public ActionGenerator(String language, UnaryOperator<String>... fixers) {
        this(language, WikihowSearcher.fromEnvironment(language), fixers);
    }

    public String getRandomAction() throws IOException {
        String randomAction = null;
        do {
            randomAction = fixer.apply(wikiHow.scrapeRandomCard().getTitle());
        } while (!isValidAction(randomAction));

        return randomAction;
    }

    public Optional<String> getActionRelevantTo(String message) {
        List<String> searchWords = SentenceUtil.splitOnSpaces(message)
                .filter(e -> !TwitterUtil.isTwitterWord(e))
                .map(SentenceUtil::removePunctuations)
                .filter(SentenceUtil::hasOnlyLetters)
                .collect(Collectors.toList());

        System.out.println("Searching on WikiHow for: " + searchWords);


        List<PageCard> pages = new ArrayList<>();
        while (pages.isEmpty() && !searchWords.isEmpty()) {
            try {
                pages = wikiHowSearcher.search(searchWords);
            } catch (HttpStatusException e) {
                System.out.println("Couldn't find anything for " + searchWords);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (pages.isEmpty()) {
                searchWords = removeShortestWords(searchWords);
            }
        }

        return getFirstAction(pages);
    }

    private List<String> removeShortestWords(List<String> searchWords) {
        OptionalInt lowestAmountOfLetters = searchWords.stream().mapToInt(String::length).min();
        if (lowestAmountOfLetters.isPresent()) {
            searchWords = searchWords.stream()
                    .filter(e -> e.length() > lowestAmountOfLetters.getAsInt())
                    .collect(Collectors.toList());
        }
        return searchWords;
    }

    private Optional<String> getFirstAction(List<PageCard> pages) {
        return pages.stream()
                .map(PageCard::getTitle)
                .map(fixer)
                .findFirst();
    }

    private boolean isValidAction(String title) {
        return !SentenceUtil.containsCapitalisedLetters(title) && !title.startsWith("tips");
    }

}
