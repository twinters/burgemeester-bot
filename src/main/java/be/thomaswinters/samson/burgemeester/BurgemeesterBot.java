package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.language.SubjectType;
import be.thomaswinters.language.dutch.DutchActionNegator;
import be.thomaswinters.language.dutch.DutchSentenceSubjectReplacer;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import stringmorpher.Decapitaliser;

import java.io.IOException;
import java.util.Optional;

public class BurgemeesterBot {

    private WikiHowPageScraper wikiHow = new WikiHowPageScraper();
    private DutchSentenceSubjectReplacer subjectReplacer = new DutchSentenceSubjectReplacer();
    private DutchActionNegator negator = new DutchActionNegator();


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
        return !containsCapitalisedLetters(title);
    }

    private boolean containsCapitalisedLetters(String input) {
        return !input.toLowerCase().equals(input);

    }

    private String negateAction(String input) {
        try {
            Optional<String> result = negator.negateAction(input);
            if (result.isPresent()) {
                return result.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("No negation of '" + input + "' found!");
    }


    public String createRandomToespraak() throws IOException {
        String action = getRandomAction();
        return "Aan allen die " + action + ": proficiat.\nAan allen die " + negateAction(action) + ": ook profiat.";
    }


    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 1000; i++) {
            System.out.println(new BurgemeesterBot().createRandomToespraak() + "\n\n");

        }
    }

}
