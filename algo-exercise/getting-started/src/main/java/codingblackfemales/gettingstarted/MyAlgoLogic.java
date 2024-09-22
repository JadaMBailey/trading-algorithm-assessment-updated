package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        final BidLevel nearTouch = state.getBidAt(0); // Highest buy price
        AskLevel fTouch = state.getAskAt(0); // Lowest sell price
        long price = (nearTouch.price - (fTouch.price - nearTouch.price)); // JB: 1) workout spread 2) Take away from the highest bid price


        /********
         *
         * Add your logic here....
         *
         */

        return NoAction.NoAction;
    }
}
/*
 ## Tips for my Passive Algo ##
        - Need to add a condition to cancel existing order if price moves away from current market; Would need to set a range
        - Want to make the price in range
        - Would be ideal to calculate quantity to be similar to others in orderbook, to minimise market impact on pricing
 */