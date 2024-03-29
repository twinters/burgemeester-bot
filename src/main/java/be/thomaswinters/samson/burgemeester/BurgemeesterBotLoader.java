package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.samson.burgemeester.util.ActionNegatorCommand;
import be.thomaswinters.textgeneration.domain.factories.command.CommandFactory;
import be.thomaswinters.textgeneration.domain.factories.command.SingleTextGeneratorArgumentCommandFactory;
import be.thomaswinters.textgeneration.domain.generators.databases.DeclarationFileTextGenerator;
import be.thomaswinters.textgeneration.domain.parsers.DeclarationsFileParser;
import be.thomaswinters.twitter.bot.BehaviourCreator;
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

        ITweetsFetcher tweetsToAnswer =
                TwitterBot.MENTIONS_RETRIEVER.apply(twitter)
//                        .combineWith(
//                                new SearchTweetsFetcher(twitter, Arrays.asList("burgemeester", "samson"))
//                                        .filterRandomlyIf(twitter, x -> true, 1, 3)
//                        )
                        .combineWith(
                                new AdvancedListTweetsFetcher(twitter, samsonBotsList, false, true)
                        )
                        // Filter out botfriends tweets randomly
                        .filterRandomlyIf(twitter, e -> botFriends.contains(e.getUser()), 1, 25)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        .filterOutOwnTweets(twitter);


        ITweetsFetcher tweetsToQuoteRetweet =
                TwitterBot.MENTIONS_RETRIEVER.apply(twitter)
//                        .combineWith(
//                                new SearchTweetsFetcher(twitter, Arrays.asList("burgemeester", "samson"))
//                                        .filterRandomlyIf(twitter, x -> true, 1, 3)
//                        )
                        .combineWith(
                                new AdvancedListTweetsFetcher(twitter, samsonBotsList, false, true)
                        )
                        // Filter out botfriends tweets randomly
                        .filterRandomlyIf(twitter, e -> botFriends.contains(e.getUser()), 1, 25)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        .filterOutOwnTweets(twitter);


        BurgemeesterBot burgemeesterBot = build();

        return new TwitterBot(twitter,
                BehaviourCreator.fromTextGenerator(burgemeesterBot),
                BehaviourCreator.fromMessageReactor(burgemeesterBot),
                tweetsToAnswer);
    }

}
