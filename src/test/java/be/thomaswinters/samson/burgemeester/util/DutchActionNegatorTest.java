package be.thomaswinters.samson.burgemeester.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class DutchActionNegatorTest {

    private DutchActionNegator negator;

    @Before
    public void setup() {
        negator = new DutchActionNegator();
    }

    @Test
    public void divergent_antonym_negation() throws IOException, ExecutionException {
        assertEquals("convergent leren denken", negator.negateAction("divergent leren denken").get());
    }


}
