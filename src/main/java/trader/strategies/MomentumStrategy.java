package trader.strategies;

import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import org.jetbrains.annotations.Contract;
import org.jooq.lambda.tuple.Tuple2;

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
        return null;
    }

}
