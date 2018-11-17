package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.generator.generators.GeneratorCombiner;
import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.generator.generators.related.IRelatedGenerator;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.language.dutch.DutchSentenceSubjectReplacer;
import be.thomaswinters.language.stringmorpher.Decapitaliser;
import be.thomaswinters.samson.burgemeester.generators.ActionGeneratorBuilder;
import be.thomaswinters.samson.burgemeester.generators.NewsActionGenerator;
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
    private final NewsActionGenerator newsActionGenerator = new NewsActionGenerator();
    private final IGenerator<String> randomToespraakGenerator =
            actionGenerator.updateGenerator(
                    generator ->
                            GeneratorCombiner
                                    .fromGenerators(
                                            generator
                                                    .filter(title -> !title.startsWith("tips")),
                                            newsActionGenerator)
                                    .filter(10, title -> !SentenceUtil.containsCapitalisedLetters(title))
                                    .map(this::createToespraakForAction)
                                    .select(8,
                                            new RouletteWheelSelection<>(this::getToespraakFitness)));

    //region Toespraak Fixer

    public BurgemeesterBot(DeclarationFileTextGenerator toespraakGenerator) throws IOException {
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
            return subjectReplacer.secondPersonToThirdPerson(input);
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
    public Optional<String> generate(IChatMessage message) {
        String text = message.getText();
        for (String word : samsonBotWords) {
            text = text.replaceAll(word, "");
        }
        String finalText = text.replaceAll(" {2}", " ");

        Optional<String> optionalAction = actionGenerator.generate(finalText);
        return optionalAction.map(action -> {
                    ITextGeneratorContext register = createGenerationContext(action);

                    System.out.println("Adding longest word");
                    SentenceUtil.splitOnSpaces(finalText)
                            .filter(e -> !TwitterUtil.isTwitterWord(e))
                            .filter(e -> samsonBotWords.stream().noneMatch(e::matches))
                            .map(SentenceUtil::removeNonLetters)
                            .max(Comparator.comparingInt(String::length))
                            .ifPresent(longWord -> register.createGenerator("langstewoord", longWord));
                    System.out.println("Generating using template");
                    return toespraakTemplatedGenerator.generate("reply", register);
                }
        );
    }
}
