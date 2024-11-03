# ReadMe
This readMe has been included to provide easy navigation to the files, and what to expect.
Each objective set has separate files to be accessed.

## Market Data
- There has been 17 market data ticks created (1 marketTick = 30 mins) #Todo
- I have increase the ticks at 2.30pm to reflect the USA market open, and additionally increased the spread

## My Strategy Flow
My conditions in order to create an order is dependent on a narrow spread of the topbook orders and the top order at that current state is below the price limit set. 


![img.png](img.png)

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

## Understanding the log outputs
### The Logging Consumer will output the following 
- The current Orderbook state
- Displays reference to my orders placed
  - Orders that I have been created to be added to the Orderbook
  - Orders that have been filled 
  - Orders that have been cancelled 

### Removing, adding and matching Makret Data (OrderBook)
- After processing Mkt Data log output
- It looks to remove existing market data if present
- It then looks to add or match my order against the current market data
  - try to match order first
  - Then if not successful add onto the orderbook

### fLOW
- The current Orderbook state
- 'Processing Mkt Data Update' log out correlates with eack market data tick of the above orderbook output 
- MarketDataService.onBookUpdate : lists out each market data order
- After an action type is chosen in the evaluate method in my logic, it then sents the action type to runAlgoLogic() within the AlgoContainer class.
- If the action type is not 'NoAction' then it goes to processAction in Action class
  - For create a child order : CreateChildOrder.apply() -> 
### What I want to implement
- Add another cancellation logic that reflects a stop loss measure put in place
- Modify and add the overall condition for creating orders to factor in orders that have been completed
  - Potentially will be able to remove the inner child limit condition 
