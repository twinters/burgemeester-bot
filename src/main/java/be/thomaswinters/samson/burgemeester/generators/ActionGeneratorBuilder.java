package be.thomaswinters.samson.burgemeester.generators;

import be.thomaswinters.action.ActionExtractor;
import be.thomaswinters.action.data.ActionDescription;
import be.thomaswinters.generator.generators.related.IRelatedGenerator;
import be.thomaswinters.generator.generators.related.RelatedGenerator;
import be.thomaswinters.random.Picker;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.twitter.util.TwitterUtil;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import be.thomaswinters.wikihow.WikihowSearcher;
import be.thomaswinters.wikihow.data.PageCard;
import com.google.common.collect.ImmutableSet;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ActionGeneratorBuilder {
    private final WikiHowPageScraper wikiHow;
    private final WikihowSearcher wikiHowSearcher;
    private final Collection<String> replyWordBlackListWords;
    private final ActionExtractor actionExtractor = new ActionExtractor();
    private final IRelatedGenerator<String, String> relatedGenerator =
            new RelatedGenerator<>(this::getRandomTitle, this::getActionRelevantTo);

    //region CONSTRUCTOR
    public ActionGeneratorBuilder(String language, WikihowSearcher wikiHowSearcher, Collection<String> replyWordBlackListWords) throws IOException {
        this.wikiHow = new WikiHowPageScraper(language);
        this.wikiHowSearcher = wikiHowSearcher;
        this.replyWordBlackListWords = ImmutableSet.copyOf(replyWordBlackListWords);

    }
    //endregion

    //region BUILDER

    public ActionGeneratorBuilder(String language, Collection<String> blackListWords) throws IOException {
        this(language, WikihowSearcher.fromEnvironment(language, Duration.ofSeconds(5)), blackListWords);
    }

    //endregion

    public IRelatedGenerator<String, String> buildGenerator() {
        return relatedGenerator;
    }

    private Optional<String> getRandomTitle() {
        try {
            return Optional.of(wikiHow.scrapeRandomCard().getTitle());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Optional<String> getActionRelevantTo(String message)  {
        String searchWords = SentenceUtil.splitOnSpaces(message)
                .filter(e -> !TwitterUtil.isTwitterWord(e))
                .map(SentenceUtil::removePunctuations)
                .filter(SentenceUtil::hasOnlyLetters)
                .filter(this::isAllowedWord)
                .collect(Collectors.joining(" "));

        Optional<ActionDescription> actionDescription = Optional.empty();
        try {
            actionDescription = Picker.pickOptional(actionExtractor.extractAction(searchWords));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (actionDescription.isPresent()) {
            searchWords = actionDescription.get().getVerb() + actionDescription.get().getRestOfSentence();

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
//            if (pages.isEmpty()) {
//                searchWords = removeShortestWords(searchWords);
//            }
            }

            return getFirstAction(pages);
        }
        return Optional.empty();
        //.or(this::getRandomTitle);
    }

    private boolean isAllowedWord(String s) {
        return !replyWordBlackListWords.contains(s.toLowerCase());
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


}
