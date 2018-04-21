package be.thomaswinters.samson.burgemeester.generators;

import be.thomaswinters.generator.related.FilteringRelatedGenerator;
import be.thomaswinters.generator.related.IRelatedGenerator;
import be.thomaswinters.generator.related.MappingRelatedGenerator;
import be.thomaswinters.generator.related.RelatedGenerator;
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

public class ActionGeneratorBuilder {
    private final WikiHowPageScraper wikiHow;
    private final WikihowSearcher wikiHowSearcher;
    private final ISentenceFixer fixer;

    private final IRelatedGenerator<String> relatedGenerator =
            new FilteringRelatedGenerator<String>(
                    new MappingRelatedGenerator<String>(
                            new RelatedGenerator<>(
                                    this::getRandomTitle,
                                    this::getActionRelevantTo),
                            this::fixAction),
                    this::isValidAction
            );

    //region CONSTRUCTOR
    @SafeVarargs
    public ActionGeneratorBuilder(String language, WikihowSearcher wikiHowSearcher, UnaryOperator<String>... fixers) {
        this.wikiHow = new WikiHowPageScraper(language);
        this.wikiHowSearcher = wikiHowSearcher;
        this.fixer = new CompositeFixer(fixers);

    }

    @SafeVarargs
    public ActionGeneratorBuilder(String language, UnaryOperator<String>... fixers) {
        this(language, WikihowSearcher.fromEnvironment(language), fixers);
    }
    //endregion

    //region BUILDEr

    public IRelatedGenerator<String> buildGenerator() {
        return relatedGenerator;
    }

    //endregion

    private Optional<String> getRandomTitle() {
        try {
            return Optional.of(wikiHow.scrapeRandomCard().getTitle());
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<String> getActionRelevantTo(String message) {
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

    private String fixAction(String action) {
        return fixer.apply(action);
    }

}
