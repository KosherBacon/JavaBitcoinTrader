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

package trader.strategies;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.trading.rules.*;
import org.jetbrains.annotations.Contract;
import org.jooq.lambda.tuple.Tuple2;
import trader.indicators.CMOIndicator;

/**
 * Created by jkahn on 12/22/15.
 *
 * @author Joshua Kahn
 */
public class BasicStrategy extends Strategy {

    /**
     * The number of ticks needed for the strategy to be fully functional.
     */
    public static final int TICKS_NEEDED = 205;

    /**
     * The number of periods to use for the Chande Momentum Oscillator.
     */
    private static final int CMO_PERIODS = 2;

    /**
     * The upper bound to use for the Chande Momentum Oscillator
     */
    private static final int CMO_UPPER = 70;

    /**
     * The lower bound to use for the Chande Momentum Oscillator
     */
    private static final int CMO_LOWER = -70;

    /**
     * The number of periods to use for the short running part of the MACD.
     */
    private static final int MACD_SHORT_PERIODS = 9;

    /**
     * The number of periods to use for the long running part of the MACD.
     */
    private static final int MACD_LONG_PERIODS = 26;

    /**
     * The number of periods to use for the short running SMA.
     */
    private static final int SMA_SHORT_PERIODS = 5;

    /**
     * The number of periods to use for the long running SMA.
     */
    private static final int SMA_LONG_PERIODS = 200;

    /**
     * The threshold (in percent) for the maximum loss allowed on a given
     * trade. This value protects against severe losses, lower values should be
     * used for more conservative trading.
     */
    private static final int STOP_LOSS_THRESHOLD = 10;

    /**
     * The threshold (in percent) for the maximum gain allowed on a given
     * trade. This value protects the gains from a trade, lower values should
     * be used for more conservative trading.
     */
    private static final int STOP_GAIN_THRESHOLD = 10;

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
     * @return the rules to build the
     * {@link eu.verdelhan.ta4j.Strategy Strategy}.
     */
    @Contract("null -> fail")
    private static Tuple2<Rule, Rule> buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        SMAIndicator shortSma = new SMAIndicator(closePrice, SMA_SHORT_PERIODS);
        SMAIndicator longSma = new SMAIndicator(closePrice, SMA_LONG_PERIODS);

        CMOIndicator cmo = new CMOIndicator(series, CMO_PERIODS);

        // The bias is bearish when the shorter-moving averagemoves below the
        // longer moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 26);

        StochasticOscillatorKIndicator stochasticOscillK = new
                StochasticOscillatorKIndicator(series, 14);

        MACDIndicator macd = new MACDIndicator(closePrice, MACD_SHORT_PERIODS,
                MACD_LONG_PERIODS);
        EMAIndicator emaMacd = new EMAIndicator(macd, 18);

        Rule momentumEntry = new OverIndicatorRule(shortSma, longSma) // Trend
                // Signal 1
                .and(new CrossedDownIndicatorRule(cmo,
                        Decimal.valueOf(CMO_LOWER)))
                // Signal 2
                .and(new OverIndicatorRule(shortEma, closePrice));

        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                // Signal 1
                .and(new CrossedDownIndicatorRule(stochasticOscillK,
                        Decimal.valueOf(20)))
                // Signal 2
                .and(new OverIndicatorRule(macd, emaMacd))
                .or(momentumEntry);

        Rule momentumExit = new UnderIndicatorRule(shortSma, longSma) // Trend
                // Signal 1
                .and(new CrossedUpIndicatorRule(cmo,
                        Decimal.valueOf(CMO_UPPER)))
                // Signal 2
                .and(new UnderIndicatorRule(shortSma, closePrice));

        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                // Signal 1
                .and(new CrossedUpIndicatorRule(stochasticOscillK,
                        Decimal.valueOf(80)))
                // Signal 2
                .and(new UnderIndicatorRule(macd, emaMacd))
                .or(momentumExit)
                // Protect against severe losses
                .or(new StopLossRule(closePrice, Decimal.valueOf
                        (STOP_LOSS_THRESHOLD)))
                // Take profits and run
                .or(new StopGainRule(closePrice, Decimal.valueOf
                        (STOP_GAIN_THRESHOLD)));

        return new Tuple2<>(entryRule, exitRule);
    }

}
