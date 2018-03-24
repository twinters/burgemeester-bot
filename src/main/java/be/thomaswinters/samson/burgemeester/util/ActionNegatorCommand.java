package be.thomaswinters.samson.burgemeester.util;

import be.thomaswinters.textgeneration.domain.context.ITextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.generators.commands.SingleGeneratorArgumentCommand;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ActionNegatorCommand extends SingleGeneratorArgumentCommand {

    private final DutchActionNegator actionNegator = new DutchActionNegator();

    public ActionNegatorCommand(ITextGenerator generator) {
        super(generator);
    }


    @Override
    public String apply(String string, ITextGeneratorContext parameters) {
        try {
            return actionNegator.negateAction(string).get();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return "negate";
    }
}
