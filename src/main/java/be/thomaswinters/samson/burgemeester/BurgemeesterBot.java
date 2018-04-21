package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.bot.IChatBot;
import be.thomaswinters.bot.data.IChatMessage;
import be.thomaswinters.generator.generators.SelectionGenerator;
import be.thomaswinters.generator.related.IRelatedGenerator;
import be.thomaswinters.generator.related.MappingRelatedGenerator;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.language.SubjectType;
import be.thomaswinters.language.dutch.DutchSentenceSubjectReplacer;
import be.thomaswinters.language.stringmorpher.Decapitaliser;
import be.thomaswinters.samson.burgemeester.generators.ActionGeneratorBuilder;
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

    private final DutchSentenceSubjectReplacer subjectReplacer = new DutchSentenceSubjectReplacer();

    private final ITextGenerator toespraakTemplatedGenerator;
    private final IRelatedGenerator<String> actionGenerator =
            new ActionGeneratorBuilder("nl",
                    Decapitaliser::decapitaliseFirstLetter,
                    SentenceUtil::removeBetweenBrackets,
                    this::replaceSubject
            ).buildGenerator();
    private final IRelatedGenerator<String> toespraakGenerator =
            new MappingRelatedGenerator<>(
                    actionGenerator
                            .updateGenerator(generator ->
                                    new SelectionGenerator<String>(
                                            generator,
                                            new RouletteWheelSelection<>(this::getToespraakFitness), 8)),
                    this::createToespraak
            );

    public BurgemeesterBot(ITextGenerator toespraakGenerator) {
        this.toespraakTemplatedGenerator = toespraakGenerator;
    }

    //region Toespraak Fixer

    /**
     * Replaces the subject from second person to third person
     *
     * @param input
     * @return
     */
    private String replaceSubject(String input) {
        try {
            return subjectReplacer.replaceSecondPerson(input,
                    "zij", "hun", "hen", "zichzelf",
                    SubjectType.THIRD_SINGULAR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //end region


    private double getToespraakFitness(String e) {
        if (e.contains("niet") || e.contains("geen")) {
            return 1d;
        }
        return 20d;
    }

    private ITextGeneratorContext createGenerationContext(String action) {
        NamedGeneratorRegister register = new NamedGeneratorRegister();
        register.createGenerator("actie", new StaticTextGenerator(action));
        return new TextGeneratorContext(new ArrayList<>(), register, true);
    }

    private String createToespraak(String action) {
        return toespraakTemplatedGenerator.generate(createGenerationContext(action));
    }


    @Override
    public Optional<String> generateText() {
        return toespraakGenerator.generate();
    }


    @Override
    public Optional<String> generateReply(IChatMessage message) {
        return toespraakGenerator.generateRelated(message.getMessage());
    }

    public static void main(String[] args) throws IOException, TwitterException {
        BurgemeesterBot burgemeesterBot = new BurgemeesterBotLoader().build();
        TwitterBot bot = new GeneratorTwitterBot(TwitterFactory.getSingleton(), burgemeesterBot, burgemeesterBot);
        new TwitterBotExecutor(bot).run(args);
    }


}
