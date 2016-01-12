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

package trader.exchanges.backtest;

import com.opencsv.CSVReader;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Period;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jkahn on 12/21/15.
 *
 * @author Joshua Kahn
 */
public class BacktestLoader {

    private static final int SECONDS_PER_TICK = 120;

    public static TimeSeries loadSeries(String fileName) {

        // Reading all lines of the CSV file
        InputStream stream = BacktestLoader.class.getResourceAsStream
                ("/csv_data/" + fileName);
        CSVReader csvReader = null;
        List<String[]> lines = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(stream, Charset
                    .forName("UTF-8")), ',');
            /*
             * This call can use an absurd amount of memory for large csv
             * files. It can cause an OutOfMemoryError on csv files of only a
             * few hundred MB.
             *
             * CONSIDER YESELF WARNED!
             */
            lines = csvReader.readAll();
            lines.remove(0); // Removing header line
        } catch (IOException ioe) {
            Logger.getLogger(BacktestLoader.class.getName()).log(Level
                    .SEVERE, "Unable to load trades from CSV", ioe);
        } finally {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException ioe) {
                }
            }
        }

        List<Tick> ticks = null;
        if ((lines != null) && !lines.isEmpty()) {

            // Getting the first and last trades timestamps
            DateTime beginTime = new DateTime(Long.parseLong(lines.get(0)[0])
                    * 1000);
            DateTime endTime = new DateTime(Long.parseLong(lines.get(lines
                    .size() - 1)[0]) * 1000);
            if (beginTime.isAfter(endTime)) {
                Instant beginInstant = beginTime.toInstant();
                Instant endInstant = endTime.toInstant();
                beginTime = new DateTime(endInstant);
                endTime = new DateTime(beginInstant);
            }

            // Building the empty ticks (every 300 seconds, yeah welcome in
            // Bitcoin world)
            ticks = buildEmptyTicks(beginTime, endTime, SECONDS_PER_TICK);
            // Filling the ticks with trades
            int beginTimeInt = (int) (beginTime.toDate().getTime() / 1000);
            for (String[] tradeLine : lines) {
                DateTime tradeTimestamp = new DateTime(Long.parseLong
                        (tradeLine[0]) * 1000);
                int index = (int) ((tradeTimestamp.toDate().getTime() / 1000)
                        - beginTimeInt) / SECONDS_PER_TICK;
                double tradePrice = Double.parseDouble(tradeLine[1]);
                double tradeAmount = Double.parseDouble(tradeLine[2]);
                ticks.get(index).addTrade(tradeAmount, tradePrice);
            }
            // Removing still empty ticks
            removeEmptyTicks(ticks);
        }

        return new TimeSeries(fileName, ticks);
    }

    private static List<Tick> buildEmptyTicks(DateTime beginTime, DateTime
            endTime, int duration) {

        List<Tick> emptyTicks = new ArrayList<>();

        Period tickTimePeriod = Period.seconds(duration);
        DateTime tickEndTime = beginTime;
        do {
            tickEndTime = tickEndTime.plus(tickTimePeriod);
            emptyTicks.add(new Tick(tickTimePeriod, tickEndTime));
        } while (tickEndTime.isBefore(endTime));

        return emptyTicks;
    }

    private static void removeEmptyTicks(List<Tick> ticks) {
        for (int i = ticks.size() - 1; i >= 0; i--) {
            if (ticks.get(i).getTrades() == 0) {
                ticks.remove(i);
            }
        }
    }

}
