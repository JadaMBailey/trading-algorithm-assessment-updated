package codingblackfemales.orderbook;

import codingblackfemales.orderbook.channel.MarketDataChannel;
import codingblackfemales.orderbook.channel.OrderChannel;
import codingblackfemales.orderbook.order.LimitOrderFlyweight;
import codingblackfemales.orderbook.order.MarketDataOrderFlyweight;
import codingblackfemales.orderbook.visitor.CancelOrderVisitor;
import codingblackfemales.orderbook.visitor.MutatingMatchOneMarketDataOrderVisitor;
import codingblackfemales.orderbook.visitor.MutatingMatchOneOrderVisitor;
import codingblackfemales.orderbook.visitor.ReadOnlyMarketDataChannelPublishVisitor;
import codingblackfemales.sequencer.event.MarketDataEventListener;
import messages.marketdata.AskBookUpdateDecoder;
import messages.marketdata.BidBookUpdateDecoder;
import messages.marketdata.BookUpdateDecoder;
import messages.marketdata.Source;
import messages.order.Side;
import org.agrona.MutableDirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderBook extends MarketDataEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderBook.class);

    private final MarketDataChannel marketDataChannel;
    private final OrderChannel orderChannel;

    public OrderBook(final MarketDataChannel marketDataChannel, final OrderChannel orderChannel) {
        this.marketDataChannel = marketDataChannel;
        this.orderChannel = orderChannel;
    }

    private ReadOnlyMarketDataChannelPublishVisitor mktDataVisitor = new ReadOnlyMarketDataChannelPublishVisitor();
    // JB: Why have they created a new object, and returning the reference in the methods below? #Todo
    private AskBookSide askBookSide = new AskBookSide();
    private BidBookSide bidBookSide = new BidBookSide();

    public AskBookSide getAskBookSide() {
        return askBookSide;
    }

    public BidBookSide getBidBookSide() {
        return bidBookSide;
    }

    public boolean canMatch(final Side side, final long price){
        boolean canMatch = false;

        if(side.equals(Side.BUY) && this.getAskBookSide().getFirstLevel() != null){
            canMatch = this.getAskBookSide().getFirstLevel().getPrice() <= price;
        }else if(side.equals(Side.SELL) && this.getBidBookSide().getFirstLevel() != null){
            canMatch = this.getBidBookSide().getFirstLevel().getPrice() >= price;
        }

        return canMatch;
    }
