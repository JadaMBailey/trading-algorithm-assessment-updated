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

import java.util.Iterator;

public class MyStretchLogic2 implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyStretchLogic.class);


    @Override
    public Action evaluate(SimpleAlgoState state) {

        logger.info("[MY-STRETCH-ALGO] In Algo Logic....");

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MY-STRETCH-ALGO] The state of the order book is:\n" + orderBookAsString);

        // ### Buy Orders
          // Client's order quantity
        long totalBuyQuantity = 1000;
        long absoluteBuyLimit = 101;   // Absolute price limit set by client

        for (int i = 0; i < state.getAskLevels(); i++) {
            AskLevel askLevel = state.getAskAt(i);
            if (askLevel != null && askLevel.price <= absoluteBuyLimit) {
                long askQty = askLevel.quantity;
                long orderQty = Math.min(askQty, (totalBuyQuantity/4)); // Use ASK side's quantity to avoid market impact

                if (totalBuyQuantity > 0) {
                    logger.info("[MY-LOGIC] Placing Buy Child Order: " + orderQty + "@" + askLevel.price);// Reduce total quantity to place
                    return new CreateChildOrder(Side.BUY, orderQty, askLevel.price);  // Return the child order action
                }
                totalBuyQuantity -= orderQty;
            }
        }

        // ## Sell Order
        long totalSellQuantity = 300;  // Client's sell order quantity
        long absoluteSellLimit = 102;  // Absolute price limit for sell orders
        double stopLossThreshold = 0.10;  // 10% stop loss

        for (int i = 0; i < state.getBidLevels(); i++) {
            BidLevel bidLevel = state.getBidAt(i);
            if (bidLevel != null && bidLevel.price >= absoluteSellLimit * (1 - stopLossThreshold)) {
                long bidQty = bidLevel.quantity;
                long quantity = Math.min(bidQty, totalSellQuantity); // Minimize market impact

                if (totalSellQuantity > 0) {
                    logger.info("[MY-LOGIC] Placing Sell Child Order: " + quantity + " @ " + bidLevel.price);
                    CreateChildOrder sellOrder = new CreateChildOrder(Side.SELL, quantity, bidLevel.price);
                    totalSellQuantity -= quantity;
                    return sellOrder;
                }
            }

        }

    return NoAction.NoAction;
    }

    public String postTradeAnalysis(SimpleAlgoState state) {
        StringBuilder summary = new StringBuilder();
        summary.append("Post-Trade Analysis\n");

        state.getActiveChildOrders().forEach(order -> {
            String orderSummary = String.format(
                    "Order ID: %d, Side: %s, Price: %d, Quantity: %d, Filled: %d%%\n",
                    order.getOrderId(),
                    order.getSide(),
                    order.getPrice(),
                    order.getQuantity(),
                    calculateFilledPercentage(order)
            );
            summary.append(orderSummary);
        });

        logger.info(summary.toString());
        return summary.toString();
    }

    private int calculateFilledPercentage(ChildOrder order) {
        return (int) ((order.getFilledQuantity() * 100) / order.getQuantity());
    }

}