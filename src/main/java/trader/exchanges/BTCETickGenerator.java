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
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.btce.v3.BTCEExchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Trade;
import com.xeiam.xchange.dto.marketdata.Trades;
import com.xeiam.xchange.service.polling.marketdata.PollingMarketDataService;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import org.joda.time.DateTime;
import org.joda.time.Period;
import trader.TickListener;

import java.io.IOException;
import java.util.*;

/**
 * Created by jkahn on 12/21/15.
 *
 * @author Joshua Kahn
 */
public class BTCETickGenerator extends TimerTask {

    private static final int TICK_LENGTH = 300;
    private static final Period TICK_TIME_PERIOD = Period.seconds(TICK_LENGTH);

    private static final Exchange BTCE = ExchangeFactory.INSTANCE
            .createExchange(BTCEExchange.class.getName());

    private static long tickStartTime;
    private static long tickEndTime;

    private static LinkedHashMap<String, Trade> tradesInBlock;

    private static boolean needsNewTick;

    private static final List<TickListener> LISTENERS = new ArrayList<>();

    public BTCETickGenerator() {

        System.out.println("BTCETickGenerator Task Started");

        needsNewTick = true;
        tickStartTime = System.currentTimeMillis() / 1000L;
        tickEndTime = tickStartTime + TICK_LENGTH;

        if (tradesInBlock == null) {
            tradesInBlock = new LinkedHashMap<>();
        }
    }

    public synchronized static void addListener(TickListener listener) {
        LISTENERS.add(listener);
    }

    public synchronized static void removeListener(TickListener listener) {
        LISTENERS.remove(listener);
    }

    private synchronized void fireNewTickEvent(Tick tick) {
        for (TickListener tickListener : LISTENERS) {
            tickListener.tickReceived(tick);
        }
    }

    private static List<Trade> generic(Exchange exchange) throws IOException {

        // Interested in the public polling market data feed (no authentication)
        PollingMarketDataService marketDataService = exchange
                .getPollingMarketDataService();

        // Get the latest trade data for BTC/USD
        Trades trades = marketDataService.getTrades(CurrencyPair.BTC_USD);

        return trades.getTrades();
    }

    public void run() {
        System.out.println("RUNNING");
        try {
            List<Trade> recentTrades = generic(BTCE);
            for (Trade entry : recentTrades) {
                Decimal price = Decimal.valueOf(entry.getPrice().toString());
                if (entry.getTimestamp().toInstant().getEpochSecond() <
                        tickStartTime) {
                    continue;
                }
                tradesInBlock.put(entry.getId(), entry);
                System.out.println("Trade Value: " + price.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        needsNewTick = (System.currentTimeMillis() / 1000L) > tickEndTime;
        if (needsNewTick) {
            System.out.println("Tick Needed");
            Tick tick = new Tick(TICK_TIME_PERIOD, new DateTime(tickEndTime *
                    1000L));
            for (Map.Entry<String, Trade> entry : tradesInBlock.entrySet()) {
                tick.addTrade(Decimal.valueOf(entry.getValue()
                        .getTradableAmount().toString()), Decimal.valueOf
                        (entry.getValue().getPrice().toString()));
            }
            fireNewTickEvent(tick);

            needsNewTick = false;
            tickStartTime = tickEndTime;
            tickEndTime += TICK_LENGTH;
            tradesInBlock.clear();
        }
    }
}
