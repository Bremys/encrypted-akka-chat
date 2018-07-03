package client;

import akka.actor.AbstractActor;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

public class outputActor extends AbstractActor {
    private LineReader reader;
    private Terminal terminal;

    public outputActor(LineReader reader, Terminal terminal) {
        this.reader = reader;
        this.terminal = terminal;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, m -> {
                    reader.callWidget(LineReader.CLEAR);
                    reader.getTerminal().writer().print(m);
                    reader.callWidget(LineReader.REDRAW_LINE);
                    reader.callWidget(LineReader.REDISPLAY);
                    reader.getTerminal().writer().flush();
                })
                .build();
    }
}
