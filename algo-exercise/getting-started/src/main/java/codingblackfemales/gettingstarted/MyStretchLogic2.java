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
import java.util.stream.Stream;

public class MyStretchLogic2 implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyStretchLogic.class);
    private final Map<Long, Integer> orderIterationCount = new HashMap<>();
    long lowestAskQ = Long.MAX_VALUE;

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
            // Need to fix the cancelling Logic in 'MyStretchLogic' before adding here
            long orderId = order.getOrderId();
            int currentCount;
            int cancelLimit = 5;
            if (!orderIterationCount.containsKey(orderId)) // If the order ID is not currently in HashMap list (-> Go into block)
            {
                orderIterationCount.put(orderId, 1); // Start tracking the order
                logger.info("[MY-STRETCH-ALGO] Tracking new order: " + order + " with ID: " + orderId); // Only one order is going through here - problem to investigate
            } else {
                // Increment the count for the existing order
                currentCount = orderIterationCount.get(orderId);
                orderIterationCount.put(orderId, currentCount + 1);
                logger.info("[MY-STRETCH-ALGO] Order ID: " + orderId + " has been active for " + currentCount + " iterations.");

                // If the order has been active for 5 iterations, cancel it
                if (currentCount >= cancelLimit) {
                    logger.info("[MY-STRETCH-ALGO] Cancelling order after 5 iterations: " + order);
//                    orderIterationCount.entrySet().stream().map(entry -> " Order Id: " + entry.getKey() + " | Duration within OrderBook: " + (entry.getValue()));
                    orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                    state.getActiveChildOrders().remove(orderId);
                    return new CancelChildOrder(order);  // Cancel the order after 5 iterations
                }
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
                    if ( buyOrderCount < 2) {
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

            return NoAction.NoAction; // Need this return, as if it doesn't go into the block above, then need to return ' no action'

    }

    public long lowestAskQOnBook(SimpleAlgoState state)
    { // Will be using this loop for more than one order's logic
        for (int i = 0; i < state.getAskLevels(); i++){
            long askQuantity = state.getAskAt(i).quantity;
            if (askQuantity < lowestAskQ){
                lowestAskQ = askQuantity;
            }
        }
        logger.info("Lowest Ask quantity found " + lowestAskQ); // Self checking log that the quantity is correct
        return lowestAskQ;
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
                return summary.toString();
            }

    private int calculateFilledPercentage(ChildOrder order) {
        return (int) ((order.getFilledQuantity() * 100) / order.getQuantity());
    }

}