// JB: breakpoint on 'onBookUpdate()' outputs bid and ask prices in console. #Todo Need to find out which class outputs ask and buy prices.
    @Override
    public void onBookUpdate(BookUpdateDecoder bookUpdate) {
        //don't process updates from ourself.
        if(!bookUpdate.source().equals(Source.ORDERBOOK)){
            logger.info("[.onBookUpdate(JB)] [ORDERBOOK] Processing Mkt Data Update");
            getBidBookSide().removeMarketDataOrders();
            addOrMatchBidMarketDataOrders(bookUpdate); // Need to step into this line of code #Todo

            getAskBookSide().removeMarketDataOrders();
            addOrMatchAskMarketDataOrders(bookUpdate);
        }
    }

    private void addOrMatchAskMarketDataOrders(BookUpdateDecoder bookUpdateDecoder){
        for(BookUpdateDecoder.AskBookDecoder decoder : bookUpdateDecoder.askBook()) {
            final long price = decoder.price();
            final long quantity = decoder.size();
            var marketOrder = new MarketDataOrderFlyweight(Side.SELL, price, quantity);
            logger.debug("[JB:addOrMatchAskMarketDataOrders(1)] [ORDERBOOK] ASK: Adding order" + marketOrder);
            if(canMatch(Side.SELL, price)){
                matchMarketDataOrder(marketOrder);
            }else{
                getAskBookSide().addMarketDataOrder(marketOrder);
            }
        }
    }

    private void addOrMatchAskMarketDataOrders(AskBookUpdateDecoder askBookUpdateDecoder){
        for(AskBookUpdateDecoder.AskBookDecoder decoder : askBookUpdateDecoder.askBook()) {
            final long price = decoder.price();
            final long quantity = decoder.size();
            var marketOrder = new MarketDataOrderFlyweight(Side.SELL, price, quantity);
            logger.debug("[ORDERBOOK] ASK: Adding order" + marketOrder);
            if(canMatch(Side.SELL, price)){
                matchMarketDataOrder(marketOrder);
            }else{
                getAskBookSide().addMarketDataOrder(marketOrder);
            }
        }
    }

    private void addOrMatchBidMarketDataOrders(BidBookUpdateDecoder askBookUpdateDecoder){
        for(BidBookUpdateDecoder.BidBookDecoder decoder : askBookUpdateDecoder.bidBook()) {
            final long price = decoder.price();
            final long quantity = decoder.size();
            var marketOrder = new MarketDataOrderFlyweight(Side.SELL, price, quantity);
            logger.debug("[ORDERBOOK] ASK: Adding order" + marketOrder);
            if(canMatch(Side.BUY, price)){
                matchMarketDataOrder(marketOrder);
            }else{
                getBidBookSide().addMarketDataOrder(marketOrder);
            }
        }
    }

    private void addOrMatchBidMarketDataOrders(BookUpdateDecoder bookUpdateDecoder){
        for(BookUpdateDecoder.AskBookDecoder decoder : bookUpdateDecoder.askBook()) {
            final long price = decoder.price();
            final long quantity = decoder.size();
            var marketOrder = new MarketDataOrderFlyweight(Side.SELL, price, quantity);
            logger.debug("[JB:addOrMatchAskMarketDataOrders(4)] [ORDERBOOK] ASK: Adding order" + marketOrder);
            if(canMatch(Side.BUY, price)){
                matchMarketDataOrder(marketOrder);
            }else{
                getBidBookSide().addMarketDataOrder(marketOrder);
            }
        }
    }
    /*
    ### JB Notes #Todo ###
    Not sure why bid price and size values are being inputted in the 'addOrMatchBidMarketDataOrders()'.
    Additionally, addOrMatchBidMarketDataOrders() is written 4 times to match original ask order quantities. Not sufficient if child order quantity changes

     */


    @Override
    public void onAskBook(AskBookUpdateDecoder askBook) {
        getAskBookSide().removeMarketDataOrders();
        addOrMatchAskMarketDataOrders(askBook);
    }

    @Override
    public void onBidBook(BidBookUpdateDecoder bidBook) {
        getBidBookSide().removeMarketDataOrders();
        addOrMatchBidMarketDataOrders(bidBook);
    }

    public void matchOrder(final LimitOrderFlyweight limit) {
        final MutatingMatchOneOrderVisitor visitor = new MutatingMatchOneOrderVisitor(limit, orderChannel);
        if(limit.getSide().equals(Side.BUY)){
            getAskBookSide().accept(visitor);
        }else if(limit.getSide().equals(Side.SELL)){
            getBidBookSide().accept(visitor);
        }
    }

    public void matchMarketDataOrder(final MarketDataOrderFlyweight market) {
        final MutatingMatchOneMarketDataOrderVisitor visitor = new MutatingMatchOneMarketDataOrderVisitor(market, orderChannel);
        if(market.getSide().equals(Side.BUY)){
            getAskBookSide().accept(visitor);
        }else if(market.getSide().equals(Side.SELL)){
            getBidBookSide().accept(visitor);
        }
    }

    public void addLiquidity(final LimitOrderFlyweight limit) {
        if(limit.getSide().equals(Side.BUY)){
            logger.info("[JB:addLiquidity()] [ORDERBOOK] Adding passive limit order to BID book" + limit);
            this.getBidBookSide().addLimitOrder(limit);
        }else{
            logger.info("[ORDERBOOK] Adding passive limit order to ASK book" + limit);
            this.getAskBookSide().addLimitOrder(limit);
        }
    }

    public void onLimitOrder(final LimitOrderFlyweight limit) {
        if(canMatch(limit.getSide(), limit.getPrice())){
            matchOrder(limit);
        }else{
            addLiquidity(limit);
        }

        publishBook();
    }

    public void onCancelOrder(final long orderIdToCancel){
        logger.info("[JB: onCancelOrder()] [ORDERBOOK] Cancelling order (id=:" + orderIdToCancel + ")");
        var cancelVisitor = new CancelOrderVisitor(orderIdToCancel);
        getAskBookSide().accept(cancelVisitor);
        getBidBookSide().accept(cancelVisitor);
        publishBook();
    }


    public void publishBook(){
        final var messageBuffer = getBookUpdateMessage();
        marketDataChannel.publish(messageBuffer);
    }

    public MutableDirectBuffer getBookUpdateMessage(){
        mktDataVisitor.start();
        getBidBookSide().accept(mktDataVisitor);
        getAskBookSide().accept(mktDataVisitor);
        return mktDataVisitor.end();
    }
}
