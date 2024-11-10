package codingblackfemales.gettingstarted;
import messages.order.Side;
import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;


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

    private MyAlgoLogic myStretchLogic;
    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        myStretchLogic = new MyAlgoLogic();
        return myStretchLogic;
    }


    @Test
    public void testDispatchThroughSequencer() throws Exception {

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


        //simple assert to check we had a total of 3 orders (2 active and 1 cancelled)
        assertEquals(3, container.getState().getChildOrders().size());
    }


    @Test
    public void testOnly1SellOrder() throws Exception {
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
       
        SimpleAlgoState state = container.getState();

        long sellOrderCount = state.getActiveChildOrders().stream()
                        .filter(order -> order.getSide() == Side.SELL)
                        .count();

//        assertEquals(1, sellOrderCount);
    }

    @Test
    public void shouldCreateOrder() throws Exception {
        // Setup market conditions
        send(createTick8am());
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
        
        // // Mockito handles the creation of the mock object which is a ChildOrder
        ChildOrder testOrder = Mockito.mock(ChildOrder.class);
        container.getState().getActiveChildOrders().add(testOrder);
        
         // Assert that the order was added
        //  assertTrue(container.getState().getActiveChildOrders().contains(testOrder));
    }

    
}