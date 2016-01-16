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

import com.xeiam.xchange.bitfinex.v1.service.polling.BitfinexTradeService;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.dto.trade.OpenOrders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import trader.exchanges.BitfinexTrader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.support.SuppressCode.suppressConstructor;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Created by jkahn on 1/15/16.
 *
 * @author Joshua Kahn
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BitfinexTrader.class)
public class BitfinexTraderTest {

    @InjectMocks
    BitfinexTrader trader;

    @Mock
    BitfinexTradeService bitfinexTradeService;

    @Mock
    MarketOrder mktOrder;

    private OpenOrders openOrders;

    private final String orderID = "12345";

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        LimitOrder order = new LimitOrder.Builder(Order.OrderType.ASK,
                CurrencyPair.BTC_USD)
                .id(orderID)
                .limitPrice(BigDecimal.ONE)
                .timestamp(new Date(System.currentTimeMillis()))
                .tradableAmount(BigDecimal.TEN)
                .build();
        openOrders = new OpenOrders(Collections.singletonList(order));
    }

    @Test
    public void cancelOrdersTest() throws Exception {
        when(bitfinexTradeService.getOpenOrders()).thenReturn(openOrders);

        Field field = PowerMockito.field(BitfinexTrader.class,
                "bitfinexTradeService");
        field.set(BitfinexTrader.class, bitfinexTradeService);

        boolean status = invokeMethod(trader, "cancelOrders");
        assertTrue(status);

        verify(bitfinexTradeService, times(1)).cancelOrder(orderID);
        trader.stopTrader();
    }

    @Test
    public void placeOrderTest() throws Exception {
        when(bitfinexTradeService.placeMarketOrder(mktOrder)).thenReturn(orderID);

        Field field = PowerMockito.field(BitfinexTrader.class,
                "bitfinexTradeService");
        field.set(BitfinexTrader.class, bitfinexTradeService);

        invokeMethod(trader, "placeOrder", mktOrder);
        verify(bitfinexTradeService, times(1)).placeMarketOrder(mktOrder);

        trader.stopTrader();
    }

}
