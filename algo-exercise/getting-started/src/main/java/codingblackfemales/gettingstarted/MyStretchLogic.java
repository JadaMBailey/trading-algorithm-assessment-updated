package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.orderbook.order.MarketDataOrderFlyweight;
import codingblackfemales.orderbook.visitor.MutatingMatchOneMarketDataOrderVisitor;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AbstractLevel;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MyStretchLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyStretchLogic.class);

    private final Map<Long, Integer> orderIterationCount = new HashMap<>();
    int cancelLimit = 5;

    @Override
    public Action evaluate(SimpleAlgoState state) {


        logger.info("[MY-STRETCH-ALGO] In Algo Logic....");

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MY-STRETCH-ALGO] The state of the order book is:\n" + orderBookAsString);

        final BidLevel nearTouch = state.getBidAt(0);
        final AskLevel farTouch = state.getAskAt(0);
        final long spreadPrice = farTouch.price - nearTouch.price;
        long farTouchQ = farTouch.quantity;
        long nearTouchQ = nearTouch.quantity;
        long OrderBookBidOQuantity = 0;
        long OrderBookAskOQuantity = 0;
        long targetBidQuantity = 1000;
        int buy;
        int ask;
        int marketDataCount = 0;


        for (buy = 0; buy < state.getBidLevels(); buy++) // If less than order book bid size=true, then iterate variable
        {
            var checkingQuantity = state.getBidAt(buy).getQuantity();
//            logger.info("[MY-STRETCH-ALGO] In Algo Logic AND CHECKING FOR LOOP CONDITION " + checkingQuantity);
            OrderBookBidOQuantity += checkingQuantity; // This accummulates the quanitity for bids on OrderBook
//            logger.info("[MY-STRETCH-ALGO] In Algo Logic AND Checking sum of quantity " + OrderBookBidOQuantity); // Check for myself
        }

        for (ask = 0; ask < state.getAskLevels(); ask++)
        {
            var checkingAskQunatity = state.getAskAt(ask).getQuantity();
//            logger.info("[MY-STRETCH-ALGO] In Algo Logic AND CHECKING FOR LOOP CONDITION " + checkingAskQunatity);
            OrderBookAskOQuantity += checkingAskQunatity;
//            logger.info("[MY-STRETCH-ALGO] In Algo Logic AND Checking sum of quantity " + OrderBookAskOQuantity);
        }


            if (state.getChildOrders().size() < 3 || state.getChildOrders().contains(Side.SELL) == true) // Issue here
            {
                if (spreadPrice > 3) // wider spread -> less competitive
                {
                    long price = (nearTouch.price - (spreadPrice));
                    // JB: 1) workout spread 2) Take away from the highest bid price
                    long quantity = OrderBookAskOQuantity / state.getAskLevels();
                    if (quantity > 500) // Trying to blend in with quantity here, as less competitive likely means less activity (can't look desperate)
                    {
                        quantity = 500;
                    }
                    logger.info("[MY-STRETCH-ALGO] Have:" + state.getActiveChildOrders().size()
                            + " children, want 3, joining passive side of book on BUY side" +
                            " with: " + quantity + " @ " + price);
                    return new CreateChildOrder(Side.BUY, quantity, price);
                }
            }

                if (state.getChildOrders().size() < 3) // Need to alter condition as it is
                {
                    if (spreadPrice < 2 && state.getActiveChildOrders().contains(Side.SELL) == false) // Need to fix logic condition allows two sell orders #Todo
                    {// Short position is risky, better to place order when the book looks to be narrow
                        long price = (farTouch.price + (spreadPrice));
                        // JB: 1) workout spread 2) Take away from the highest bid price
                        long quantity = 200;

                        logger.info("[MY-STRETCH-ALGO] Have:" + state.getActiveChildOrders().size()
                                + " children, want 3, joining passive side of book on ASK side" +
                                " with: " + quantity + " @ " + price);
                        return new CreateChildOrder(Side.SELL, quantity, price);
                    }
                }
        if (state.getActiveChildOrders().size() < 3) // Total of 3 child orders to control how many are made
        {
            if (state.getChildOrders().size() < 1)
            {
                if (spreadPrice < 3) // difference in topbook prices being under 3 : narrow spread -> be more competitive
                {
                    long price = nearTouch.price;
                    long quantity = 75;
                    long lowestAskQ = Long.MAX_VALUE;// Need to research more on this constant

                    for (int i = 0; i < state.getAskLevels(); i++)
                    { // I am iterating through the ask side of the book, as I don't want a large quantity with the price set
                        // , so looking to fill all order if a match does occur.
                        long askQuantity = state.getAskAt(i).quantity;
                        if (askQuantity < lowestAskQ){
                            lowestAskQ = askQuantity;
                        }
                    }
                    if (lowestAskQ == Long.MAX_VALUE){
                        logger.info("No valid Ask quantity found ");
                    } else {
                        logger.info("Lowest Ask quantity found " + lowestAskQ);
                    }
                    quantity = lowestAskQ;
//                    logger.info("THIS IS A CHECK FOR 'AVG' VARIABLE " + quantity);

                    logger.info("[MY-STRETCH-ALGO] Have:" + state.getActiveChildOrders().size() +
                            " children, want 3, joining BUY side of book" +
                            " with: " + quantity + " @ " + price);
                    return new CreateChildOrder(Side.BUY, quantity, price);
                }
            } else if (state.getChildOrders().size() < 3 && state.getActiveChildOrders().size() < 3) {
                    logger.info("[MY-STRETCH-ALGO] Have:" + state.getChildOrders().size() + " children added, want 3, done.");
                } else {
                    logger.info("[MY-STRETCH-ALGO] Have:" + state.getChildOrders().size() + " children and couldn't achieve target of 3 :C .");
                    // Need to fix this logic as it is taking into account the cancelled orders as well
                    return NoAction.NoAction;
                }

                    for (ChildOrder order:state.getActiveChildOrders()){
                        long orderId = order.getOrderId();

                        if (!orderIterationCount.containsKey(orderId)) {
                            orderIterationCount.put(orderId, 1); // Start tracking the order
                            logger.info("[MY-STRETCH-ALGO] Tracking new order: " + order + " with ID: " + orderId);
                        } else {
                            // Increment the count for the existing order
                            int currentCount = orderIterationCount.get(orderId);
                            orderIterationCount.put(orderId, currentCount + 1);
                            logger.info("[MY-STRETCH-ALGO] Order ID: " + orderId + " has been active for " + currentCount + " iterations.");

                            // If the order has been active for 5 iterations, cancel it
                            if (currentCount >= cancelLimit) {
                                logger.info("[MY-STRETCH-ALGO] Cancelling order after 5 iterations: " + order);
                                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                                return new CancelChildOrder(order);  // Cancel the order after 5 iterations
                            }
                        }

                        long bestBid = state.getBidAt(0).price;
                        long bestAsk = state.getAskAt(0).price;

//                        if (order.getSide() == Side.BUY && order.getPrice() < bestAsk) {
//                            logger.info("[MY-STRETCH-ALGO] Cancelling BUY order: " + order);
//                            return new CancelChildOrder(order);
//                        }
//
//                        // Cancel sell orders that have a price higher than the best bid (not profitable)
//                        if (order.getSide() == Side.SELL && order.getPrice() > bestBid) {
//                            logger.info("[MY-STRETCH-ALGO] Cancelling SELL order: " + order);
//                            return new CancelChildOrder(order);
//                        }
                    } // Need to add cancel condition when the order in orderBook is out of bounds

        }

        return NoAction.NoAction;
    }

    public String postTradeAnalysis(SimpleAlgoState state)
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
            return summary.toString();
     }


