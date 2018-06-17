package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.generator.generators.related.IRelatedGenerator;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.language.SubjectType;
import be.thomaswinters.language.dutch.DutchSentenceSubjectReplacer;
import be.thomaswinters.language.stringmorpher.Decapitaliser;
import be.thomaswinters.samson.burgemeester.generators.ActionGeneratorBuilder;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.text.generator.IStringGenerator;
import be.thomaswinters.textgeneration.domain.context.ITextGeneratorContext;
import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.databases.DeclarationFileTextGenerator;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import be.thomaswinters.twitter.bot.TwitterBotExecutor;
import be.thomaswinters.twitter.util.TwitterUtil;
import twitter4j.TwitterException;

import java.io.IOException;
import java.util.*;

public class BurgemeesterBot implements IStringGenerator, IChatBot {

    private final DutchSentenceSubjectReplacer subjectReplacer = new DutchSentenceSubjectReplacer();

    private final DeclarationFileTextGenerator toespraakTemplatedGenerator;
    private final List<String> replyWordBlackListWords = Arrays.asList(
            "samson",
            "gert",
            "burgemeester",
            "meneer",
            "de",
            "albert",
            "alberto",
            "AL-BER-TOOOOOOO"
    );

    private final IRelatedGenerator<String, String> actionGenerator =
            new ActionGeneratorBuilder("nl", replyWordBlackListWords)
                    .buildGenerator()
                    .map(Decapitaliser::decapitaliseFirstLetter)
                    .map(SentenceUtil::removeBetweenBrackets)
                    .map(this::replaceSubject);
    private final IGenerator<String> randomToespraakGenerator =
            actionGenerator.updateGenerator(
                    generator -> generator
                            .filter(10, title -> !SentenceUtil.containsCapitalisedLetters(title))
                            .filter(title -> !title.startsWith("tips"))
                            .map(this::createToespraakForAction)
                            .select(8,
                                    new RouletteWheelSelection<>(this::getToespraakFitness)));


    public BurgemeesterBot(DeclarationFileTextGenerator toespraakGenerator) {
        this.toespraakTemplatedGenerator = toespraakGenerator;
    }

    //region Toespraak Fixer

    public static void main(String[] args) throws IOException, TwitterException {
        new TwitterBotExecutor(new BurgemeesterBotLoader().buildTwitterBot()).run(args);
    }
    //end region

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

    private double getToespraakFitness(String e) {
        if (e.contains("niet") || e.contains("geen")) {
            return 1d;
        }
        if (e.contains("uit") && e.contains("in")) {
            return 4d;
        }
        return 20d;
    }

    private ITextGeneratorContext createGenerationContext(String action) {
        NamedGeneratorRegister register = new NamedGeneratorRegister();
        register.createGenerator("actie", action);
        return new TextGeneratorContext(new ArrayList<>(), register, true);
    }

    private String createToespraakForAction(String action) {
        return toespraakTemplatedGenerator.generate(createGenerationContext(action));
    }

    @Override
    public Optional<String> generateText() {
        return randomToespraakGenerator.generate();
    }

    @Override
    public Optional<String> generateReply(IChatMessage message) {
        System.out.println("Generating reply for: " + message.getText());

        Optional<String> optionalAction = actionGenerator.generateRelated(message.getText());
        return optionalAction.map(action -> {
                    ITextGeneratorContext register = createGenerationContext(action);
                    SentenceUtil.splitOnSpaces(message.getText())
                            .filter(e->!TwitterUtil.isTwitterWord(e))
                            .map(SentenceUtil::removeNonLetters)
                            .max(Comparator.comparingInt(String::length))
                            .ifPresent(longWord -> register.createGenerator("langstewoord", longWord));
                    return toespraakTemplatedGenerator.generate("reply", register);
                }
        );
    }


}
