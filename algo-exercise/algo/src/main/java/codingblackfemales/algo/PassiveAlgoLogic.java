package codingblackfemales.algo;

import codingblackfemales.action.Action;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static codingblackfemales.action.NoAction.NoAction;

public class PassiveAlgoLogic implements AlgoLogic{

    private static final Logger logger = LoggerFactory.getLogger(PassiveAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        logger.info("[PASSIVEALGO] In Algo Logic....");

        final String book = Util.orderBookToString(state);

        logger.info("[PASSIVEALGO] Algo Sees Book as:\n" + book);

        final BidLevel nearTouch = state.getBidAt(0);

        long quantity = 75;
        long price = nearTouch.price; // JB: best bid for buy order

        //until we have three child orders....
        if(state.getChildOrders().size() < 3){
            //then keep creating a new one
            logger.info("[PASSIVEALGO] Have:" + state.getChildOrders().size() + " children, want 3, joining passive side of book with: " + quantity + " @ " + price);
            return new CreateChildOrder(Side.BUY, quantity, price);
            // JB: need to look into alternative ways of creating object to reduce need of fetching from Heap memory
        }else{
            logger.info("[PASSIVEALGO] Have:" + state.getChildOrders().size() + " children, want 3, done.");
            return NoAction;
        }
        /*
        Passive Orders
        - typically placed away from the current market price (to avoid immediate execution)
        - [Benefit] : Reduce market impact and costs, as it is a liquidity provider
        -Typical algo: higher price for sells and lower price for bids (think best case scenario for waiting so long)
        -



        Assumptions to check
        #Todo : I believe this algo is a price/time priority
         */

    }
}
