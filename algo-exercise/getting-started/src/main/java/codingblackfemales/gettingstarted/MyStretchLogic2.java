package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MyStretchLogic2 implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyStretchLogic.class);
    private final Map<Long, Integer> orderIterationCount = new HashMap<>();
    long comparisonVar = Long.MAX_VALUE;

    @Override
    public Action evaluate(SimpleAlgoState state) {

        logger.info("[MY-STRETCH-ALGO] In Algo Logic....");

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MY-STRETCH-ALGO] The state of the order book is:\n" + orderBookAsString);

        final BidLevel nearTouch = state.getBidAt(0);
        final AskLevel farTouch = state.getAskAt(0);
        final long spreadPrice = farTouch.price - nearTouch.price;

          /*
         ### Condition set for cancelling child orders ###
         - Placed up top, so new child orders created doesn't get cancelled automatically
          Stream<ChildOrder> checkActiveO = state.getActiveChildOrders().stream();// Able to manipulate the orders without impacting the existing active order list
            - checkActiveO.forEach(a -> logger.info("This is my activeOrder output: " + a));
         */

        for (ChildOrder order : state.getActiveChildOrders()) // Checking active orders are still relevant to be on the book
        {
//            // Need to fix the cancelling Logic in 'MyStretchLogic' before adding here
            long orderId = order.getOrderId();
            int currentCount;
            int cancelLimit = 5;
            if (!orderIterationCount.containsKey(orderId) && !(order.getQuantity() == order.getFilledQuantity())) // If the order ID is not currently in HashMap list (-> Go into block)
            {
                orderIterationCount.put(orderId, 1); // Start tracking the order
                logger.info("[MY-STRETCH-ALGO] Tracking new order: " + order + " with ID: " + orderId); // Only one order is going through here - problem to investigate
            }
            else {
                // Increment the count for the existing order
                currentCount = orderIterationCount.get(orderId);
                orderIterationCount.put(orderId, currentCount + 1);
                logger.info("[MY-STRETCH-ALGO] Order ID: " + orderId + " has been active for " + currentCount + " iterations.");
//
//                // ### Cancel order after 5 iteriations ### #Todo - Problem here
//                if (currentCount >= cancelLimit) {
//                    logger.info("[MY-STRETCH-ALGO] Cancelling order after 5 iterations: " + order);
////                    orderIterationCount.entrySet().stream().map(entry -> " Order Id: " + entry.getKey() + " | Duration within OrderBook: " + (entry.getValue()));
//                    orderIterationCount.remove(orderId); // Remove order from tracking after canceling
//                    state.getActiveChildOrders().remove(orderId);
//                    return new CancelChildOrder(order);  // Cancel the order after 5 iterations
//                }
            }
            // Cancel BUY order if the market moves after from sitting order
            if (order.getSide() == Side.BUY && order.getPrice() < minBuyBookPrice(state) ){
                logger.info("[MY-STRETCH-ALGO] Cancelling BUY order as it is out of range with new market update: " + order);
                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                state.getActiveChildOrders().remove(orderId);
                return new CancelChildOrder(order);
            } // Cancel SELL order if market moves after from sitting order
            else if (order.getSide() == Side.SELL && order.getPrice() > maxAskBookPrice(state)) {
                logger.info("[MY-STRETCH-ALGO] Cancelling ASK order as it is out of range with new market update: " + order);
                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                state.getActiveChildOrders().remove(orderId);
                return new CancelChildOrder(order);
            }
        }

            // ### Creating Child Orders ###
            if (spreadPrice > 3) { // creating less competitive Buy orders
                long buyOrderCount = state.getActiveChildOrders().stream().filter(order -> order.getSide() == Side.BUY).count();
                if ( buyOrderCount < 2) {
                    {
                        long price = (nearTouch.price - 2);// Want to be passive order, due to less competitive scenario
                        long initialQuantity = 250;
                        long chosenQuantity = lowestAskQOnBook(state);
                        long quantity = Long.min(initialQuantity, chosenQuantity); // might have a large quantity from ask side, that isn't feasible for the client.
                        logger.info("[MY-STRETCH-ALGO] Have:" + state.getActiveChildOrders().size() +
                                " children, want 3, joining BUY side of book" +
                                " with: " + quantity + " @ " + price);
                        return new CreateChildOrder(Side.BUY, quantity, price);
                    }
                }
            }

            if (spreadPrice < 2)
            { // Short position is risky, better to place order when book looks to be narrow
                long sellOrderCount = state.getActiveChildOrders().stream()
                        .filter(order -> order.getSide() == Side.SELL)
                        .count();
                if (sellOrderCount < 1) {
                    long price = farTouch.price - 1;
                    long quantity = 200;

                    logger.info("[MY-STRETCH-ALGO] Have:" + state.getActiveChildOrders().size()
                            + " children, want 3, joining passive side of book on ASK side" +
                            " with: " + quantity + " @ " + price);
                    return new CreateChildOrder(Side.SELL, quantity, price);
                }
            } else if (spreadPrice < 3){
                // Creating Aggressive BUY order
                long buyOrderCount = state.getActiveChildOrders().stream().filter(order -> order.getSide() == Side.BUY).count();
                    if ( buyOrderCount < 1) {
                        {
                            long price = nearTouch.price;
                            long initialQuantity = 100;
                            long chosenQuantity = lowestAskQOnBook(state);
                            long quantity = Long.min(initialQuantity, chosenQuantity); // might have a large quantity from ask side, that isn't feasible for the client.
                            logger.info("[MY-STRETCH-ALGO] Have:" + state.getActiveChildOrders().size() +
                                    " children, want 3, joining BUY side of book" +
                                    " with: " + quantity + " @ " + price);
                            return new CreateChildOrder(Side.BUY, quantity, price);
                        }
                    } else {
                        return NoAction.NoAction;
                    }

            }
            long vwapResult = vwap(state);
            logger.info("VWAP Results: " + vwapResult);

            return NoAction.NoAction; // Need this return, as if it doesn't go into the block above, then need to return ' no action'

    }

    public long minBuyBookPrice(SimpleAlgoState state){
        for (int i=0; i < state.getBidLevels(); i++){
            long buyPrice = state.getBidAt(i).price;
            if (buyPrice < comparisonVar){
                comparisonVar = buyPrice;
            }
        }
        logger.info("Lowest Buy price to compare against" + comparisonVar);
        return comparisonVar;
    }

    public long maxAskBookPrice(SimpleAlgoState state){
        long lowestValue = 0;
        for (int i=0; i < state.getAskLevels(); i++){
            long maxAskPrice = state.getAskAt(i).price;
            if (maxAskPrice > lowestValue){
                lowestValue = maxAskPrice;
            }
        }
        logger.info("Highest Ask price to compare against" + lowestValue);
        return lowestValue;
    }

    public long lowestAskQOnBook(SimpleAlgoState state)
    { // Will be using this loop for more than one order's logic
        for (int i = 0; i < state.getAskLevels(); i++){
            long askQuantity = state.getAskAt(i).quantity;
            if (askQuantity < comparisonVar){
                comparisonVar = askQuantity;
            }
        }
        logger.info("Lowest Ask quantity found " + comparisonVar); // Self checking log that the quantity is correct
        return comparisonVar;
    }


    public String postTradeAnalysis (SimpleAlgoState state)
            {
                StringBuilder summary = new StringBuilder();


                state.getActiveChildOrders().forEach(order -> {
                    String orderSummary = String.format("Order ID: %d, Price: %d, Quantity: %d, Side of Book: %s, Quantity filled: %d",
                            order.getOrderId(),
                            order.getPrice(),
                            order.getQuantity(),
                            order.getSide(),
                            order.getFilledQuantity());
                    logger.info((orderSummary));

                });
                // Accumulated VWAP Result -> Need to store it in a HashMap somehow { Key: VWAP , Value: Iterations}
                return summary.toString();
            }

    public long vwap(SimpleAlgoState state) //CBF - Intro to Financial Markets 2024_slide-26
    {
//        logger.info("Testing Method - It looks like it has entered into this method");
        long totalPriceQuantityBids = 0;
        long totalQuantityBids = 0;

        long totalPriceQuantityAsks = 0;
        long totalQuantityAsks = 0;

        int bidLevels = state.getBidLevels();
        int askLevels = state.getAskLevels();

        logger.info("Number of Bid Levels: " + bidLevels);
        logger.info("Number of Ask Levels: " + askLevels);

        int i;
        long result = 0;
        for (i = 0; i <= Math.max(bidLevels, askLevels); i++){
            if (i < bidLevels){
                BidLevel bidLevel = state.getBidAt(i);
                if(bidLevel != null){
//                    logger.info("Bid at index " + i + " has price: " + bidLevel.price + ", quantity: " + bidLevel.quantity);
                    totalPriceQuantityBids += (bidLevel.price * bidLevel.quantity);
                    totalQuantityBids += bidLevel.getQuantity();
                } else {
                    logger.info("Bid at index " + i + " is null");
                }
//                totalPriceQuantityBids =+ (state.getBidAt(i).price * state.getBidAt(i).quantity);
//                totalQuantityBids =+ state.getBidAt(i).getQuantity();
            }

            if (i < askLevels) {
                AskLevel askLevel = state.getAskAt(i);
                if (askLevel != null) {
//                    logger.info("Ask at index " + i + " has price: " + askLevel.price + ", quantity: " + askLevel.quantity);
                    totalPriceQuantityAsks += (askLevel.price * askLevel.quantity);
                    totalQuantityAsks += askLevel.getQuantity();
                } else {
                    logger.info("Ask at index " + i + " is null");
                }
            }

            long totalPriceQuantity = totalPriceQuantityBids + totalPriceQuantityAsks;
            long totalQuantity = totalQuantityBids + totalQuantityAsks;

//            logger.info("Total Price-Quantity for Bids: " + totalPriceQuantityBids + ", Total Quantity for Bids: " + totalQuantityBids);
//            logger.info("Total Price-Quantity for Asks: " + totalPriceQuantityAsks + ", Total Quantity for Asks: " + totalQuantityAsks);

            if (totalQuantity > 0) {
             result =  totalPriceQuantity / totalQuantity;
            } else {
                result = 0;
            }

        }
        return result;
    }




    /*
     Ideas
     - Checking if active orders have been executed fully : List<ChildOrder> executedOrder = activeChildOrders.stream()
     .filter(order -> order.getFilledQuantity() == 0).toList();
     -
     */



}