//    public void vwap(SimpleAlgoState state) //CBF - Intro to Financial Markets 2024_slide-26
//    {
//        logger.info("Testing Method - It looks like it has entered into this method");
//        long totalPriceQuantityBids = 0;
//        long totalQuantityBids = 0;
//
//        long totalPriceQuantityAsks = 0;
//        long totalQuantityAsks = 0;
//
//        int bidLevels = state.getBidLevels();
//        int askLevels = state.getAskLevels();
//
//        logger.info("Number of Bid Levels: " + bidLevels);
//        logger.info("Number of Ask Levels: " + askLevels);
//
//        int i;
//        for (i = 0; i <= Math.max(bidLevels, askLevels); i++){
//            if (i < bidLevels){
//                BidLevel bidLevel = state.getBidAt(i);
//                if(bidLevel != null){
//                    logger.info("Bid at index " + i + " has price: " + bidLevel.price + ", quantity: " + bidLevel.quantity);
//                    totalPriceQuantityBids += (bidLevel.price * bidLevel.quantity);
//                    totalQuantityBids += bidLevel.getQuantity();
//                } else {
//                    logger.info("Bid at index " + i + " is null");
//                }
////                totalPriceQuantityBids =+ (state.getBidAt(i).price * state.getBidAt(i).quantity);
////                totalQuantityBids =+ state.getBidAt(i).getQuantity();
//            }
//
//            if (i < askLevels) {
//                AskLevel askLevel = state.getAskAt(i);
//                if (askLevel != null) {
//                    logger.info("Ask at index " + i + " has price: " + askLevel.price + ", quantity: " + askLevel.quantity);
//                    totalPriceQuantityAsks += (askLevel.price * askLevel.quantity);
//                    totalQuantityAsks += askLevel.getQuantity();
//                } else {
//                    logger.info("Ask at index " + i + " is null");
//                }
//            }
//
//            long totalPriceQuantity = totalPriceQuantityBids + totalPriceQuantityAsks;
//            long totalQuantity = totalQuantityBids + totalQuantityAsks;
//
//            logger.info("Total Price-Quantity for Bids: " + totalPriceQuantityBids + ", Total Quantity for Bids: " + totalQuantityBids);
//            logger.info("Total Price-Quantity for Asks: " + totalPriceQuantityAsks + ", Total Quantity for Asks: " + totalQuantityAsks);
//
//            if (totalQuantity > 0) {
//                logger.info("VWAP Results: " + totalPriceQuantity / totalQuantity);
//            } else {
//                logger.warn("No valid data for VWAP calculation.");
//            }
//
//        }
//    }
//}
}