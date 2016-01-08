/*
 * The MIT License (MIT)
 * Copyright (c) 2015 - 2016 Joshua Kahn
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

package trader.exchanges.backtest;

import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.analysis.criteria.*;

/**
 * Created by jkahn on 12/24/15.
 *
 * @author Joshua Kahn
 */
public class BacktestResult {

    private static final double INITIAL_AMOUNT = 1D;

    private int numberOfTrades;

    private double profit;
    private double buyAndHoldProfit;
    private double profitableTradesRatio;
    private double maxDrawdown;
    private double rewardRiskRatio;
    private double linearTransactionCost;

    private double transactionPercentCost;
    private double transactionFixedCost;

    private TimeSeries timeSeries;

    private Strategy[] strategies;

    private TradingRecord tradingRecord;

    private boolean backtestRan;

    public BacktestResult(TimeSeries timeSeries, double
            transactionPercentCost, double transactionFixedCost, Strategy...
            strategies) {
        this.backtestRan = false;
        this.timeSeries = timeSeries;
        this.strategies = strategies;
        this.transactionPercentCost = transactionPercentCost;
        this.transactionFixedCost = transactionFixedCost;
    }

    public BacktestResult(TimeSeries timeSeries, Strategy... strategies) {
        this(timeSeries, 0D, 0D, strategies);
    }

    public BacktestResult(TimeSeries timeSeries, double
            transactionPercentCost, double transactionFixedCost, Strategy
            strategy) {
        this(timeSeries, transactionPercentCost, transactionFixedCost, new
                Strategy[]{strategy});
    }

    public BacktestResult(TimeSeries timeSeries, Strategy strategy) {
        this(timeSeries, new Strategy[]{strategy});
    }

    public BacktestResult test() {
        this.backtestRan = true;
        this.tradingRecord = this.timeSeries.run(this.strategies[0]);

        setResults();
        return this;
    }

    public BacktestResult walkoverTest() {
        this.backtestRan = true;

        //TODO - Finish writing to code to do a walkover test.

        setResults();
        return this;
    }

    public boolean backtestRan() {
        return this.backtestRan;
    }

    public int getNumberOfTrades() {
        return this.numberOfTrades;
    }

    public double getProfit() {
        return this.profit;
    }

    public double getBuyAndHoldProfit() {
        return this.buyAndHoldProfit;
    }

    public double getProfitableTradesRatio() {
        return this.profitableTradesRatio;
    }

    public double getMaxDrawdown() {
        return this.maxDrawdown;
    }

    public double getRewardRiskRatio() {
        return this.rewardRiskRatio;
    }

    public double getLinearTransactionCost() {
        return this.linearTransactionCost;
    }

    public Strategy getStrategy() {
        return this.strategies[0];
    }

    public Strategy getStrategy(int i) {
        return this.strategies[i];
    }

    public TimeSeries getTimeSeries() {
        return this.timeSeries;
    }

    private void setResults() {
        this.numberOfTrades = (int) (new NumberOfTradesCriterion().calculate
                (this.timeSeries, this.tradingRecord) + 0.5D);
        this.profit = new TotalProfitCriterion().calculate(this.timeSeries,
                this.tradingRecord);
        this.buyAndHoldProfit = new BuyAndHoldCriterion().calculate(this
                .timeSeries, this.tradingRecord);
        this.profitableTradesRatio = new AverageProfitableTradesCriterion()
                .calculate(this.timeSeries, this.tradingRecord);
        this.maxDrawdown = new MaximumDrawdownCriterion().calculate(this
                .timeSeries, this.tradingRecord);
        this.rewardRiskRatio = new RewardRiskRatioCriterion().calculate(this
                .timeSeries, this.tradingRecord);
        this.linearTransactionCost = new LinearTransactionCostCriterion(
                INITIAL_AMOUNT, this.transactionPercentCost, this
                .transactionFixedCost)
                .calculate(this.timeSeries, this.tradingRecord);
    }

}
