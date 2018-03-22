package com.github.tteofili.jtm.cli;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tteofili.jtm.JiraAnalysisTool;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "jtm",
    descriptionHeading = "Tool for analyzing Atlassian Jira issues exported to the XML feed format\n",
    description = "This tool does topic modelling based on word2vec and paragraph vectors",
    versionProvider = JtmVersionProvider.class
)
public class JtmCommand
{

    private static final Logger log = LoggerFactory.getLogger(JtmCommand.class);

    @Parameters(index = "0", description = "Exported JIRA XML feed file.", arity = "1")
    private File exportedJiraFeed;

    @Option(names = { "-e", "--epochs" }, description = "Epochs.")
    private int epochs = 5;

    @Option(names = { "-l", "--layer-size" }, description = "Layers.")
    private int layerSize = 200;

    @Option(names = { "-t", "--top-n" }, description = "Top.")
    private int topN = 5;

    @Option(names = { "-v", "--hierarchical-vectors" }, description = "Hierarchical vectors.")
    private boolean hierarchicalVectors = false;

    @Option(names = { "-c", "--include-comments" }, description = "Include comments.")
    private boolean includeComments = true;

    @Option(names = { "-i", "--index" }, description = "Index.")
    private boolean index = false;

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display the usage message.")
    private boolean helpRequested = false;

    @Option(names = { "-V", "--version" }, versionHelp = true, description = "Display version info.")
    private boolean versionInfoRequested = false;

    @Option(names = { "-X", "--verbose" }, description = "Produce execution debug output.")
    private boolean verbose = false;

    @Option( names = { "-q", "--quiet" }, description = "Log errors only." )
    private boolean quiet = false;

    public static void main( String[] args )
    {
        /* exit statuses:
         * -1: error
         *  0: info
         *  1: success
         */
        JtmCommand command = new JtmCommand();
        CommandLine commandLine = new CommandLine(command);

        try {
            commandLine.parse(args);
        } catch (Throwable t) {
            System.err.println( t.getMessage() );
            System.exit( -1 );
        }

        if (commandLine.isUsageHelpRequested()) {
           commandLine.usage(System.out);
           System.exit( 0 );
        } else if (commandLine.isVersionHelpRequested()) {
           commandLine.printVersionHelp(System.out);
           System.exit( 0 );
        }

        Runtime.getRuntime().addShutdownHook( new ShutDownHook(log) );

        // setup the logging stuff

        if ( command.quiet )
        {
            System.setProperty( "logging.level", "ERROR" );
        }
        else if ( command.verbose )
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

        log.info( "                         ''~``" );
        log.info( "                        ( o o )" );
        log.info( "+------------------.oooO--(_)--Oooo.------------------+" );
        log.info( "{} v{}", new Object[]{ System.getProperty( "app.name" ), System.getProperty( "project.version" ) } );
        log.info( "+-----------------------------------------------------+" );
        log.info( "" );

        int status = 1;
        Throwable error = null;
        try {
            new JiraAnalysisTool( command.exportedJiraFeed,
                                  command.epochs,
                                  command.layerSize,
                                  command.topN,
                                  command.hierarchicalVectors,
                                  command.includeComments,
                                  command.index ).execute();
        } catch (Throwable t) {
            status = -1;
            error = t;
        }

        log.info( "+-----------------------------------------------------+" );
        log.info( "{} {}", System.getProperty( "app.name" ).toUpperCase(), ( status < 0 ) ? "FAILURE" : "SUCCESS" );
        log.info( "+-----------------------------------------------------+" );

        if ( status < 0 )
        {
            if ( command.verbose )
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

}
