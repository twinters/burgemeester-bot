package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.bot.IChatBot;
import be.thomaswinters.bot.data.IChatMessage;
import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.generator.generators.SelectionGenerator;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.language.SubjectType;
import be.thomaswinters.language.dutch.DutchSentenceSubjectReplacer;
import be.thomaswinters.language.stringmorpher.Decapitaliser;
import be.thomaswinters.samson.burgemeester.util.ActionGenerator;
import be.thomaswinters.samson.burgemeester.util.DutchActionNegator;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.text.generator.IStringGenerator;
import be.thomaswinters.textgeneration.domain.context.ITextGeneratorContext;
import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.generators.StaticTextGenerator;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import be.thomaswinters.twitter.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.TwitterBotExecutor;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class BurgemeesterBot implements IStringGenerator, IChatBot {

    private static final String NEDERLANDS = "nl";

    private final DutchSentenceSubjectReplacer subjectReplacer = new DutchSentenceSubjectReplacer();
    private final DutchActionNegator negator = new DutchActionNegator();

    private final ITextGenerator toespraakTemplatedGenerator;

    private final IGenerator<String> tweetGenerator =
            new SelectionGenerator<String>(
                    this::createRandomToespraak,
                    new RouletteWheelSelection<>(
                            this::getToespraakFitness
                    ),
                    8
            );

    private final ActionGenerator actionGenerator = new ActionGenerator(NEDERLANDS,
            Decapitaliser::decapitaliseFirstLetter,
            SentenceUtil::removeBetweenBrackets,
            this::replaceSubject
    );

    //region Toespraak Fixer

    /**
     * Replaces the subject from second person to third person
     *
     * @param input
     * @return
     */
    private String replaceSubject(String input) {
        try {
            return subjectReplacer.replaceSecondPerson(input, "zij", "hun", "hen", "zichzelf", SubjectType.THIRD_SINGULAR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //end region

    public BurgemeesterBot(ITextGenerator toespraakGenerator) {
        this.toespraakTemplatedGenerator = toespraakGenerator;
    }


    private double getToespraakFitness(String e) {
        if (e.contains("niet") || e.contains("geen")) {
            return 1d;
        }
        return 20d;

    }


    public ITextGeneratorContext createGenerationContext(String action) {
        NamedGeneratorRegister register = new NamedGeneratorRegister();
        register.createGenerator("actie", new StaticTextGenerator(action));
        return new TextGeneratorContext(new ArrayList<>(), register, true);
    }

    public String createToespraak(String action) throws IOException {
        return toespraakTemplatedGenerator.generate(createGenerationContext(action));
    }

    public Optional<String> createRandomToespraak() {
        try {
            return Optional.of(createToespraak(actionGenerator.getRandomAction()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> generateText() {
        return tweetGenerator.generate();
    }


    @Override
    public Optional<String> generateReply(IChatMessage message) {
        try {
            Optional<String> relevantAction = actionGenerator.getActionRelevantTo(message.getMessage());
            if (relevantAction.isPresent()) {
                return Optional.of(createToespraak(relevantAction.get()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();

    }

    public static void main(String[] args) throws IOException, TwitterException {
        BurgemeesterBot burgemeesterBot = new BurgemeesterBotLoader().build();
        TwitterBot bot = new GeneratorTwitterBot(TwitterFactory.getSingleton(), burgemeesterBot, burgemeesterBot);
        new TwitterBotExecutor(bot).run(args);
    }


}
