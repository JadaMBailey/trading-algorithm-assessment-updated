package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This test plugs together all of the infrastructure, including the order book (which you can trade against)
 * and the market data feed.
 *
 * If your algo adds orders to the book, they will reflect in your market data coming back from the order book.
 *
 * If you cross the srpead (i.e. you BUY an order with a price which is == or > askPrice()) you will match, and receive
 * a fill back into your order from the order book (visible from the algo in the childOrders of the state object.
 *
 * If you cancel the order your child order will show the order status as cancelled in the childOrders of the state object.
 *
 */
public class MyBackTest extends AbstractAlgoBackTest {
    private MyAlgoLogic myStretchLogic; // Added class variable - 1
    @Override
    public AlgoLogic createAlgoLogic() {
        myStretchLogic = new MyAlgoLogic(); // Assigned variable to create an object of 'MyStretchLogic' class - 2
        return myStretchLogic; // Changed to return the variable instead of new object which was there originally - 3
    }

    @Test
    public void testBackTest() throws Exception {
        //then: get the state
        var state = container.getState();

        //create a sample market data tick....
        send(createTick8am()); //1
        send(createTick8_30am()); //2
        send(createTick9am()); //3
        send(createTick9_30am()); //4
        send(createTick10am()); //5
        send(createTick10_30am()); //6
        send(createTick11am()); //7
        send(createTick11_30am()); //8
        send(createTick12pm()); //9
        send(createTick12_30pm()); //10
        send(createTick1pm()); //11
        send(createTick1_30pm()); //12
        send(createTick2pm()); //13
        send(createTick2_30pm()); //14
        send(createTick3pm()); //15
        send(createTick3_30pm()); //16
        send(createTick4pm()); //17

        myStretchLogic.postTradeAnalysis(state); // Call post trade analysis method after all market data ticks have been actioned. - 4












//        assertEquals(5, container.getState().getChildOrders().size());
//        assertEquals(3, container.getState().getActiveChildOrders().size());

        //Check things like filled quantity, cancelled order count etc....
//        long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
//        //and: check that our algo state was updated to reflect our fills when the market data
//        assertEquals(400, filledQuantity);
    }

}
