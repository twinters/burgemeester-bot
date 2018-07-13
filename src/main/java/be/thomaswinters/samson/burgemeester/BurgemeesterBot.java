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
import be.thomaswinters.textgeneration.domain.context.ITextGeneratorContext;
import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.databases.DeclarationFileTextGenerator;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import be.thomaswinters.twitter.util.TwitterUtil;
import twitter4j.TwitterException;

import java.io.IOException;
import java.util.*;

// Todo: use related generator here instead of these
public class BurgemeesterBot implements IRelatedGenerator<String, IChatMessage> {

    private static final List<String> samsonBotWords = Arrays.asList(

            // Gert woorden
            "maar", "betekent",

            // Alberto woorden
            "lekker", "overheerlijk(e)?",

            // Octaaf woorden
            "specialiteit.*", "toevallig",

            // Jeanine woorden
            "moeder", "vader", "typisch", "hobbyclub.*", "voorzitster", "jongen"
    );
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

    //region Toespraak Fixer

    public BurgemeesterBot(DeclarationFileTextGenerator toespraakGenerator) {
        this.toespraakTemplatedGenerator = toespraakGenerator;
    }
    //end region

    public static void main(String[] args) throws IOException, TwitterException {
        new BurgemeesterBotLoader().buildTwitterBot().createExecutor().run(args);
    }

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
    public Optional<String> generate() {
        return randomToespraakGenerator.generate();
    }


    @Override
    public Optional<String> generateRelated(IChatMessage message) {
        String text = message.getText();
        for (String word : samsonBotWords) {
            text = text.replaceAll(word, "");
        }
        String finalText = text.replaceAll(" {2}", " ");

        Optional<String> optionalAction = actionGenerator.generateRelated(finalText);
        return optionalAction.map(action -> {
                    ITextGeneratorContext register = createGenerationContext(action);

                    SentenceUtil.splitOnSpaces(finalText)
                            .filter(e -> !TwitterUtil.isTwitterWord(e))
                            .filter(e -> samsonBotWords.stream().noneMatch(e::matches))
                            .map(SentenceUtil::removeNonLetters)
                            .max(Comparator.comparingInt(String::length))
                            .ifPresent(longWord -> register.createGenerator("langstewoord", longWord));
                    return toespraakTemplatedGenerator.generate("reply", register);
                }
        );
    }
}
