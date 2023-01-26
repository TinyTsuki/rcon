package xin.vanilla.rcon;

import xin.vanilla.rcon.util.RconServerSimulator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RconBuilderTests {

    @Test
    void testRequiresChannel() {
        final RconBuilder builder = new RconBuilder();
        assertThrows(NullPointerException.class, builder::build);
        builder.withChannel(new RconServerSimulator());
        Assertions.assertDoesNotThrow(builder::build);
    }

    @Test
    void setBufferCapacity() {
        final RconBuilder builder = new RconBuilder()
                .withChannel(new RconServerSimulator())
                .withReadBufferCapacity(100)
                .withWriteBufferCapacity(100);

        Assertions.assertDoesNotThrow(builder::build);
    }
}
