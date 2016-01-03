/*
 * The MIT License (MIT)
 * Copyright (c) 2015 Joshua Kahn
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

package trader;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import trader.exchanges.backtest.BacktestLoader;
import trader.exchanges.backtest.BacktestResult;
import trader.strategies.BasicStrategy;

/**
 * Created by jkahn on 12/21/15.
 *
 * @author Joshua Kahn
 */
public class Backtester {

    private static BacktestResult backtestBitstamp() {
        TimeSeries series = BacktestLoader.loadSeries("bitstampUSD.csv");
        Strategy strategy = BasicStrategy.buildStrategy(series);

        return new BacktestResult(series, 0.0025D, 0, strategy).test();
    }

    private static BacktestResult backtestBTCE() {
        TimeSeries series = BacktestLoader.loadSeries("btceUSD.csv");
        Strategy strategy = BasicStrategy.buildStrategy(series);

        return new BacktestResult(series, 0.002D, 0, strategy).test();
    }

    private static BacktestResult backtestBitfinex() {
        TimeSeries series = BacktestLoader.loadSeries("bitfinexUSD.csv");
        Strategy strategy = BasicStrategy.buildStrategy(series);

        return new BacktestResult(series, 0.002D, 0, strategy).test();
    }

    private static void printBackTestResults(String mkt, TimeSeries series,
                                             Strategy strategy) {
        BacktestResult result = new BacktestResult(series, strategy).test();
        StringBuilder builder = new StringBuilder();
        builder.append(mkt + " Backtest: " + series
                .getSeriesPeriodDescription());
        builder.append("\n\tSeries Metrics:");
        int numTicks = series.getTickCount();
        builder.append("\n\t\tNumber of Ticks: " + numTicks);
        builder.append("\n\t\tFirst Tick Open Price: " + series.getTick(0)
                .getOpenPrice());
        builder.append("\n\t\tLast Tick Close Price: " + series.getTick
                (numTicks - 1).getClosePrice());
        Decimal volume = Decimal.ZERO;
        for (int i = 0; i < numTicks; i++) {
            volume = volume.plus(series.getTick(i).getAmount());
        }
        builder.append("\n\t\tVolume (All Ticks): " + volume);
        builder.append("\n\n\tTrade Performance:");
        builder.append("\n\t\tNumber of Trades: " + result.getNumberOfTrades());
        builder.append("\n\t\tTotal Profit: " + result.getProfit());
        builder.append("\n\t\tBuy and Hold Profit: " + result
                .getBuyAndHoldProfit());
        double profitableTradesRatio = result.getProfitableTradesRatio();
        builder.append("\n\t\tProfitable Trades Ratio: " +
                profitableTradesRatio + " (" + profitableTradesRatio * 100D +
                "%)");
        double maximumDrawdown = result.getMaxDrawdown();
        builder.append("\n\t\tMaximum Drawdown: " + maximumDrawdown + " (" +
                maximumDrawdown * 100D + "%)");
        builder.append("\n\t\tReward Risk Ratio: " + result
                .getRewardRiskRatio());
        builder.append("\n\t\tLinear Transaction Cost: " + result
                .getLinearTransactionCost());

        System.out.println(builder.toString());
    }

    public static void main(String args[]) {
        BacktestResult btceResult = backtestBTCE();
        printBackTestResults("BTCE", btceResult.getTimeSeries(), btceResult
                .getStrategy());
        System.out.println();
        BacktestResult bitstampResult = backtestBitstamp();
        printBackTestResults("Bitstamp", bitstampResult.getTimeSeries(),
                bitstampResult.getStrategy());
        System.out.println();
        BacktestResult bitfinexResult = backtestBitfinex();
        printBackTestResults("Bitfinex", bitfinexResult.getTimeSeries(),
                bitfinexResult.getStrategy());
        System.out.println();
    }

}
