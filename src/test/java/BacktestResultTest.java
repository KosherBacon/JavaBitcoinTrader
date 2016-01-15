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

import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import trader.exchanges.backtest.BacktestResult;
import trader.strategies.BasicStrategy;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Created by jkahn on 1/15/16.
 *
 * @author Joshua Kahn
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BacktestResult.class)
public class BacktestResultTest {

    private BacktestResult result;

    @Mock
    BasicStrategy strategy;

    @Mock
    TimeSeries series;

    @Mock
    TradingRecord record;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.result = spy(new BacktestResult(series, new Strategy[]{strategy}));
        doNothing().when(result, "setResults");
    }

    @Test
    public void testTest() {
        when(series.run(strategy)).thenReturn(record);
        result.test();
        verify(series, times(1)).run(strategy);
    }

}
