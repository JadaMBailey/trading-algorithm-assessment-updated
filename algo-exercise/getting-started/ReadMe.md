# ReadMe
This readMe has been included to provide easy navigation to the files, and what to expect.
Each objective set has separate files to be accessed.

## Market Data
- There has been 17 market data ticks created (1 marketTick = 30 mins)
- I have increase the ticks at 2.30pm to reflect the USA market open, and additionally increased the spread

## My Strategy
- long vwapResult = vwap(state) -> Capture the vwap of market data for every iteration which is stored in a hashmap collection type
-  String trackingMessage = trackOrderState(state) -> track active orders to review how long it has been present in orderbook which is stored in a hashmap collection type
- Action creatOrderAction = createOrdersConditions(state) -> Create 1 buy and 1 sell order with a criteria of a narrow spread of 2, narrow spread would signal a competitive market
  - Try not to place child orders all at once by separating the creation of Ask and Buy orders into separate conditional statements 
- return cancelOrderConditions(state) -> Cancel child order if activeOrders has moved away from the market, so will return cancel Action or No Action
-  myStretchLogic.postTradeAnalysis(state) -> created a post trade analysis which is displayed at the end of log output to reflect whether my trades have executed at the end of a trading day
  - post Trade analysis is inputted when executing and cancelling of orders finish 

### Pseudocode 



#### Create Child
if(activeChild < 2){
    if(buyMarketSpread < 3){
    price 
    quantity
    return new ChildOrder(buy, price, quantity)
    }
    if(askMarketSpread < 3){
    price
    quantity 
    return new ChildOrder(sell, price, quantity)
    }
}