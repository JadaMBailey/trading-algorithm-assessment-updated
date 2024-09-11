package codingblackfemales.sequencer.net;

import org.agrona.DirectBuffer;

import java.util.LinkedList;
import java.util.List;

public class TestNetwork implements Network{

    private final List<Consumer> consumers = new LinkedList<>();
    /*
     JB : buffer variable passes some type of message relevant to each event (Example below)
        A market data update (e.g., prices going up or down).
        An order being created or canceled.
        Order fills (when your order matches a counterpart's).
     */
    @Override
    public void dispatch(DirectBuffer buffer){
        for (Consumer consumer: consumers) {
            /*
            JB: a consumer is either order book, logging service or trading logic
            All consumers do something different with the message

             */
            consumer.onMessage(buffer); // Need to step into this line #Todo
        }
    }

    public void addConsumer(Consumer consumer){
        consumers.add(consumer);
    }

}
/*
## JB Notes
    Responsible for sending out the message.
 */