package trader.Indicators;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.CachedIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.PreviousPriceIndicator;

/**
 * Chande Momentum Oscillator.
 * <p>
 *
 * Created by Joshua Kahn on 1/7/2016.
 *
 * @author Joshua Kahn
 */
public class CMOIndicator extends CachedIndicator<Decimal> {

    private static final Decimal NEGATIVE_ONE = Decimal.valueOf(-1);

    private final Indicator<Decimal> indicator;

    private final int timeFrame;

    private final PreviousPriceIndicator previousPriceIndicator;

    public CMOIndicator(TimeSeries timeSeries, int timeFrame) {
        this(new ClosePriceIndicator(timeSeries), timeFrame, new PreviousPriceIndicator(timeSeries));
    }

    public CMOIndicator(Indicator<Decimal> indicator, int timeFrame, PreviousPriceIndicator previousPriceIndicator) {
        super(indicator);
        this.indicator = indicator;
        this.timeFrame = timeFrame;
        this.previousPriceIndicator = previousPriceIndicator;
    }

    @Override
    protected Decimal calculate(int index) {
        Decimal sum1 = Decimal.ZERO;
        Decimal sum2 = Decimal.ZERO;
        for (int i = Math.max(0, index - timeFrame + 1); i <= index; i++) {
            Decimal close = indicator.getValue(i);
            Decimal prevClose = previousPriceIndicator.getValue(i);

            Decimal d = close.minus(prevClose);
            if (d.compareTo(Decimal.ZERO) >= 0) {
                sum1 = sum1.plus(d);
            } else {
                sum2 = sum2.plus(d.multipliedBy(NEGATIVE_ONE));
            }
        }
        return sum1.minus(sum2).dividedBy(sum1.plus(sum2)).multipliedBy(Decimal.HUNDRED);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " timeFrame: " + timeFrame;
    }

}
