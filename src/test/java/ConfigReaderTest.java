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

import com.typesafe.config.Config;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import trader.exchanges.utils.ConfigReader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jkahn on 1/15/16.
 *
 * @author Joshua Kahn
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigReaderTest {

    private ConfigReader configReader;

    @Mock
    Config config;

    @Before
    public void setup() {
        this.configReader = new ConfigReader();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAPIKeys() {
        when(config.getString("bitfinex.apiKey")).thenReturn("API_KEY");
        when(config.getString("bitfinex.apiSecretKey"))
                .thenReturn("API_SECRET_KEY");

        configReader.setConfig(config);

        Tuple2<String, String> apiKeys = configReader.getAPIKeys("bitfinex");
        String apiKey = apiKeys.v1();
        String apiSecretKey = apiKeys.v2();

        verify(config).getString("bitfinex.apiKey");
        verify(config).getString("bitfinex.apiSecretKey");

        verify(config, times(2)).getString(anyString());

        assertEquals("API_KEY", apiKey);
        assertEquals("API_SECRET_KEY", apiSecretKey);
    }

}
