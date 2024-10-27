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

public class MyStretchLogic implements AlgoLogic {
    // Need to add reason why the class variables are here and purpose for each
    private static final Logger logger = LoggerFactory.getLogger(MyStretchLogic.class);
    private final Map<Long, Integer> orderIterationCount = new HashMap<>();
    long comparisonVar = Long.MAX_VALUE;
    long priceBuyThreshold = 99;
    long priceAskThreshold = 101;
    private final Map<Integer, Long> vwapTracker = new HashMap<>();
    private int marketTickCounter = 0; // Might be the issue - Test 1
    int currentCount;
    private final List<ChildOrder> completedOrders = new ArrayList<>(); // New list for complete orders

    /*
    ### Outstanding Tasks to complete
    Time : Work out why the orders are not being created properly using logs
Time : Try to show which order has been matched
Time : Commit changes once the above has been completed
Time : Look into different algorithms to find the min value for OrderBook on either side :
(Why){ If this was to go to production, it would take too long to retrieve using the for loop. Consequently not placing orders within time for logic to be effective.
     */

    @Override
    public Action evaluate(SimpleAlgoState state) {

//       executedOrders = state.getChildOrders().stream().filter(order -> OrderState.FILLED).list();

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
        logger.info("VWAP Result for Tick " + marketTickCounter + " is " + vwapResult); // Place vwap info before the create and cancel order action
        logger.info("vwapTracker now has " + vwapTracker.size() + " entries");

        String trackingMessage = verifyingOrders(state); // Start tracking active orders
        logger.info(trackingMessage);

        Action creatOrderAction = createOrdersConditions(state); // Want to create all orders first
        if (!(creatOrderAction instanceof NoAction)){
            return creatOrderAction;
        }

        Action cancelAction = cancelOrderConditions(state); // Cancel active orders based on
        if (!(cancelAction instanceof NoAction)){
            return cancelAction;
        }
        return NoAction.NoAction; // Need this return, as if it doesn't go into the block above, then need to return ' no action'
    }

    public long vwap(SimpleAlgoState state) //CBF - Intro to Financial Markets 2024_slide-26
    {
        long totalPriceQuantityBids = 0;
        long totalQuantityBids = 0;

        long totalPriceQuantityAsks = 0;
        long totalQuantityAsks = 0;

        int bidLevels = state.getBidLevels();
        int askLevels = state.getAskLevels();

        int i;
        long result = 0;
        for (i = 0; i <= Math.max(bidLevels, askLevels); i++) // This ensures all orders are taken into account even if
        {
            if (i < bidLevels){
                BidLevel bidLevel = state.getBidAt(i);
                if(bidLevel != null){
                   totalPriceQuantityBids += (bidLevel.price * bidLevel.quantity);
                    totalQuantityBids += bidLevel.getQuantity();
                } else {
                    logger.info("Bid at index " + i + " is null");
                }
            }

            if (i < askLevels) {
                AskLevel askLevel = state.getAskAt(i);
                if (askLevel != null) {
                   totalPriceQuantityAsks += (askLevel.price * askLevel.quantity);
                    totalQuantityAsks += askLevel.getQuantity();
                } else {
                    logger.info("Ask at index " + i + " is null");
                }
            }

            long totalPriceQuantity = totalPriceQuantityBids + totalPriceQuantityAsks;
            long totalQuantity = totalQuantityBids + totalQuantityAsks;

            if (totalQuantity > 0) {
                result =  totalPriceQuantity / totalQuantity;
            } else {
                result = 0;
            }
        }
        return result;
    }

