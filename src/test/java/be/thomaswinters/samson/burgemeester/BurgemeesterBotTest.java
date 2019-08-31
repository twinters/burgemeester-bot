package be.thomaswinters.samson.burgemeester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BurgemeesterBotTest {

    private BurgemeesterBot burgemeesterBot;

    @BeforeEach
    public void setup() throws IOException {
        this.burgemeesterBot = new BurgemeesterBotLoader().build();
    }

    @Test
    void test_action_generator_no_wiki_metadata() {
        Optional<String> action = this.burgemeesterBot.getActionGenerator().generate("fluiten");
        System.out.println(action);
        assertTrue(action.isPresent());
        assertFalse(action.get().contains("keer bekeken"));
    }
}