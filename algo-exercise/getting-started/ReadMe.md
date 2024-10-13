# ReadMe
This readMe has been included to provide easy navigation to the files, and what to expect.
Each objective set has separate files to be accessed.

## Market Data
- There has been 17 market data ticks created (1 marketTick = 30 mins)
- I have increase the ticks at 2.30pm to reflect the USA market open, and additionally increased the spread

## My Strategy
- Cancel child order if it's activeOrder is moved away from the market
- Try not to place child orders all at once
- Need to create a post trade analysis displayed through teh logs

if(activeChild < 3){
    if(spread < 2){
    price 
    quantity
} else if(spread < 3)
    price
    quantity
} else if (spread < 4){
    price
    quantity
}