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

package trader.strategies;

import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.indicators.candles.*;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.*;
import eu.verdelhan.ta4j.trading.rules.*;
import org.bouncycastle.asn1.cms.Time;
import org.jetbrains.annotations.Contract;
import org.jooq.lambda.tuple.Tuple2;

/**
 * Created by jkahn on 12/22/15.
 *
 * @author Joshua Kahn
 */
public class BasicStrategy extends Strategy {

    /**
     * The number of ticks needed for the strategy to be fully functional.
     */
    public static final int TICKS_NEEDED = 300;

    /**
     * The number of periods to use for both the crowes and soldiers
     * candlestick analysis.
     */
    private static final int CROWES_AND_SOLDIERS_PERIODS = 4;

    private static final int RSI_PERIODS = 2;
    private static final int RSI_UPPER = 95;
    private static final int RSI_LOWER = 5;

    public BasicStrategy(TimeSeries series) {
        this(buildStrategy(series));
    }

    private BasicStrategy(Tuple2<Rule, Rule> rules) {
        super(rules.v1(), rules.v2());
    }

    /**
     * Build the {@link eu.verdelhan.ta4j.Strategy Strategy} to use for
     * trading and backtesting.
     *
     * @param series
     * {@link eu.verdelhan.ta4j.TimeSeries TimeSeries} to use when building the {@link eu.verdelhan.ta4j.Strategy Strategy}
     * @return the rules to build the {@link eu.verdelhan.ta4j.Strategy Strategy}.
     */
    @Contract("null -> fail")
    private static Tuple2<Rule, Rule> buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        SMAIndicator longSma = new SMAIndicator(closePrice, 200);

        // We use a 2-period RSI indicator to identify buying
        // or selling opportunities within the bigger trend.
        RSIIndicator rsi = new RSIIndicator(closePrice, RSI_PERIODS);

        // The bias is bearish when the shorter-moving average moves below
        // the longer moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 26);

        StochasticOscillatorKIndicator stochasticOscillK = new
                StochasticOscillatorKIndicator(series, 14);

        MACDIndicator macd = new MACDIndicator(closePrice, 9, 26);
        EMAIndicator emaMacd = new EMAIndicator(macd, 18);

        Rule macdEntry = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillK, Decimal
                        .valueOf(20))) // Signal 1
                .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2

        Rule crowesRule = new BooleanIndicatorRule(new
                ThreeBlackCrowsIndicator(series, CROWES_AND_SOLDIERS_PERIODS,
                Decimal.valueOf(7)));

        Rule candleEntry = new OrRule(crowesRule, new BooleanIndicatorRule
                (new BullishHaramiIndicator(series)));

        Rule rsiEntry = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(rsi, Decimal.valueOf
                        (RSI_LOWER))) // Signal 1
                .and(new OverIndicatorRule(shortEma, closePrice)); // Signal 2

        Rule entryRule = new OrRule(macdEntry, rsiEntry)
                .or(candleEntry);

        Rule macdExit = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, Decimal
                        .valueOf(80))) // Signal 1
                .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2

        Rule soldiersRule = new BooleanIndicatorRule(new
                ThreeWhiteSoldiersIndicator(series,
                CROWES_AND_SOLDIERS_PERIODS, Decimal.valueOf(7)));

        Rule rsiExit = new UnderIndicatorRule(shortSma, longSma) // Trend
                .and(new CrossedUpIndicatorRule(rsi, Decimal.valueOf
                        (RSI_UPPER))) // Signal 1
                .and(new UnderIndicatorRule(shortSma, closePrice)); // Signal 2

        Rule exitRule = new OrRule(macdExit, rsiExit)
                .or(soldiersRule)
                .or(new StopLossRule(closePrice, Decimal.valueOf(5))) //
                // Protect against severe losses
                .or(new StopGainRule(closePrice, Decimal.valueOf(15))); //
        // Take profits and run

        return new Tuple2<Rule, Rule>(entryRule, exitRule);
    }

}
