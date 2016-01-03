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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Trades;
import com.xeiam.xchange.service.polling.marketdata.PollingMarketDataService;
import org.java_websocket.drafts.Draft_10;
import org.slf4j.LoggerFactory;
import trader.exchanges.BitfinexTickGenerator;
import trader.exchanges.BitfinexTrader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jkahn on 12/21/15.
 *
 * @author Joshua Kahn
 */
public class MainTrader {


    private static final URL LOGBACK_CONF_FILE = MainTrader.class.getClassLoader().getResource("logback-traces.xml");

    /**
     * Loads the Logback configuration from a resource file.
     * Only here to avoid polluting other examples with logs. Could be replaced by a simple logback.xml file in the resource folder.
     */
    private static void loadLoggerConfiguration() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(LOGBACK_CONF_FILE);
        } catch (JoranException je) {
            Logger.getLogger(MainTrader.class.getName()).log(Level.SEVERE, "Unable to load Logback configuration", je);
        }
    }

    public static void main(String[] args) throws IOException,
            URISyntaxException {

        loadLoggerConfiguration();

        BitfinexTickGenerator c = new BitfinexTickGenerator(new URI
                ("wss://api2.bitfinex.com:3000/ws"), new Draft_10());
        c.connect();

        BitfinexTrader bitfinexTrader = BitfinexTrader.getInstance();

    }

    private static void generic(Exchange exchange) throws IOException {

        // Interested in the public polling market data feed (no authentication)
        PollingMarketDataService marketDataService = exchange
                .getPollingMarketDataService();

        // Get the latest trade data for BTC/USD
        Trades trades = marketDataService.getTrades(CurrencyPair.BTC_USD);

        System.out.println(trades.toString());
    }

}
