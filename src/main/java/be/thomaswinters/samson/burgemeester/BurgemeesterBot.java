package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.bot.IChatBot;
import be.thomaswinters.bot.ITextGeneratorBot;
import be.thomaswinters.bot.data.IChatMessage;
import be.thomaswinters.language.SubjectType;
import be.thomaswinters.language.dutch.DutchSentenceSubjectReplacer;
import be.thomaswinters.language.stringmorpher.Decapitaliser;
import be.thomaswinters.samson.burgemeester.util.DutchActionNegator;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.textgeneration.domain.constraints.LockConstraint;
import be.thomaswinters.textgeneration.domain.context.ITextGeneratorContext;
import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.generators.StaticTextGenerator;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import be.thomaswinters.twitter.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.TwitterBotExecutor;
import be.thomaswinters.twitter.util.TwitterUtil;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import be.thomaswinters.wikihow.WikihowLoginCookieCreator;
import be.thomaswinters.wikihow.WikihowSearcher;
import be.thomaswinters.wikihow.data.PageCard;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class BurgemeesterBot implements ITextGeneratorBot, IChatBot {

    private static final String NEDERLANDS = "nl";
    private WikiHowPageScraper wikiHow = new WikiHowPageScraper(NEDERLANDS);
    private WikihowSearcher wikiHowSearcher;

    private DutchSentenceSubjectReplacer subjectReplacer = new DutchSentenceSubjectReplacer();
    private DutchActionNegator negator = new DutchActionNegator();
    private final ITextGenerator toespraakGenerator;

    public BurgemeesterBot(ITextGenerator toespraakGenerator) {
        this.toespraakGenerator = toespraakGenerator;

        this.wikiHowSearcher = createWikihowSearcher();
    }

    private WikihowSearcher createWikihowSearcher() {
        String cookie = null;
        try {
            if (System.getenv("wikihow_user") != null && System.getenv("wikihow_password") != null) {
                cookie = new WikihowLoginCookieCreator().login(
                        System.getenv("wikihow_user"),
                        System.getenv("wikihow_password"));
            } else {
                System.out.println("No wikihow login specified");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new WikihowSearcher(NEDERLANDS, cookie);
    }

    private String getRandomAction() throws IOException {
        String randomAction = null;
        do {
            randomAction = wikiHow.scrapeRandomCard().getTitle();
            randomAction = Decapitaliser.decapitaliseFirstLetter(randomAction);
        } while (!isValidAction(randomAction));

        return fixAction(randomAction);
    }

    private Optional<String> getActionRelevantTo(String message) {
        List<String> searchWords = SentenceUtil.splitOnSpaces(message)
                .filter(e -> !TwitterUtil.isTwitterWord(e))
                .map(SentenceUtil::removePunctuations)
                .filter(SentenceUtil::hasOnlyLetters)
                .collect(Collectors.toList());

        System.out.println("Searching on WikiHow for: " + searchWords);

        List<PageCard> pages = new ArrayList<>();
        while (pages.isEmpty() && !searchWords.isEmpty()) {
            try {
                pages = wikiHowSearcher.search(searchWords);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (pages.isEmpty()) {
                searchWords = removeShortestWords(searchWords);
            }
        }

        return getFirstAction(pages);
    }

    private List<String> removeShortestWords(List<String> searchWords) {
        OptionalInt lowestAmountOfLetters = searchWords.stream().mapToInt(String::length).min();
        if (lowestAmountOfLetters.isPresent()) {
            searchWords = searchWords.stream()
                    .filter(e -> e.length() > lowestAmountOfLetters.getAsInt())
                    .collect(Collectors.toList());
        }
        return searchWords;


    }

    private Optional<String> getFirstAction(List<PageCard> pages) {
        return pages.stream()
                .map(PageCard::getTitle)
                .map(Decapitaliser::decapitaliseFirstLetter)
                .map(this::tryFixingAction)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    // FIXERS
    private Optional<String> tryFixingAction(String action) {
        try {
            return Optional.of(fixAction(action));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

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

    public String createToespraak(String action) throws IOException {
        return toespraakGenerator.generate(createGenerationContext(action));

//        return "Aan allen die " + action + ": proficiat.\nAan allen die " + negateAction(action) + ": ook proficiat.";
    }

    @Override
    public Optional<String> generateText() {
        try {
            return Optional.of(createToespraak(getRandomAction()));
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


    @Override
    public Optional<String> generateReply(IChatMessage message) {
        try {


            Optional<String> relevantAction = getActionRelevantTo(message.getMessage());

            if (relevantAction.isPresent()) {
                return Optional.of(createToespraak(relevantAction.get()));
            }

        } catch (
                IOException e)

        {
            e.printStackTrace();
        }
        return Optional.empty();

    }
}
