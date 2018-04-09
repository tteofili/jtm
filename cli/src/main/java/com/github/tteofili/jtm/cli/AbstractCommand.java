/*
 * Copyright 2018 Tommaso Teofili and Simone Tripodi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.tteofili.jtm.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tteofili.jtm.AnalysisTool;
import com.github.tteofili.jtm.JiraAnalysisTool;
import com.github.tteofili.jtm.feed.jira.Feed;
import com.github.tteofili.jtm.feed.jira.io.stax.JiraFeedStaxReader;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

abstract class AbstractCommand implements Runnable, AnalysisTool {

    protected static final Logger log = LoggerFactory.getLogger(AbstractCommand.class);

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display the usage message.")
    private boolean helpRequested = false;

    @Option(names = { "-V", "--version" }, versionHelp = true, description = "Display version info.")
    private boolean versionInfoRequested = false;

    @Option(names = { "-X", "--verbose" }, description = "Produce execution debug output.")
    private boolean verbose = false;

    @Option( names = { "-q", "--quiet" }, description = "Log errors only." )
    private boolean quiet = false;

    @Parameters(index = "0", description = "Exported JIRA XML feed file(s).", arity = "*")
    private File[] exportedJiraFeeds;

    @Override
    public void run() {
        /*
         * exit statuses:
         * -1: error
         *  0: info
         *  1: success
         */

        Runtime.getRuntime().addShutdownHook( new ShutDownHook(log) );

        // setup the logging stuff

        if ( quiet )
        {
            System.setProperty( "logging.level", "ERROR" );
        }
        else if ( verbose )
        {
            System.setProperty( "logging.level", "DEBUG" );
        }
        else
        {
            System.setProperty( "logging.level", "INFO" );
        }

        // assume SLF4J is bound to logback in the current environment
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        try
        {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext( lc );
            // the context was probably already configured by default configuration
            // rules
            lc.reset();
            configurator.doConfigure( JiraAnalysisTool.class.getClassLoader().getResourceAsStream( "logback-config.xml" ) );
        }
        catch ( JoranException je )
        {
            // StatusPrinter should handle this
        }

        // GO!!!

        log.info( "                         ''~``" );
        log.info( "                        ( o o )" );
        log.info( "+------------------.oooO--(_)--Oooo.------------------+" );
        log.info( "{} v{}", new Object[]{ System.getProperty( "app.name" ), System.getProperty( "project.version" ) } );
        log.info( "+-----------------------------------------------------+" );
        log.info( "" );

        final JiraFeedStaxReader feedReader = new JiraFeedStaxReader();

        int status = 1;
        Throwable error = null;
        InputStream input = null;
        Feed feed = null;

        try {
            setUp();

            for (File exportedJiraFeed : exportedJiraFeeds) {
                input = new FileInputStream(exportedJiraFeed);
                feed = feedReader.read(input, false);

                analyze(feed);
            }

            tearDown();
        } catch (Throwable t) {
            status = -1;
            error = t;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // nothing to do, swallow it
                }
            }
        }

        log.info( "+-----------------------------------------------------+" );
        log.info( "{} {}", System.getProperty( "app.name" ).toUpperCase(), ( status < 0 ) ? "FAILURE" : "SUCCESS" );
        log.info( "+-----------------------------------------------------+" );

        if ( status < 0 )
        {
            if ( verbose )
            {
                log.error( "Execution terminated with errors", error );
            }
            else
            {
                log.error( "Execution terminated with errors: {}", error.getMessage() );
            }

            log.info( "+-----------------------------------------------------+" );
        }
    }

    protected void setUp() throws Exception {
        // do nothing by default
    }

    protected void tearDown() throws Exception {
        // do nothing by default
    }

}
