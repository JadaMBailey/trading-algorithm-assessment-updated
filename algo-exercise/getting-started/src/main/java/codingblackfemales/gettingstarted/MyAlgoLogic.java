package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
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

public class MyAlgoLogic implements AlgoLogic {
    
    long buyPriceLimit = 99;
    long askPriceLimit = 101;
    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);
    private final Map<Long, Integer> orderIterationCount = new HashMap<>(); // tracks order through OrderBook linked to iteration
    private final Map<Integer, Long> vwapTracker = new HashMap<>(); // tracks and stores vwap of each market tick method linked to iteration
    private int marketTickCounter = 0; // Parameter used within 'vwapTracker' to track iteration
    private final List<ChildOrder> completedOrders = new ArrayList<>(); // New list for complete orders

    @Override
    public Action evaluate(SimpleAlgoState state) {

        logger.info("[MY-STRETCH-ALGO] In Algo Logic....");
        var orderBookAsString = Util.orderBookToString(state);
        logger.info("[MY-STRETCH-ALGO] The state of the order book is:\n"
                + orderBookAsString);
        marketTickCounter++; // Increase tick counter for each iteration
        // Retrieve vwap result for each market tick sent
        long vwapResult = vwap(state);
        // Strore vwap against the market tick iterations
        vwapTracker.put(marketTickCounter, vwapResult);
        // start tracking an active order or update iteration
        String trackingMessage = trackOrderState(state);
        logger.info(trackingMessage);
        // criteria to create orders first
        Action createOrderCondition = createOrdersConditions(state);
        if ((createOrderCondition instanceof CreateChildOrder)){
            return createOrderCondition;
        } else {
            // criteria to cancel orders
            return cancelOrderConditions(state);
        }

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
                    logger.error("Bid at index " + i + " is null");
                    // checking if there is a fault within 'AbstractAlgoBackTest' with order's price and quantity
                }
            }

            if (i < askLevels) {
                AskLevel askLevel = state.getAskAt(i);// Retrieve each ask order details
                if (askLevel != null) {
                   totalPriceQuantityAsks += (askLevel.price * askLevel.quantity);
                    totalQuantityAsks += askLevel.getQuantity();
                } else {
                    logger.error("Ask at index " + i + " is null");
                    // checking if there is a fault within 'AbstractAlgoBackTest' with order's price and quantity
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

    public String trackOrderState(SimpleAlgoState state) {
        int currentCount;
        StringBuilder message = new StringBuilder();
        List<ChildOrder> ordersToRemove = new ArrayList<>();
        
        // First, check if any completed orders are still in active orders
        for (ChildOrder order : state.getActiveChildOrders()) {
            if (completedOrders.contains(order)) {
                ordersToRemove.add(order);
                logger.info("[MY-STRETCH-ALGO] Removing already completed order ID: " + order.getOrderId());
            }
        }
        
        // Loop through remaining active orders
        for (ChildOrder order : state.getActiveChildOrders()) {
            if (ordersToRemove.contains(order)) {
                continue;  // Skip orders marked for removal
            }
            
            long orderId = order.getOrderId();
            // Checks if the order has been completely filled
            if (order.getQuantity() == order.getFilledQuantity()) {
                order.setState(OrderState.FILLED);
                orderIterationCount.remove(orderId);
                completedOrders.add(order);
                ordersToRemove.add(order);
                message.append("[MY-STRETCH-ALGO] Removing ").append(order.getSide())
                      .append(" order - ID:").append(orderId)
                      .append(" from list, now completely filled.\n");

            }
            
            // Only track orders that aren't being removed
            if (!orderIterationCount.containsKey(orderId)) {
                orderIterationCount.put(orderId, 1);
                message.append("[MY-STRETCH-ALGO] Tracking new order: ").append(order)
                      .append(" with ID:").append(orderId).append("\n");
            } else {
                currentCount = orderIterationCount.get(orderId);
                orderIterationCount.put(orderId, currentCount + 1);
                message.append("[MY-STRETCH-ALGO] Order ID: ").append(orderId)
                      .append(" has been active for ").append(currentCount)
                      .append(" iterations.\n");
            }
        }
        
        // Remove orders from list that have been 'FILLED'
        if (!ordersToRemove.isEmpty()) {
            state.getActiveChildOrders().removeAll(ordersToRemove);
            logger.info("[MY-STRETCH-ALGO] Removed " + ordersToRemove.size() + " order(s) from active tracking");
        }
        
        return message.toString();
    }

    public Action createOrdersConditions(SimpleAlgoState state) {
        final BidLevel nearTouch = state.getBidAt(0);
        final AskLevel farTouch = state.getAskAt(0);
        final long spreadPrice = farTouch.price - nearTouch.price;

        long activeOrderCount = state.getActiveChildOrders().stream()
                .filter(order -> order.getFilledQuantity() < order.getQuantity())
                .count();
//
        if (activeOrderCount <= 2)
        {
            if(spreadPrice < 3 && nearTouch.getPrice() < buyPriceLimit) {
                // Check if we've already had a completed BUY order
                long completedBuyOrders = completedOrders.stream()
                        .filter(order -> order.getSide() == Side.BUY)
                        .count();

                long buyOrderCount = state.getActiveChildOrders().stream()
                        .filter(order -> order.getSide() == Side.BUY)
                        .count();

                if (buyOrderCount < 1 && completedBuyOrders == 0) {
                    long price = (nearTouch.price - 1);
                    long initialQuantity = 100;
                    long chosenQuantity = lowestAskQuantityOnBook(state);
                    long quantity = Long.min(initialQuantity, chosenQuantity);
                    logger.info("[MY-STRETCH-ALGO] Have:" + activeOrderCount +
                            " children, want 2, joining orderbook on BUY side with: " + quantity + " @ " + price);
                    return new CreateChildOrder(Side.BUY, quantity, price);
                }
            }

            if(spreadPrice < 3 && farTouch.getPrice() > (askPriceLimit)) {
                // Check if we've already had a completed SELL order
                long completedSellOrders = completedOrders.stream()
                        .filter(order -> order.getSide() == Side.SELL)
                        .count();
                
                long sellOrderCount = state.getActiveChildOrders().stream()
                        .filter(order -> order.getSide() == Side.SELL)
                        .count();

                if (sellOrderCount < 1 && completedSellOrders == 0) {
                    activeOrderCount++;
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
    {// Objective is to return the lowest ask quantity : This value is then used to compare against -> Limit market impact
        long comparisonVar = Long.MAX_VALUE;
        for (int i = 0; i < state.getAskLevels(); i++){
            long askQuantity = state.getAskAt(i).quantity;
            if (askQuantity < comparisonVar){
                comparisonVar = askQuantity;
            }
        }
//        logger.info("Lowest Ask quantity found " + comparisonVar); // Self checking log that the quantity is correct
        return comparisonVar;
    }


    public Action cancelOrderConditions(SimpleAlgoState state) {
        // Cancel BUY and SELL order if the market moves after from set order
        for (ChildOrder order : state.getActiveChildOrders()) {
            // Since trackOrderState has already removed filled orders,
            // we don't need to check for filled status here
            long orderId = order.getOrderId();
            
            if (order.getSide() == Side.BUY && order.getPrice() < minBuyBookPrice(state)) {
                logger.info("[MY-STRETCH-ALGO] Cancelling BUY order ID: " + orderId 
                        + ", as it is out of range with new market update");
                orderIterationCount.remove(orderId);
                return new CancelChildOrder(order);
            }
            else if (order.getSide() == Side.SELL && order.getPrice() > maxAskBookPrice(state)) {
                logger.info("[MY-STRETCH-ALGO] Cancelling ASK order ID: " + orderId 
                        + ", as it is out of range with new market update");
                orderIterationCount.remove(orderId);
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
//        logger.info("Lowest Buy price to compare against is " + chosenPrice); // Self checking log that the price is correct
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
        return lowestValue;// returns the highest ask price at current state
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

                    if (order.getFilledQuantity() > 0){
                        boolean beatsAverage = (order.getSide() == Side.BUY && order.getPrice() <= averageVWAP)||
                                (order.getSide() == Side.SELL && order.getPrice() >= averageVWAP);

                        // Ternary operator for Buy or Sell to compare against beatsAverage variable
                        orderSummary += beatsAverage ? " Beats Average VWAP" : " Below Average VWAP";
                    } else {
                        orderSummary += " No VWAP Comparison, as order is not filled.";
                    }
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

        logger.info("Average price of this trading day: " + averageVWAP);
        System.out.println();
        logger.info("Highest price willing to buy at: " + buyPriceLimit);
        logger.info("Lowest price willing to sell at: " + askPriceLimit);
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