    public String verifyingOrders(SimpleAlgoState state) {

        StringBuilder message = new StringBuilder();

        for (ChildOrder order : state.getActiveChildOrders()) {
            long orderId = order.getOrderId();
            if ((order.getQuantity() == order.getFilledQuantity())) {
                order.setState(OrderState.FILLED);
                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                completedOrders.add(order); // Add order to 'completed list'
                state.getActiveChildOrders().remove(order); // Remove from active orders list
                message.append("[MY-STRETCH-ALGO] Removing ").append(order.getSide()).append(" order - ID:").append(order.getOrderId()).append(" from list, now completely filled.");
            }

            if (!orderIterationCount.containsKey(orderId)) // If the order ID is not currently in HashMap list (-> Go into block)
            {
                orderIterationCount.put(orderId, 1); // Start tracking the order
                message.append("[MY-STRETCH-ALGO] Tracking new order: ").append(order).append(" with ID:").append(orderId);
                // Only one order is going through here - problem to investigate
            } else {
                // Increment the count for the existing order
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
        var activeOrderTest = (state.getActiveChildOrders().stream().filter(order -> order.getFilledQuantity() < order.getQuantity())).count();

        if (state.getActiveChildOrders().size() < 3 ){ // || OrderState.FILLED == true
            if(spreadPrice > 2 && nearTouch.getPrice() <priceBuyThreshold)
            { // creating less competitive Buy orders
                long buyOrderCount = state.getActiveChildOrders().stream().filter(order -> order.getSide() == Side.BUY).count();// count is an issue, need it to be size
                if (buyOrderCount < 1)
                {
                        long price = (nearTouch.price - 1);// Want to be passive order, due to less competitive scenario
                        long initialQuantity = 250;
                        long chosenQuantity = lowestAskQOnBook(state);
                        long quantity = Long.min(initialQuantity, chosenQuantity); // might have a large quantity from ask side, that isn't feasible for the client.
                       Action orderCreated = new CreateChildOrder(Side.BUY, quantity, price);
                        logger.info("[MY-STRETCH-ALGO] Have:" + activeOrderTest +
                                " children, want 3, joining orderbook on BUY side (passive) with: " + quantity + " @ " + price);
                        return orderCreated;
                }
            }

            if(spreadPrice< 3 && farTouch.getPrice()>=priceAskThreshold) // Something is wrong, no SELL order being made
            { // Short position is risky, better to place order when book looks to be narrow
                long sellOrderCount = state.getActiveChildOrders().stream()
                        .filter(order -> order.getSide() == Side.SELL)
                        .count();
                if (sellOrderCount == 0)
                {
                    activeOrderTest ++;
                    long price = farTouch.price + 1;
                    long quantity = 150;

                    logger.info("[MY-STRETCH-ALGO] Have:" + activeOrderTest
                            + " children, want 3, joining orderbook on ASK side with: " + quantity + " @ " + price);
                    return new CreateChildOrder(Side.SELL, quantity, price);
                }
            } else if(spreadPrice < 3 && nearTouch.getPrice()< priceBuyThreshold)
            {
                // Creating Aggressive BUY order
                long buyOrderCount = state.getActiveChildOrders().stream().filter(order -> order.getSide() == Side.BUY).count();
                if (buyOrderCount < 1) // Might be an issue here
                {
                    long price = nearTouch.price;
                    long initialQuantity = 100;
                    long chosenQuantity = lowestAskQOnBook(state);
                    long quantity = Long.min(initialQuantity, chosenQuantity); // might have a large quantity from ask side, that isn't feasible for the client.
                    logger.info("[MY-STRETCH-ALGO] Have:" + activeOrderTest +
                            " children, want 3, joining orderbook on BUY side with: " + quantity + " @ " + price);
                    return new CreateChildOrder(Side.BUY, quantity, price);
                }
            }
        }
        return NoAction.NoAction;
    }

    public long lowestAskQOnBook(SimpleAlgoState state) // Need rename this method
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


    private Action cancelOrderConditions(SimpleAlgoState state) {
        for (ChildOrder order : state.getActiveChildOrders()) // Checking active orders are still relevant to be on the book
        {
//            // Need to fix the cancelling Logic in 'MyStretchLogic' before adding here
            int cancelLimit = 12;
            long orderId = order.getOrderId();
            // Cancel BUY order if the market moves after from sitting order
            if (order.getSide() == Side.BUY && order.getPrice() < minBuyBookPrice(state)) {
                logger.info("[MY-STRETCH-ALGO] Cancelling BUY order ID:" + order.getOrderId() + ", as it is out of range with new market update");
                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                state.getActiveChildOrders().remove(orderId);
                return new CancelChildOrder(order);
            } // Cancel SELL order if market moves after from sitting order
            else if (order.getSide() == Side.SELL && order.getPrice() > maxAskBookPrice(state)) {
                logger.info("[MY-STRETCH-ALGO] Cancelling ASK order ID:" + order.getOrderId() + ", as it is out of range with new market update");
                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                state.getActiveChildOrders().remove(orderId);
                return new CancelChildOrder(order);
            }

//                // ### Cancel order after 5 iteriations ### #Todo - Problem here
            if (currentCount >= cancelLimit) {
                logger.info("[MY-STRETCH-ALGO] Cancelling order after 5 iterations: " + order);
                orderIterationCount.remove(orderId); // Remove order from tracking after canceling
                state.getActiveChildOrders().remove(orderId); // This line seems to work looking into logs
                return new CancelChildOrder(order);  // Cancel the order after 5 iterations
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

        state.getActiveChildOrders().forEach(order -> {
            String orderSummary = String.format("Order ID: %d, Price: %d, Quantity: %d, Side of Book: %s, Quantity filled: %d",
                    order.getOrderId(), order.getPrice(), order.getQuantity(), order.getSide(), order.getFilledQuantity());
            logger.info((orderSummary));

        });

        // Capture completed orders
        completedOrders.forEach(order -> {
            String orderSummary = String.format("Completed Order - ID: %d, Price: %d, Quantity: %d, Side: %s, Filled: %d",
                    order.getOrderId(), order.getPrice(), order.getQuantity(), order.getSide(), order.getFilledQuantity());
            summary.append(orderSummary).append("\n");
            logger.info(orderSummary);
        });

        // Accumulated VWAP Result -> Need to store it in a HashMap somehow { Key: VWAP , Value: Iterations}
        long averageVWAP = calculateAverageVWAP();
        logger.info("Average VWAP across all ticks: " + averageVWAP); // #Todo: this variable appearing as '0.0'
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



    /*
     Ideas
     - Checking if active orders have been executed fully : List<ChildOrder> executedOrder = activeChildOrders.stream()
     .filter(order -> order.getFilledQuantity() == 0).toList();
     - orderIterationCount.entrySet().stream().map(entry -> " Order Id: " + entry.getKey() + " | Duration within OrderBook: " + (entry.getValue()));
     -//            logger.info("This the output of my new variable: ");

         ### Condition set for cancelling child orders ###
         - Placed up top, so new child orders created doesn't get cancelled automatically
          Stream<ChildOrder> checkActiveO = state.getActiveChildOrders().stream();// Able to manipulate the orders without impacting the existing active order list
            - checkActiveO.forEach(a -> logger.info("This is my activeOrder output: " + a));
            - new ChildOrder(order.getSide(), order.getOrderId(), order.getFilledQuantity(), order.getPrice(), order.getState());
    */





