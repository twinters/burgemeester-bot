package be.thomaswinters.samson.burgemeester.util;

import be.thomaswinters.language.dutch.negator.*;
import be.thomaswinters.wiktionarynl.data.Language;
import be.thomaswinters.wiktionarynl.scraper.WiktionaryPageScraper;
import org.languagetool.CachedLanguageTool;
import org.languagetool.ILanguageTool;
import org.languagetool.JLanguageTool;
import org.languagetool.JLanguageToolAdaptor;
import org.languagetool.language.Dutch;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Negates a Dutch action
 */
public class DutchActionNegator {
    private final NegatorRule rule;

    public DutchActionNegator(NegatorRule rule) {
        this.rule = rule;
    }

    public DutchActionNegator() {
        this(createDefaultRules());
    }

    private static NegatorRule createDefaultRules() {
        ILanguageTool languageTool = new CachedLanguageTool(new JLanguageToolAdaptor(new JLanguageTool(new Dutch())));
        WiktionaryPageScraper wiktionaryScraper = new WiktionaryPageScraper("nl");
        Language dutch = new Language("Nederlands");

        return new CascadeNegatorRule(Arrays.asList(
                new AntonymReplacer(wiktionaryScraper, dutch),
                new WordReplacerRule(languageTool, "zonder", "met"),
                new WordReplacerRule(languageTool, "met", "zonder", Arrays.asList("omgaan")),
                new WordReplacerRule(languageTool, "in", "buiten"),
                new WordReplacerRule(languageTool, "uit", "binnen"),
                new FirstWordReplacerRule(languageTool, "een", "geen"),
                new FirstWordReplacerRule(languageTool, "hun", "iemand anders zijn"),
                new ErNietRule(languageTool),
                new VerbFirstRule(languageTool),
                new NounFirstRule(languageTool),
                new SimpleNietRule()));
    }

    public Optional<String> negateAction(String input) throws IOException, ExecutionException {
        return rule.negateAction(input);
    }
}
