package com.sailorgeoffrey.pinger;

import io.advantageous.qbit.spring.annotation.AutoFlush;
import io.advantageous.qbit.spring.annotation.EnableQBit;
import io.advantageous.qbit.spring.annotation.QBitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Pings sites at a set interval and reports stats.
 *
 * @author gcc@rd.io (Geoff Chandler)
 */
@EnableQBit
@SpringBootApplication
public class Pinger {

    private final Logger logger = LoggerFactory.getLogger(Pinger.class);

    public static void main(String[] args) {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
        SpringApplication.run(Pinger.class);
    }

    @Bean
    public CommandLineRunner runner(final ReporterServiceAsync reporterService) {
        return args -> {
            try {
                final Process p = Runtime.getRuntime().exec("ping google.com");
                final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String mutableLine;
                while ((mutableLine = reader.readLine()) != null) {
                    reporterService.reportLine(mutableLine);
                }
                p.wait(100);
            } catch (IOException e) {
                logger.error("IOException thrown while running command.", e);
            }
        };
    }

    @Bean
    @AutoFlush
    @QBitService(asyncInterface = ReporterServiceAsync.class)
    public Object reporterService() {
        return (ReporterServiceAsync) line -> {
            final int pos = line.lastIndexOf("time=");
            if (pos > 0) {
                final float ping = Float.parseFloat(line.substring(pos + 5, line.length() - 3));
                logger.info("Time: " + ping);
            }
        };
    }

    private interface ReporterServiceAsync {
        void reportLine(String line);
    }

}
