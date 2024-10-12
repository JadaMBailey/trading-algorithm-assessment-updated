package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoAddCancelLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoAddCancelLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        logger.info("[MYALGO] In Algo Logic....");

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        final BidLevel nearTouch = state.getBidAt(0); // Highest buy price - TopBook for BUY
        final AskLevel farTouch = state.getAskAt(0); // Lowest sell price - TopBook for ASK
        long targetQuantity = 300;


        if (state.getChildOrders().size() < 2) {
            if (state.getBidLevels() < 4) {
                long price = nearTouch.price;
                long quantity = targetQuantity / 4;
                logger.info("[MYALGO] Have:" + state.getChildOrders().size() + " children, want 4, joining passive side of book" +
                        " with: " + quantity + " @ " + price);
                return new CreateChildOrder(Side.BUY, quantity, price);
            }
        }
        final var activeOrders = state.getActiveChildOrders();
        if(activeOrders.size() > 3){
            final var option = activeOrders.stream().findFirst();
            if (option.isPresent()) {
                var childOrder = option.get();
                logger.info("[MYALGO] Cancelling high price order:" + childOrder);
                return new CancelChildOrder(childOrder);
            }
        }

        if (state.getChildOrders().size() < 4) {
            if (state.getBidLevels() < 5) {
                long price = (nearTouch.price - (farTouch.price - nearTouch.price));
                // JB: 1) workout spread 2) Take away from the highest bid price
                long quantity = targetQuantity / 2;
                logger.info("[MYALGO] Have:" + state.getChildOrders().size() + " children, want 4, joining passive side of book" +
                        " with: " + quantity + " @ " + price);
                return new CreateChildOrder(Side.BUY, quantity, price);
            }
        } else {
            logger.info("[MYALGO] Have:" + state.getChildOrders().size() + " children added, want 4, done.");
            return NoAction.NoAction;
        }
        return NoAction.NoAction;
    }
}




        /******** My Thoughts
         * Venue is set to 'XLON' which is a reference for the LSE market
         * Have chosen to go with a PassiveAlgo strategy
         * Have chosen 'FTSE 100' index to grab data for backtesting for two reasons
            * FTSE 100 is on the LSE market
            * In a real world situation FTSE 100 has a high liquidity
         * Add a cancel condition : As the task mentions Add and Cancel then the stretch being Buy and Sell, I can
           fulfil all the requirements without creating two separate algo
            * Problem: The only reason I can think of to cancel is based on how long the order has been on the book.
            #Todo Need to do more research into passive algo strategies
         * Need to also make the quantity dynamic to reduce market impact movement
         */



/*
 ## Tips for my Passive Algo ##
        - Need to add a condition to cancel existing order if price moves away from current market; Would need to set a range
        - Want to make the price in range
        - Would be ideal to calculate quantity to be similar to others in orderbook, to minimise market impact on pricing
 */