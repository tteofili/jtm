package com.github.tteofili.jtm.cli;

import java.util.Date;
import java.util.Formatter;

import org.slf4j.Logger;

final class ShutDownHook extends Thread {

    private final long start = System.currentTimeMillis();

    private final Logger log;

    public ShutDownHook(Logger log) {
        super("shutdown-hook");
        this.log = log;
    }

    public void run() {
        // format the uptime string

        Formatter uptime = new Formatter();
        uptime.format( "Total uptime:" );

        long uptimeInSeconds = ( System.currentTimeMillis() - start ) / 1000;
        final long hours = uptimeInSeconds / 3600;

        if ( hours > 0 )
        {
            uptime.format( " %s hour%s", hours, ( hours > 1 ? "s" : "" ) );
        }

        uptimeInSeconds = uptimeInSeconds - ( hours * 3600 );
        final long minutes = uptimeInSeconds / 60;

        if ( minutes > 0 )
        {
            uptime.format( " %s minute%s", minutes, ( minutes > 1 ? "s" : "" ) );
        }

        uptimeInSeconds = uptimeInSeconds - ( minutes * 60 );

        if ( uptimeInSeconds > 0 )
        {
            uptime.format( " %s second%s", uptimeInSeconds, ( uptimeInSeconds > 1 ? "s" : "" ) );
        }

        log.info( uptime.toString() );
        uptime.close();

        log.info( "Finished at: {}", new Date() );

        final Runtime runtime = Runtime.getRuntime();
        final int megaUnit = 1024 * 1024;
        log.info( "Final Memory: {}M/{}M",
                     ( runtime.totalMemory() - runtime.freeMemory() ) / megaUnit,
                     runtime.totalMemory() / megaUnit );

        log.info( "                     .oooO                            " );
        log.info( "                     (   )   Oooo.                    " );
        log.info( "+---------------------\\ (----(   )--------------------+" );
        log.info( "                       \\_)    ) /" );
        log.info( "                             (_/" );
    }

}
