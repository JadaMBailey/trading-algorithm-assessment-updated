package codingblackfemales.algo;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddCancelAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(AddCancelAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {
        // JB: SimpleAlgoState is an interface : Assume parent class
        logger.info("[JB:evaluate()][ADDCANCELALGO] In Algo Logic....");
        // Not sure
        final String book = Util.orderBookToString(state);

        logger.info("[JB:evaluate()] [ADDCANCELALGO] Algo Sees Book as:\n" + book);

        var totalOrderCount = state.getChildOrders().size();
        // JB: would change 'var' to be specific to increase speed I believe to int
        // JB: Assume you set child orders to sit, then create method to move them into 'activeChild' list.

        //make sure we have an exit condition...
        if (totalOrderCount > 20) {
            return NoAction.NoAction;
            // JB: this does nothing, assume 'No action' included as the method must return something

        }

        final var activeOrders = state.getActiveChildOrders();
        // JB: why is this final, if active child orders will change?
        if (activeOrders.size() > 0) {

            final var option = activeOrders.stream().findFirst();
            // child order option
            // JB: Need to look into this block of why it is cancelling child order
            // JB: He has used a nested if condition - think why?
            if (option.isPresent()) {
                var childOrder = option.get();
                logger.info("[JB:evaluate()] [ADDCANCELALGO] Cancelling order:" + childOrder);
                return new CancelChildOrder(childOrder);
            } else {
                return NoAction.NoAction;
                // Nothing is being done with this code as there is no child orders
            }
        } else {
            BidLevel level = state.getBidAt(0);
            final long price = level.price;
            final long quantity = level.quantity;
            logger.info("[JB:evaluate()] [ADDCANCELALGO] Adding order for" + quantity + "@" + price); // Expect to change ref 'ADDCANCELALGO' to 'MYALGO'
            return new CreateChildOrder(Side.BUY, quantity, price);
            // jb: Need to find what BUY constant is
            // Finds the highest buyers quantity price
        }
    }
    }

    /*
    So currently, I believe it is doing if you have active child orders in list stored, find the fist child order in that list. Then if it is there, get child order (assume 'get()'' returns child order; logs this transaction / movement; creates object and passes detail through 'childOrder' variable.
        Else not present, do nothing

       Outer Else: no child orders = true
       active child order to buy at '0'; store price and quantity of active child order;
       log price and quantity details;
       Create a new object of child order and pass in quantity, price and BUY constant (Not sure what that actually means?)

     */

/*
    ### Current Understanding of Flow ###
    1) Loads the first 7 orders found in AddCancelBackTest onto the OrderBook
    2) Creates a Buy order of the highest bid order
    3) Cancels that order
    4) Repeats

    ### Questions
    1) Have no idea where it is loading the orders to the Orderbook
    2) When I switch the marketTicks around in terms of in the order it appears in the testExampleBackTest() the following occurs
        - extra log lines appear
        - Matches the first two Buy orders against the 501@98 ask order. But only impacts the quantity of 105 buy order
        - Removes the two ask orders further on, have no idea why.
 */