package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.orderbook.OrderBookLevel;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyStretchLogic implements AlgoLogic {
    // Need to add reason why the class variables are here and purpose for each
    private static final Logger logger = LoggerFactory.getLogger(MyStretchLogic.class);
    private final Map<Long, Integer> orderIterationCount = new HashMap<>();
    private final Map<Integer, Long> vwapTracker = new HashMap<>();
    private int marketTickCounter = 0;
    int currentCount;
    private final List<ChildOrder> completedOrders = new ArrayList<>(); // New list for complete orders

    @Override
    public Action evaluate(SimpleAlgoState state) {

        logger.info("[MY-STRETCH-ALGO] In Algo Logic....");
        var orderBookAsString = Util.orderBookToString(state);
        logger.info("[MY-STRETCH-ALGO] The state of the order book is:\n" + orderBookAsString);
        marketTickCounter++; // Increase tick counter for each iteration
        /*
        ### Reorganised code ###
        - Breaking down the logics and storing in methods helps for readability;
        - Helps with testing the code functionality;
         */
        long vwapResult = vwap(state);
        vwapTracker.put(marketTickCounter, vwapResult); // Storing the Vwap against the market data tick iterations
        logger.info("VWAP Result for Tick " + marketTickCounter + " is " + vwapResult);
        // Place vwap info before the create and cancel order action
        String trackingMessage = verifyingOrders(state); // Start tracking active orders
        logger.info(trackingMessage);

        Action creatOrderAction = createOrdersConditions(state); // Want to create all orders first
        if (!(creatOrderAction instanceof NoAction)){
            return creatOrderAction;
        }
        return cancelOrderConditions(state);
    }

    public long vwap(SimpleAlgoState state)
    {
        // Initialise variable with zero
        long totalPriceQuantityBids = 0;
        long totalQuantityBids = 0;

        long totalPriceQuantityAsks = 0;
        long totalQuantityAsks = 0;

        // store bid and ask total order quantity amount in local variables
        int bidLevels = state.getBidLevels();
        int askLevels = state.getAskLevels();

        int i;
        long result = 0; // initialise result variable that will hold end result of Vwap
        for (i = 0; i <= Math.max(bidLevels, askLevels); i++) // This ensures all orders are taken into account
        {
            if (i < bidLevels){
                BidLevel bidLevel = state.getBidAt(i); // Retrieve each bid order details
                if(bidLevel != null)
                {
                   totalPriceQuantityBids += (bidLevel.price * bidLevel.quantity);
                    totalQuantityBids += bidLevel.getQuantity();
                } else {
                    logger.info("Bid at index " + i + " is null");
                    // checking if there is a fault within 'AbstractAlgoBackTest' with order's price and quantity
                }
            }

            if (i < askLevels) {
                AskLevel askLevel = state.getAskAt(i);
                if (askLevel != null) {
                   totalPriceQuantityAsks += (askLevel.price * askLevel.quantity);
                    totalQuantityAsks += askLevel.getQuantity();
                } else {
                    logger.error("Ask at index " + i + " is null");
                }
            }

            long totalPriceQuantity = totalPriceQuantityBids + totalPriceQuantityAsks;
            long totalQuantity = totalQuantityBids + totalQuantityAsks;

            if (totalQuantity > 0) {
                result =  totalPriceQuantity / totalQuantity;
            } else {
                result = 0;
                logger.error("Issue with Vwap at this iteration: " + result);
            }
        }
        return result;
    }

    public String verifyingOrders(SimpleAlgoState state) {

        StringBuilder message = new StringBuilder();
        // Loop through all active orders placed
        for (ChildOrder order : state.getActiveChildOrders()) {
            long orderId = order.getOrderId();
            // Checks if the order has been completely filled
            // To check whether my order is completely filled and if the order has not been added to 'complete orders' list
            if ((order.getQuantity() == order.getFilledQuantity()) && !completedOrders.contains(order)) {
                order.setState(OrderState.FILLED);
                orderIterationCount.remove(order); // Remove order from tracking after canceling
                completedOrders.add(order); // Add order to 'completed list'
                state.getActiveChildOrders().remove(order); // Remove from active orders list
                message.append("[MY-STRETCH-ALGO] Removing ").append(order.getSide()).append(" order - ID:").append(order.getOrderId()).append(" from list, now completely filled.");
            }
            // Starts tracking order
            if (!orderIterationCount.containsKey(orderId)) // If the order ID is not currently in HashMap list (-> Go into block)
            {
                orderIterationCount.put(orderId, 1); // Start tracking the order
                message.append("[MY-STRETCH-ALGO] Tracking new order: ").append(order).append(" with ID:").append(orderId);

            } else // Increment the count for the existing order
            {
                currentCount = orderIterationCount.get(orderId);
                orderIterationCount.put(orderId, currentCount + 1);
                message.append("[MY-STRETCH-ALGO] Order ID: ").append(orderId).append(" has been active for ").append(currentCount).append(" iterations.");
            }
        }
        return message.toString();
    }

    private Action createOrdersConditions(SimpleAlgoState state) {
        // ### Creating Child Orders
        final BidLevel nearTouch = state.getBidAt(0);
        final AskLevel farTouch = state.getAskAt(0);
        final long spreadPrice = farTouch.price - nearTouch.price;
        long activeOrderCount = (state.getActiveChildOrders().stream().filter(order -> order.getFilledQuantity() < order.getQuantity())).count();
        long buyPriceLimit = 99;
        long askPriceLimit = 101;
        if (activeOrderCount < 2 ){ // Allows only two child orders to be created and active on Orderbook
            if(spreadPrice < 3 && nearTouch.getPrice() < buyPriceLimit)
            { // Narrow spread, signals high competition
                long buyOrderCount = state.getActiveChildOrders().stream().filter(order -> order.getSide() == Side.BUY).count();// count is an issue, need it to be size
                if (buyOrderCount < 1)
                {
                        long price = (nearTouch.price - 1);// Want to be passive order but still competitive
                        long initialQuantity = 100;
                        long chosenQuantity = lowestAskQuantityOnBook(state);
                        long quantity = Long.min(initialQuantity, chosenQuantity); // Reduce market impact by blending my quantity
                        logger.info("[MY-STRETCH-ALGO] Have:" + activeOrderCount +
                                " children, want 2, joining orderbook on BUY side (passive) with: " + quantity + " @ " + price);
                        return new CreateChildOrder(Side.BUY, quantity, price);
                }
            }

            if(spreadPrice < 3 && farTouch.getPrice() > askPriceLimit)
            { // Short position is risky, better to place order when book looks to be narrow
                long sellOrderCount = state.getActiveChildOrders().stream()
                        .filter(order -> order.getSide() == Side.SELL)
                        .count();
                if (sellOrderCount < 1)
                {
                    activeOrderCount ++;
                    long price = farTouch.price;
                    long quantity = 100;

                    logger.info("[MY-STRETCH-ALGO] Have:" + activeOrderCount
                            + " children, want 2, joining orderbook on ASK side with: " + quantity + " @ " + price);
                    return new CreateChildOrder(Side.SELL, quantity, price);
                }
            }

        }
        return NoAction.NoAction;
    }

    public long lowestAskQuantityOnBook(SimpleAlgoState state)
    {
        long comparisonVar = Long.MAX_VALUE;
        for (int i = 0; i < state.getAskLevels(); i++){
            long askQuantity = state.getAskAt(i).quantity;
            if (askQuantity < comparisonVar){
                comparisonVar = askQuantity;
            }
        }
        logger.info("Lowest Ask quantity found " + comparisonVar); // Self checking log that the quantity is correct
        return comparisonVar;
    }


    private Action cancelOrderConditions(SimpleAlgoState state) {
        // Cancel BUY and SELL order if the market moves after from setting order
        for (ChildOrder order : state.getActiveChildOrders()) // Checking active orders are still relevant to be on the book
        {
            long orderId = order.getOrderId();
            // if order price is less than the lowest bid price on current market (-> Go into block)
            if (order.getSide() == Side.BUY && order.getPrice() < minBuyBookPrice(state))
            {
                logger.info("[MY-STRETCH-ALGO] Cancelling BUY order ID:" + order.getOrderId() + ", as it is out of range with new market update");
                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                return new CancelChildOrder(order);
            }
            // if order price is more than max sell price on current market (-> Go into block)
            else if (order.getSide() == Side.SELL && order.getPrice() > maxAskBookPrice(state))
            {
                logger.info("[MY-STRETCH-ALGO] Cancelling ASK order ID:" + order.getOrderId() + ", as it is out of range with new market update");
                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                return new CancelChildOrder(order);
            }

        }
        return NoAction.NoAction;
    }

    public long minBuyBookPrice(SimpleAlgoState state)
    {
        long comparePriceMax = Long.MAX_VALUE;
        long chosenPrice = 0; // Stays outside the loop to prevent variable resetting to zero
        for (int i=0; i < state.getBidLevels(); i++){
            long buyPrice = state.getBidAt(i).price;
            if (buyPrice < comparePriceMax){
                chosenPrice = buyPrice;
            }
        }
        logger.info("Lowest Buy price to compare against is " + chosenPrice);
        return chosenPrice;
    }

    public long maxAskBookPrice(SimpleAlgoState state){
        long lowestValue = 0;
        for (int i=0; i < state.getAskLevels(); i++){
            long maxAskPrice = state.getAskAt(i).price;
            if (maxAskPrice > lowestValue){
                lowestValue = maxAskPrice;
            }
        }
        logger.info("Highest Ask price to compare against is " + lowestValue);
        return lowestValue;
    }

    public String postTradeAnalysis (SimpleAlgoState state)
    {
        StringBuilder summary = new StringBuilder();

        System.out.println();
        logger.info("Post Trade Analysis");
        long averageVWAP = calculateAverageVWAP();
        // Capture active orders if any
        state.getActiveChildOrders().stream()
                .filter(order -> order.getState() != OrderState.FILLED)
                .forEach(order -> {
            String orderSummary = String.format("Active Order ID: %d | Price: %d | Quantity: %d | Side: %s | Quantity filled: %d | VWAP Comparison:",
                    order.getOrderId(), order.getPrice(), order.getQuantity(), order.getSide(), order.getFilledQuantity());
                    boolean beatsAverage = (order.getSide() == Side.BUY && order.getPrice() <= averageVWAP)||
                            (order.getSide() == Side.SELL && order.getPrice() >= averageVWAP);

                    orderSummary += beatsAverage ? " Beats Average VWAP" : " Below Average VWAP";
                    logger.info((orderSummary));

        });
        // Capture completed orders if any
        completedOrders.forEach(order -> {
            String orderSummary = String.format("Completed Order ID: %d | Price: %d | Quantity: %d | Side: %s | Quantity Filled: %d | VWAP Comparison:",
                    order.getOrderId(), order.getPrice(), order.getQuantity(), order.getSide(), order.getFilledQuantity());
                    boolean beatsAverage = (order.getSide() == Side.BUY && order.getPrice() <= averageVWAP)||
                            (order.getSide() == Side.SELL && order.getPrice() >= averageVWAP);

            orderSummary += beatsAverage ? " Beats Average VWAP" : " Below Average VWAP";
            logger.info(orderSummary);
            summary.append(orderSummary).append("\n");
        });

        // Accumulated VWAP Result -> Need to store it in a HashMap somehow { Key: VWAP , Value: Iterations}

        logger.info("Average price of this trading day: " + averageVWAP);
        return summary.toString();


    }

    private long calculateAverageVWAP() {
        if (vwapTracker.isEmpty()) {
            logger.warn("VWAP Tracker is empty. No VWAP values to calculate average");
            return 0;
        }
        long totalVWAP = 0;
        for (long vwap : vwapTracker.values()) {
            totalVWAP += vwap;
        }
        return  totalVWAP / vwapTracker.size();
    }
}






