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

package trader.exchanges.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jooq.lambda.tuple.Tuple2;

/**
 * Created by jkahn on 1/11/16.
 *
 * @author Joshua Kahn
 */
public class ConfigReader {

    private Config config;

    public ConfigReader() {
        this("application.conf");
    }

    public ConfigReader(String configName) {
        this.config = ConfigFactory.load(configName);
    }


    public void setConfig(Config config) {
        this.config = config;
    }

    public Tuple2<String, String> getAPIKeys(String exchange) {
        // TODO - Finish implementing this.
        exchange = exchange.toLowerCase();
        if (exchange.equals("btce") || exchange.equals("btc-e")) {
            return new Tuple2<>(config.getString("btce.apiKey"), config
                    .getString("bitfinex.apiSecretKey"));
        } else if (exchange.equals("bitfinex")) {
            return new Tuple2<>(config.getString("bitfinex.apiKey"), config
                    .getString("bitfinex.apiSecretKey"));
        }
        return new Tuple2<>("", "");
    }

}
