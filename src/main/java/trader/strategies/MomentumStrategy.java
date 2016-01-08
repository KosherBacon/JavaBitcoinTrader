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

package trader.strategies;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.oscillators.AroonDownIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.AroonUpIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.trading.rules.*;
import org.jetbrains.annotations.Contract;
import org.jooq.lambda.tuple.Tuple2;
import trader.indicators.CMOIndicator;

/**
 * Created by Joshua Kahn on 1/7/2016.
 *
 * @author Joshua Kahn
 */
public class MomentumStrategy extends Strategy {


    public MomentumStrategy(TimeSeries series) {
        this(buildStrategy(series));
    }

    private MomentumStrategy(Tuple2<Rule, Rule> rules) {
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

        // Chande Moving Oscillator
        CMOIndicator cmoIndicator = new CMOIndicator(series, 14);

        // Aroon indicators
        AroonUpIndicator aroonUpIndicator = new AroonUpIndicator(series, 25);
        AroonDownIndicator aroonDownIndicator = new AroonDownIndicator(series, 25);

        RSIIndicator rsi = new RSIIndicator(closePrice, 14);

        Rule entryRule = new OverIndicatorRule(aroonUpIndicator, aroonDownIndicator).and(new CrossedUpIndicatorRule(rsi, Decimal.valueOf(30)));

        // Protect assets on loss
        Rule stopLoss = new StopLossRule(closePrice, Decimal.valueOf(15));

        Rule exitRule = new AndRule(new OverIndicatorRule(aroonDownIndicator, aroonUpIndicator), new CrossedDownIndicatorRule(rsi, Decimal.valueOf(70))).or(stopLoss);

        return new Tuple2<>(entryRule, exitRule);
    }

}
