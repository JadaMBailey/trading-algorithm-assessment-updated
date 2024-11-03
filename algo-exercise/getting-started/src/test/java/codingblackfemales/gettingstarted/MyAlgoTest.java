package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * This test is designed to check your algo behavior in isolation of the order book.
 *
 * You can tick in market data messages by creating new versions of createTick() (ex. createTick2, createTickMore etc..)
 *
 * You should then add behaviour to your algo to respond to that market data by creating or cancelling child orders.
 *
 * When you are comfortable you algo does what you expect, then you can move on to creating the MyAlgoBackTest.
 *
 */
public class MyAlgoTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        return new MyStretchLogic();
    }


    @Test
    public void testDispatchThroughSequencer() throws Exception {
        send(createTick8am());
        send(createTick8_30am());
        send(createTick8am());
        send(createTick8am());
        //create a sample market data tick....

        //simple assert to check we had 3 orders created
        assertEquals(1, container.getState().getChildOrders().size());
    }

    @Test
    public void checkVwapFunction() throws Exception{

    }

    @Test
    public void checkOrderTracked() throws Exception{


    }

    @Test
    public void checkthatAnOrderCanBeCreated() throws Exception{
        send(createTick8am());
        send(createTick8_30am());
        send(createTick8am());
        send(createTick8am());
        // container variable accessible
        assertEquals(1,container.getState().getActiveChildOrders().size());
    }

    @Test
    public void lowAskQuantity() throws Exception{
        // I want to see that I am receiving the lowest quantity
        
    }

}
