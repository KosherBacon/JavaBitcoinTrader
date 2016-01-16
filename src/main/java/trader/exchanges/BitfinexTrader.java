/*
 * The MIT License (MIT)
 * Copyright (c) 2015-2016 Joshua Kahn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package trader.exchanges;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.bitfinex.v1.service.polling.BitfinexTradeService;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.service.polling.marketdata.PollingMarketDataService;
import eu.verdelhan.ta4j.*;
import trader.TickListener;
import trader.exchanges.utils.BitfinexUtils;
import trader.strategies.BasicStrategy;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jkahn on 12/25/15.
 *
 * @author Joshua Kahn
 */
public class BitfinexTrader implements TickListener {

    /**
     * The currency pair that {@link trader.exchanges.BitfinexTrader this}
     * trader will be using.
     */
    private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.BTC_USD;

    /**
     * A constant value to represent 95%.
     */
    private static final BigDecimal NINETY_FIVE_PERCENT = new BigDecimal("0" +
            ".95");

    /**
     * A constant that represents the minimum order value for Bitfinex.
     */
    private static final BigDecimal MINIMUM_ORDER = new BigDecimal("0.01");

    /**
     * The strategy to use.
     */
    private static Strategy strategy;

    /**
     * The time series for {@link eu.verdelhan.ta4j.Strategy strategy}.
     */
    private static TimeSeries timeSeries;

    /**
     * A state variable to represent the last order that was made.
     */
    private static LastOrder lastOrder;

    private enum LastOrder {
        BOUGHT, SELL, NO_TRADES
    }

    private static BitfinexTradeService bitfinexTradeService;

    private static PollingMarketDataService marketDataService;

    private static Exchange bitfinex;

    private static TradingRecord tradingRecord;

    /**
     * Singleton instance of {@link trader.exchanges.BitfinexTrader this}.
     */
    private static BitfinexTrader INSTANCE;

    private BitfinexTrader() {
        if (BitfinexTrader.INSTANCE != null) {
            throw new InstantiationError("Creating of this object is not " +
                    "allowed.");
        }
        INSTANCE = this;
    }

    public void runTrader() {
        try {
            bitfinex = BitfinexUtils.createExchange();
            marketDataService = bitfinex.getPollingMarketDataService();
            bitfinexTradeService = new BitfinexTradeService(bitfinex);
        } catch (IOException e) {
            e.printStackTrace();
        }
        lastOrder = LastOrder.NO_TRADES;
        tradingRecord = new TradingRecord();
        List<Tick> tickList = new ArrayList<>();
        timeSeries = new TimeSeries("Bitfinex", tickList);
        timeSeries.setMaximumTickCount(BasicStrategy.TICKS_NEEDED);

        BitfinexTickGenerator.addListener(this);
    }

    public void stopTrader() {
        INSTANCE = null;
        BitfinexTickGenerator.removeListener(this);
    }

    /**
     * Gets the singleton instance of this. If this doesn't exist, create a
     * new instance and return it.
     *
     * @return the singleton instance of this
     */
    public static BitfinexTrader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BitfinexTrader();
        }
        return INSTANCE;
    }

    /**
     * Cancel any existing orders.
     *
     * @return true iff the orders were successfully canceled
     */
    private boolean cancelOrders() {
        try {
            List<LimitOrder> orders = bitfinexTradeService.getOpenOrders
                    ().getOpenOrders();
            for (LimitOrder order : orders) {
                bitfinexTradeService.cancelOrder(order.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Goes (almost) all in, either for a bid or ask order. Buys and sells
     * 95% of balance in either direction.
     *
     * @param type BID or ASK, depending
     * @return the balance used to buy in
     */
    private BigDecimal getAmountToOrder(Order.OrderType type) {
        BigDecimal toOrder;
        try {
            if (type == Order.OrderType.ASK) {
                // Sell BTC
                // Get the number of BTC in the wallet, then get 95% of the
                // value
                toOrder = bitfinex.getPollingAccountService()
                        .getAccountInfo().getWallet("BTC").getBalance();
                toOrder = toOrder.multiply(NINETY_FIVE_PERCENT);
            } else {
                // Buy BTC
                // Get the amount of USD in the wallet
                toOrder = bitfinex.getPollingAccountService()
                        .getAccountInfo().getWallet("USD").getBalance();
                // Convert the amount of USD to BTC (using best current ASK
                // price), then get 95% of the value
                toOrder = toOrder.divide(marketDataService.getTicker
                        (CURRENCY_PAIR).getBid(), RoundingMode.FLOOR);
                toOrder = toOrder.multiply(NINETY_FIVE_PERCENT);
            }
            if (toOrder.compareTo(MINIMUM_ORDER) < 0) {
                return BigDecimal.ZERO;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
        return toOrder;
    }

    /**
     * A method to actually place the order.
     *
     * @param order the order to place.
     */
    private void placeOrder(MarketOrder order) {
        try {
            bitfinexTradeService.placeMarketOrder(order);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void tickReceived(Tick tick) {
        timeSeries.addTick(tick);

        /*
         * Trader is still warming up DO NOT TRADE
         *
         * This needs to be done because as of yet, the program isn't
         * connected to a massive database to look trades
         * up in.
         */
        if (timeSeries.getTickCount() < BasicStrategy.TICKS_NEEDED) {
            System.out.println("Received " + timeSeries.getTickCount() +
                    "/" + BasicStrategy.TICKS_NEEDED + " " +
                    "needed ticks.");
            return;
        }

        if (strategy == null) {
            strategy = new BasicStrategy(timeSeries);
        }

        // There's a new tick
        // Therefore we should re-evaluate the strategy
        int endIndex = timeSeries.getEnd();
        if (strategy.shouldEnter(endIndex)) {
            if (lastOrder != LastOrder.BOUGHT) {
                lastOrder = LastOrder.BOUGHT;
                cancelOrders();
                // Buy Bitcoins
                BigDecimal amount = getAmountToOrder(Order.OrderType.BID);
                if (amount.compareTo(MINIMUM_ORDER) >= 0) {
                    System.out.println("Entered the position.");
                    MarketOrder order = new MarketOrder(Order.OrderType.BID,
                            amount, CURRENCY_PAIR);
                    placeOrder(order);
                    tradingRecord.enter(endIndex, tick.getClosePrice(),
                            Decimal.valueOf(amount.toString()));
                }
            }
        } else if (strategy.shouldExit(endIndex)) {
            if (lastOrder != LastOrder.SELL) {
                lastOrder = LastOrder.SELL;
                cancelOrders();
                // Sell Bitcoins
                BigDecimal amount = getAmountToOrder(Order.OrderType.ASK);
                if (amount.compareTo(MINIMUM_ORDER) >= 0) {
                    System.out.println("Exited the position.");
                    MarketOrder order = new MarketOrder(Order.OrderType.ASK,
                            amount, CURRENCY_PAIR);
                    placeOrder(order);
                    tradingRecord.exit(endIndex, tick.getClosePrice(),
                            Decimal.valueOf(amount.toString()));
                }
            }
        }
    }

}
