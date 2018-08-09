package be.thomaswinters.samson.burgemeester.generators;

import be.thomaswinters.action.ActionExtractor;
import be.thomaswinters.action.data.ActionDescription;
import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.newsminer.INewsRetriever;
import be.thomaswinters.newsminer.data.IArticle;
import be.thomaswinters.newsminer.dutch.VrtNwsRetriever;
import be.thomaswinters.random.Picker;
import be.thomaswinters.sentence.SentenceUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class NewsActionGenerator implements IGenerator<String> {

    private final ActionExtractor actionExtractor = new ActionExtractor(false);
    private final INewsRetriever newsRetriever = new VrtNwsRetriever();

    public NewsActionGenerator() throws IOException {
    }

    @Override
    public Optional<String> generate() {
        try {
            Collection<IArticle> articles = newsRetriever.retrieveArticles();
            Collection<ActionDescription> actions = articles.stream()
                    .flatMap(e -> {
                        try {
                            return actionExtractor.extractAction(e.getHeadline()).stream();
                        } catch (IOException e1) {
                            throw new RuntimeException(e1);
                        }
                    })
                    .filter(this::isValidAction)
                    .collect(Collectors.toList());
            System.out.println("Actions:\n"
                    + actions.stream().map(ActionDescription::getAsText).collect(Collectors.joining("\n"))
                    + "\n\n");

            return Picker.pickOptional(actions).map(ActionDescription::getAsText);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private boolean isValidAction(ActionDescription actionDescription) {
        String sentence = actionDescription.getRestOfSentence().toLowerCase();
        return !sentence.contains("moet")
                && !sentence.startsWith("hoe ")
                && !sentence.startsWith("wat ")
                && !sentence.startsWith("wie ")
                && !SentenceUtil.getWords(sentence).contains("u")
                ;

    }
}
