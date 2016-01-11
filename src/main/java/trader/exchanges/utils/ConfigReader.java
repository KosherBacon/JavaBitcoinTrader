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

    /**
     * The default name of the config file, if not specified.
     */
    private final String DEFAULT_CONFIG_FILE;

    private static ConfigReader self;

    private static Config config;

    private ConfigReader() {
        DEFAULT_CONFIG_FILE = "application.conf";
        config = ConfigFactory.load();
    }

    public static ConfigReader getInstance() {
        if (self == null) {
            self = new ConfigReader();
        }
        return self;
    }

    public Tuple2<String, String> getAPIKeys(String exchange) {
        // TODO - Finish implementing this.
        exchange = exchange.toLowerCase();
        if (exchange.equals("btce") || exchange.equals("btc-e")) {
            // TODO - Get BTC-E keys.
        } else if (exchange.equals("bitfinex")) {
            return new Tuple2<>(config.getString("bitfinex.apiKey"), config
                    .getString("bitfinex.apiSecretKey"));
        }
        return new Tuple2<>("", "");
    }

}
