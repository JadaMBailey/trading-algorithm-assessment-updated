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
public class MyAlgoAddCancelBackTest extends AbstractAlgoBackTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoAddCancelLogic();
    }

    @Test
    public void testExampleBackTest() throws Exception {
        //create a sample market data tick....
        send(createTick8am());

        //ADD asserts when you have implemented your algo logic
//        assertEquals(4, container.getState().getChildOrders().size());

        //when: market data moves towards us
        send(createTick8_30am());

        //then: get the state
        var state = container.getState();

        send(createTick9am());
        send(createTick9_30am());

        //Check things like filled quantity, cancelled order count etc....
//        long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
//        //and: check that our algo state was updated to reflect our fills when the market data
//        assertEquals(450, filledQuantity);
    }

}
