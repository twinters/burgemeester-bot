package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.language.DutchSentenceSubjectReplacer;
import be.thomaswinters.language.SubjectType;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import stringmorpher.Decapitaliser;

import java.io.IOException;

public class BurgemeesterBot {

    private WikiHowPageScraper wikiHow = new WikiHowPageScraper();
    private DutchSentenceSubjectReplacer subjectReplacer = new DutchSentenceSubjectReplacer();

    private String getRandomAction() throws IOException {
        String randomAction = null;
        do {
            if (randomAction != null) {
                System.out.println("Rejected: " + randomAction);
            }
            randomAction = wikiHow.scrapeRandomCard("nl").getTitle().toLowerCase();
            randomAction = Decapitaliser.decapitaliseFirstLetter(randomAction);
        } while (!isValidAction(randomAction));

        return fixAction(randomAction);
    }

    private String fixAction(String randomAction) throws IOException {
        return subjectReplacer.replaceSecondPerson(randomAction, "zij", "hun", "hen", "zichzelf", SubjectType.THIRD_SINGULAR);
    }

    private boolean isValidAction(String title) {
        return !containsCapitalisedLetters(title);
    }

    private boolean containsCapitalisedLetters(String input) {
        return !input.toLowerCase().equals(input);

    }

    public String createRandomToespraak() throws IOException {
        String action = getRandomAction();
        return "Aan allen die " + action + ", proficiat.\nAan allen die niet " + action + ", ook profiat.";
    }


    public static void main(String[] args) throws IOException {
        System.out.println(new BurgemeesterBot().createRandomToespraak());
    }

}
