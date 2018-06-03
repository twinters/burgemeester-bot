package be.thomaswinters.samson.burgemeester.generators;

import be.thomaswinters.generator.generators.related.IRelatedGenerator;
import be.thomaswinters.generator.generators.related.RelatedGenerator;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.twitter.util.TwitterUtil;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import be.thomaswinters.wikihow.WikihowSearcher;
import be.thomaswinters.wikihow.data.PageCard;
import com.google.common.collect.ImmutableSet;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ActionGeneratorBuilder {
    private final WikiHowPageScraper wikiHow;
    private final WikihowSearcher wikiHowSearcher;
    private final Collection<String> blackListWords;

    private final IRelatedGenerator<String, String> relatedGenerator =
            new RelatedGenerator<>(this::getRandomTitle, this::getActionRelevantTo)
                    .updateGenerator(generator -> generator
                            .filter(10, this::isValidAction));

    //region CONSTRUCTOR
    public ActionGeneratorBuilder(String language, WikihowSearcher wikiHowSearcher, Collection<String> blackListWords) {
        this.wikiHow = new WikiHowPageScraper(language);
        this.wikiHowSearcher = wikiHowSearcher;
        this.blackListWords = ImmutableSet.copyOf(blackListWords);

    }

    public ActionGeneratorBuilder(String language, Collection<String> blackListWords) {
        this(language, WikihowSearcher.fromEnvironment(language), blackListWords);
    }
    //endregion

    //region BUILDER

    public IRelatedGenerator<String,String> buildGenerator() {
        return relatedGenerator;
    }

    //endregion

    private Optional<String> getRandomTitle() {
        try {
            return Optional.of(wikiHow.scrapeRandomCard().getTitle());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Optional<String> getActionRelevantTo(String message) {
        List<String> searchWords = SentenceUtil.splitOnSpaces(message)
                .filter(e -> !TwitterUtil.isTwitterWord(e))
                .map(SentenceUtil::removePunctuations)
                .filter(SentenceUtil::hasOnlyLetters)
                .filter(this::isAllowedWord)
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
        //.or(this::getRandomTitle);
    }

    private boolean isAllowedWord(String s) {
        return !blackListWords.contains(s.toLowerCase());
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
                .findFirst();
    }

    private boolean isValidAction(String title) {
        return !SentenceUtil.containsCapitalisedLetters(title) && !title.startsWith("tips");
    }

}
