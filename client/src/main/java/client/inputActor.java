package client;

import akka.actor.AbstractActor;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;


public class inputActor extends AbstractActor {
    private LineReader reader;
    private Terminal terminal;

    public inputActor(LineReader reader, Terminal terminal) {
        this.reader = reader;
        this.terminal = terminal;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .build();
    }


    @Override
    public void preStart() {
        reader.setOpt(LineReader.Option.ERASE_LINE_ON_FINISH);
        while(true) {
            String line = null;
            try {
                line = reader.readLine();
            } catch (UserInterruptException e) {
                // Ignore
            } catch (EndOfFileException e) {
                return;
            }
            if (line == null) {
                continue;
            }
            getContext().parent().tell(line, self());
        }

    }
}
