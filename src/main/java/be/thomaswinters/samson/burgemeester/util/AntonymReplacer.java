package be.thomaswinters.samson.burgemeester.util;

import be.thomaswinters.language.dutch.negator.NegatorRule;
import be.thomaswinters.wiktionarynl.data.IWiktionaryWord;
import be.thomaswinters.wiktionarynl.data.Language;
import be.thomaswinters.wiktionarynl.data.WiktionaryPage;
import be.thomaswinters.wiktionarynl.scraper.WiktionaryPageScraper;

import java.io.IOException;
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

        List<String> words = getWords(input);

        String output = input;
        for (String word : words) {
            WiktionaryPage wiktionaryPage = scraper.scrapePage(word);
            if (wiktionaryPage.hasLanguage(language)) {
                IWiktionaryWord wiktionaryWord = wiktionaryPage.getWord(language);
                List<String> antonyms = wiktionaryWord.getAntonyms()
                        .stream()
                        .map(e -> e.getWord())
                        .collect(Collectors.toList());

                if (!antonyms.isEmpty()) {
                    System.out.println("Antoniemen van " + word + ": " + antonyms);
                    output = input.replaceAll(word, antonyms.get(random.nextInt(antonyms.size())));

                    // Sanity check: did something really change?
                    if (!output.equals(input)) {
                        System.out.println("-- Antonym");
                        return Optional.of(output);
                    }
                }
            }
        }


        return Optional.empty();
    }

    private List<String> getWords(String input) {

        return Stream.of(input.split(" "))
                .map(word -> word.replaceAll("\\W", ""))
                .collect(Collectors.toList());


    }
}
