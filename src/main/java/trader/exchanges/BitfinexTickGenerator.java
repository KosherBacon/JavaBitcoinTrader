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

package trader.exchanges;

import eu.verdelhan.ta4j.Tick;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Contract;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.json.JSONArray;
import org.json.JSONException;
import trader.TickListener;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by jkahn on 12/25/15.
 *
 * @author Joshua Kahn
 */
public class BitfinexTickGenerator extends WebSocketClient {

    /**
     * The length of time (in seconds) for each tick.
     */
    private static final int TICK_LENGTH = 60;
    private static final Period TICK_TIME_PERIOD = Period.seconds(TICK_LENGTH);

    /**
     * A list of the different classes that implement TickListener which are
     * listening for new ticks.
     */
    private static final List<TickListener> LISTENERS = new ArrayList<>();

    /**
     * The tick to be fired (i.e. sent to all listeners) when ready.
     */
    private static Tick tickToFire;

    /**
     * The time (UNIX time seconds) that
     * {@link trader.exchanges.BitfinexTickGenerator this} started.
     */
    private static long tickStartTime;

    /**
     * The time (UNIX time seconds) {@code tickStartTime} plus {@code
     * TICK_LENGTH}.
     */
    private static long tickEndTime;

    public BitfinexTickGenerator(URI serverUri, Draft draft) {
        super(serverUri, draft);

        // Force the connection to use SSL
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null); // will use java's default key
            // and trust store which is sufficient unless you deal with
            // self-signed certificates
            this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory
                    (sslContext));
        } catch (Exception e) {
            e.printStackTrace();
            // Couldn't use the necessary SSL, close
            this.close();
            return;
        }

        tickStartTime = System.currentTimeMillis() / 1000L;
        tickEndTime = tickStartTime + TICK_LENGTH;
        tickToFire = new Tick(TICK_TIME_PERIOD, new DateTime(tickEndTime *
                1000L));

        TickUpdateTask task = new TickUpdateTask();
        Timer timer = new Timer();
        timer.schedule(task, 0, TICK_LENGTH * 1000L);
    }

    @Contract("null -> fail")
    public synchronized static void addListener(TickListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null.");
        }
        LISTENERS.add(listener);
    }

    @Contract("null -> fail")
    public synchronized static void removeListener(TickListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null.");
        }
        LISTENERS.remove(listener);
    }

    /**
     * Send a new tick to all listeners, this should be called at a fixed
     * time interval.
     */
    private synchronized static void fireNewTickEvent() {
        // Don't fire off ticks if there weren't any trades
        if (tickToFire.getTrades() != 0) {
            for (TickListener tickListener : LISTENERS) {
                tickListener.tickReceived(tickToFire);
            }
        } else {
            System.out.println("Tick was empty.");
        }
        tickStartTime = tickEndTime;
        tickEndTime += TICK_LENGTH;
        tickToFire = new Tick(TICK_TIME_PERIOD, new DateTime(tickEndTime *
                1000));
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        this.send("{ \"event\": \"subscribe\", \"channel\": \"trades\", " +
                "\"pair\": \"BTCUSD\" }");
    }

    @Override
    public void onMessage(String s) {
        if (s.length() > 0 && s.charAt(0) != '{') {
            try {
                JSONArray update = new JSONArray(s);
                if (update.length() >= 2 && !update.getString(1).equals("hb")) {

                    //System.out.println(s);

                    if (update.get(1) instanceof JSONArray) {
                        // Okay, so we know there are multiple trades
                        JSONArray trades = update.getJSONArray(1);
                        for (int i = 0; i < trades.length(); i++) {
                            JSONArray trade = trades.getJSONArray(i);
                            double price = trade.getDouble(2);
                            double amount = Math.abs(trade.getDouble(3));
                            tickToFire.addTrade(amount, price);
                        }
                    } else if (update.length() == 5) {
                        // There's only one trade to examine
                        double price = update.getDouble(3);
                        double amount = Math.abs(update.getDouble(4));
                        tickToFire.addTrade(amount, price);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
        this.close();
        this.connect();
    }

    /**
     * TickUpdateTask is a TimerTask that will be called at a fixed time
     * interval.
     * The task only calls for new tick to be generated.
     *
     * @author Joshua Kahn
     */
    class TickUpdateTask extends TimerTask {

        @Override
        public void run() {
            fireNewTickEvent();
        }

    }

}
