package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.wikihow.WikiHowPageScraper;

import java.io.IOException;

public class BurgemeesterBot {

    private WikiHowPageScraper wikiHow = new WikiHowPageScraper();

    private String getRandomAction() throws IOException {
        return wikiHow.scrapeRandomCard("nl").getTitle().toLowerCase();
    }

    public String createRandomToespraak() throws IOException {
        String action = getRandomAction();
        return "Aan allen die " + action + ", proficiat. Aan allen die niet " + action + ", ook profiat.";
    }


    public static void main(String[] args) throws IOException {
        System.out.println(new BurgemeesterBot().createRandomToespraak());
    }

}
