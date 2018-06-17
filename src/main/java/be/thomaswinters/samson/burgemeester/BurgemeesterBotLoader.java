package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.samson.burgemeester.util.ActionNegatorCommand;
import be.thomaswinters.textgeneration.domain.factories.command.CommandFactory;
import be.thomaswinters.textgeneration.domain.factories.command.SingleTextGeneratorArgumentCommandFactory;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.generators.databases.DeclarationFileTextGenerator;
import be.thomaswinters.textgeneration.domain.parsers.DeclarationsFileParser;
import be.thomaswinters.twitter.bot.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.tweetsfetcher.AdvancedListTweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.ITweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.SearchTweetsFetcher;
import be.thomaswinters.twitter.userfetcher.ListUserFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import twitter4j.Twitter;
import twitter4j.User;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class BurgemeesterBotLoader {

    public BurgemeesterBot build() throws IOException {
        URL templateFile = ClassLoader.getSystemResource("templates/toespraak.decl");
        List<CommandFactory> customCommands =
                Collections.singletonList(
                        new SingleTextGeneratorArgumentCommandFactory("negate",
                                ActionNegatorCommand::new));

        DeclarationFileTextGenerator generator =
                DeclarationsFileParser.createTemplatedGenerator(templateFile, customCommands);

        return new BurgemeesterBot(generator);


    }

    public TwitterBot buildTwitterBot() throws IOException {

        Twitter twitter = TwitterLogin.getTwitterFromEnvironment("burgemeester.");

        long samsonBotsList = 1006565134796500992L;
        Collection<User> botFriends = ListUserFetcher.getUsers(twitter, samsonBotsList);

        Function<Twitter, ITweetsFetcher> tweetsToAnswer =
                twit ->
                        TwitterBot.MENTIONS_RETRIEVER.apply(twit)
                                .combineWith(
                                        new SearchTweetsFetcher(twitter, Arrays.asList("burgemeester", "samson"))
                                                .filterRandomlyIf(twit, x -> true, 1, 3)
                                )
                                .combineWith(
                                        new AdvancedListTweetsFetcher(twit, samsonBotsList, false, true)
                                )
                                // Filter out botfriends tweets randomly
                                .filterRandomlyIf(twit, e -> botFriends.contains(e.getUser()), 1, 10)
                                // Filter out own tweets & retweets
                                .filterOutRetweets()
                                .filterOutOwnTweets(twitter);


        BurgemeesterBot burgemeesterBot = build();

        return new GeneratorTwitterBot(twitter,
                burgemeesterBot, burgemeesterBot,
                tweetsToAnswer);
    }

}
