package be.thomaswinters.samson.burgemeester;

import be.thomaswinters.samson.burgemeester.util.ActionNegatorCommand;
import be.thomaswinters.textgeneration.domain.factories.command.CommandFactory;
import be.thomaswinters.textgeneration.domain.factories.command.SingleTextGeneratorArgumentCommandFactory;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.parsers.DeclarationsFileParser;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class BurgemeesterBotLoader {

    public BurgemeesterBot build() throws IOException {
        URL templateFile = ClassLoader.getSystemResource("templates/toespraak.decl");
        List<CommandFactory> customCommands =
                Arrays.asList(
                        new SingleTextGeneratorArgumentCommandFactory("negate",
                                generator -> new ActionNegatorCommand(generator)));

        ITextGenerator generator = DeclarationsFileParser.createTemplatedGenerator(templateFile, customCommands);

        return new BurgemeesterBot(generator);


    }
}
