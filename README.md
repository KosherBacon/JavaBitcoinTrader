[![Build Status](https://travis-ci.org/KosherBacon/JavaBitcoinTrader.svg?branch=master)](https://travis-ci.org/KosherBacon/JavaBitcoinTrader)

# JavaBitcoinTrader

This is a project to buy and sell Bitcoin against other currencies on various exchanges.

## Strategy

The code uses [ta4j] by [mdeverdelhan] to calculate whether or not the trader should enter the position.

Strategies can be found inside the ```trader.strategies``` package. Presently, there are only two. The ```BasicStrategy``` is just that, but it produces the best results of any strategy in the package.

[ta4j]: <https://github.com/mdeverdelhan/ta4j>
[mdeverdelhan]: <https://github.com/mdeverdelhan>