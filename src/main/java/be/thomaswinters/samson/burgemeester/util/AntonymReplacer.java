package be.thomaswinters.samson.burgemeester.util;

import be.thomaswinters.language.dutch.negator.NegatorRule;
import be.thomaswinters.replacement.Replacer;
import be.thomaswinters.textgeneration.domain.util.Picker;
import be.thomaswinters.wiktionarynl.data.IWiktionaryWord;
import be.thomaswinters.wiktionarynl.data.Language;
import be.thomaswinters.wiktionarynl.data.WiktionaryPage;
import be.thomaswinters.wiktionarynl.scraper.WiktionaryPageScraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AntonymReplacer implements NegatorRule {
    private final Random random = new Random();

    private final WiktionaryPageScraper scraper;
    private final Language language;

    public AntonymReplacer(WiktionaryPageScraper wiktionaryScraper, Language language) {
        this.scraper = wiktionaryScraper;
        this.language = language;
    }

    @Override
    public Optional<String> negateAction(String input) throws IOException, ExecutionException {

        List<Replacer> antonymReplacers = getPossibleReplacers(input);

        if (!antonymReplacers.isEmpty()) {
            System.out.println("-- Possible antonyms: " + antonymReplacers);
            Replacer chosenReplacer = Picker.pick(antonymReplacers);
            String output = chosenReplacer.replace(input);
            if (!output.equals(input)) {
                System.out.println("-- Antonym");
                return Optional.of(output);
            }
        }
        return Optional.empty();
    }

    private List<Replacer> getPossibleReplacers(String input) {
        return getWords(input).stream()
                .flatMap(word -> {
                    try {
                        return Replacer.createReplacers(word, getAntonyms(word), false, true).stream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
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
            List<String> antonyms = wiktionaryWord.getAntonyms()
                    .stream()
                    .map(e -> e.getWord())
                    .collect(Collectors.toList());
            return antonyms;
        }
        return new ArrayList<>();
    }

    private List<String> getWords(String input) {

        return Stream.of(input.split(" "))
                .map(word -> word.replaceAll("\\P{L}", ""))
                .collect(Collectors.toList());


    }
}
