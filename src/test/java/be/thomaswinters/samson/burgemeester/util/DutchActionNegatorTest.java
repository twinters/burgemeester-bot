package be.thomaswinters.samson.burgemeester.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DutchActionNegatorTest {

    private DutchActionNegator negator;

    @BeforeEach
    public void setup() {
        negator = new DutchActionNegator();
    }

    @Test
    public void divergent_antonym_negation() throws IOException, ExecutionException {
        assertEquals("convergent leren denken", negator.negateAction("divergent leren denken").get());
    }


}
