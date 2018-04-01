package be.thomaswinters.samson.burgemeester.util;

import be.thomaswinters.language.dutch.negator.AReplacerNegator;
import be.thomaswinters.language.dutch.negator.NegatorRule;
import be.thomaswinters.replacement.Replacer;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.wiktionarynl.data.IWiktionaryWord;
import be.thomaswinters.wiktionarynl.data.Language;
import be.thomaswinters.wiktionarynl.data.WiktionaryPage;
import be.thomaswinters.wiktionarynl.scraper.WiktionaryPageScraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AntonymReplacer extends AReplacerNegator implements NegatorRule {

    private final WiktionaryPageScraper scraper;
    private final Language language;

    public AntonymReplacer(WiktionaryPageScraper wiktionaryScraper, Language language) {
        this.scraper = wiktionaryScraper;
        this.language = language;
    }


    protected List<Replacer> getPossibleReplacers(String input) {
        return SentenceUtil.getWords(input).stream()
                .flatMap(word -> {
                    try {
                        return Replacer.createReplacers(word, getAntonyms(word), false, true).stream();
                    } catch (IOException | ExecutionException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private List<String> getAntonyms(String word) throws IOException, ExecutionException {
        WiktionaryPage wiktionaryPage = scraper.scrapePage(word);
        if (wiktionaryPage.hasLanguage(language)) {
            IWiktionaryWord wiktionaryWord = wiktionaryPage.getWord(language);
            return wiktionaryWord.getAntonyms()
                    .stream()
                    .map(IWiktionaryWord::getWord)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return "AntonymReplacer{" +
                "scraper=" + scraper +
                ", language=" + language +
                '}';
    }
}
