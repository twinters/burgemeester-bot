package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.samson.burgemeester.util.ActionNegatorCommand;
import be.thomaswinters.textgeneration.domain.factories.command.CommandFactory;
import be.thomaswinters.textgeneration.domain.factories.command.SingleTextGeneratorArgumentCommandFactory;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.parsers.DeclarationsFileParser;
import be.thomaswinters.twitter.bot.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.tweetsfetcher.SearchTweetsFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BurgemeesterBotLoader {

    public BurgemeesterBot build() throws IOException {
        URL templateFile = ClassLoader.getSystemResource("templates/toespraak.decl");
        List<CommandFactory> customCommands =
                Collections.singletonList(
                        new SingleTextGeneratorArgumentCommandFactory("negate",
                                ActionNegatorCommand::new));

        ITextGenerator generator = DeclarationsFileParser.createTemplatedGenerator(templateFile, customCommands);

        return new BurgemeesterBot(generator);


    }

    public TwitterBot buildTwitterBot() throws IOException {
        BurgemeesterBot burgemeesterBot = build();
        return new GeneratorTwitterBot(TwitterLogin.getTwitterFromEnvironment(),
                burgemeesterBot, burgemeesterBot,
                TwitterBot.MENTIONS_RETRIEVER,
                twitter -> new SearchTweetsFetcher(twitter, Arrays.asList("burgemeester", "samson")));
    }

}
