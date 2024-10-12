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

public class MyStretchLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyStretchLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        logger.info("[MYALGO] In Algo Logic....");

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        final BidLevel nearTouch = state.getBidAt(0); // Highest buy price - TopBook for BUY
        final AskLevel farTouch = state.getAskAt(0); // Lowest sell price - TopBook for ASK
        long targetBidQuantity = 0;
        long targetAskQuantity = 0;
        int buy;
        int ask;
        for (buy = 0; buy < state.getBidLevels(); buy++){
          var checkingQuantity = state.getBidAt(buy).getQuantity();
            logger.info("[MY-STRETCH-ALGO] In Algo Logic AND CHECKING FOR LOOP CONDITION " + checkingQuantity);
            targetBidQuantity += checkingQuantity;
            logger.info("[MY-STRETCH-ALGO] In Algo Logic AND Checking sum of quantity " + targetBidQuantity);
        }

        for (ask = 0; ask < state.getAskLevels(); ask++){
           var checkingAskQunatity = state.getAskAt(ask).getQuantity();
            logger.info("[MY-STRETCH-ALGO] In Algo Logic AND CHECKING FOR LOOP CONDITION " + checkingAskQunatity);
            targetAskQuantity += checkingAskQunatity;
            logger.info("[MY-STRETCH-ALGO] In Algo Logic AND Checking sum of quantity " + targetAskQuantity);
        }


        if (state.getChildOrders().size() < 2) {
            if (state.getBidLevels() < 4) {
                long price = (nearTouch.price - (farTouch.price - nearTouch.price));
                // JB: 1) workout spread 2) Take away from the highest bid price
                long quantity = targetBidQuantity / state.getBidLevels();
                logger.info("[MY-STRETCH-ALGO] Have:" + state.getChildOrders().size() + " children, want 3, joining passive side of book on buy side" +
                        " with: " + quantity + " @ " + price);
                return new CreateChildOrder(Side.BUY, quantity, price);
            }
        }
        if (state.getChildOrders().size() < 3) {
            if (state.getBidLevels() < 5) {
                long price = (farTouch.price + (farTouch.price - nearTouch.price));
                // JB: 1) workout spread 2) Take away from the highest bid price
                long quantity = targetAskQuantity / state.getAskLevels();
                logger.info("[MY-STRETCH-ALGO] Have:" + state.getChildOrders().size() + " children, want 3, joining passive side of book on ASK side" +
                        " with: " + quantity + " @ " + price);
                return new CreateChildOrder(Side.SELL, quantity, price);
            }
        } else {
            logger.info("[MYALGO] Have:" + state.getChildOrders().size() + " children added, want 3, done.");
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