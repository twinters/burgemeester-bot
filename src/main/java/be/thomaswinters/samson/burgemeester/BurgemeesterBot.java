package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.bot.ITextGeneratorBot;
import be.thomaswinters.language.SubjectType;
import be.thomaswinters.language.dutch.DutchSentenceSubjectReplacer;
import be.thomaswinters.language.stringmorpher.Decapitaliser;
import be.thomaswinters.samson.burgemeester.util.DutchActionNegator;
import be.thomaswinters.textgeneration.domain.constraints.LockConstraint;
import be.thomaswinters.textgeneration.domain.context.ITextGeneratorContext;
import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.generators.StaticTextGenerator;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import be.thomaswinters.twitter.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.TwitterBotExecutor;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class BurgemeesterBot implements ITextGeneratorBot {


    private WikiHowPageScraper wikiHow = new WikiHowPageScraper();
    private DutchSentenceSubjectReplacer subjectReplacer = new DutchSentenceSubjectReplacer();
    private DutchActionNegator negator = new DutchActionNegator();
    private final ITextGenerator toespraakGenerator;

    public BurgemeesterBot(ITextGenerator toespraakGenerator) {
        this.toespraakGenerator = toespraakGenerator;
    }

    private String getRandomAction() throws IOException {
        String randomAction = null;
        do {
            randomAction = wikiHow.scrapeRandomCard("nl").getTitle();
            randomAction = Decapitaliser.decapitaliseFirstLetter(randomAction);
        } while (!isValidAction(randomAction));

        return fixAction(randomAction);
    }

    // FIXERS
    private String fixAction(String randomAction) throws IOException {
        return replaceSubject(removeBetweenBrackets(randomAction));
    }

    private String removeBetweenBrackets(String input) {
        return input.replaceAll("\\s*\\([^\\)]*\\)\\s*", " ");
    }

    private String replaceSubject(String input) throws IOException {
        return subjectReplacer.replaceSecondPerson(input, "zij", "hun", "hen", "zichzelf", SubjectType.THIRD_SINGULAR);
    }

    private boolean isValidAction(String title) {
        return !containsCapitalisedLetters(title) && !title.startsWith("tips");
    }

    private boolean containsCapitalisedLetters(String input) {
        return !input.toLowerCase().equals(input);

    }


    public ITextGeneratorContext createGenerationContext(String action) {
        NamedGeneratorRegister register = new NamedGeneratorRegister();
        register.createGenerator("actie", new StaticTextGenerator(action));
        return new TextGeneratorContext(new ArrayList<LockConstraint>(), register, true);
    }

    public String createRandomToespraak() throws IOException {
        String action = getRandomAction();


        return toespraakGenerator.generate(createGenerationContext(getRandomAction()));

//        return "Aan allen die " + action + ": proficiat.\nAan allen die " + negateAction(action) + ": ook proficiat.";
    }

    @Override
    public Optional<String> generateText() {
        try {
            return Optional.of(createRandomToespraak());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static void main(String[] args) throws IOException, TwitterException {
        TwitterBot bot = new GeneratorTwitterBot(TwitterFactory.getSingleton(), new BurgemeesterBotLoader().build());
        new TwitterBotExecutor(bot).run(args);
    }


}
