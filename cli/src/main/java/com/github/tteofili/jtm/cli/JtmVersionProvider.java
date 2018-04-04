package com.github.tteofili.jtm.cli;

import picocli.CommandLine.IVersionProvider;

public final class JtmVersionProvider
    implements IVersionProvider
{

    @Override
    public String[] getVersion() throws Exception {
      return new String[]{
          "                         ''~``",
          "                        ( o o )",
          "+------------------.oooO--(_)--Oooo.------------------+",
          String.format( "%s v%s (built on %s)",
                         System.getProperty( "project.artifactId" ),
                         System.getProperty( "project.version" ),
                         System.getProperty( "build.timestamp" ) ),
          String.format( "Java version: %s, vendor: %s",
                      System.getProperty( "java.version" ),
                      System.getProperty( "java.vendor" ) ),
          String.format( "Java home: %s", System.getProperty( "java.home" ) ),
          String.format( "Default locale: %s_%s, platform encoding: %s",
                      System.getProperty( "user.language" ),
                      System.getProperty( "user.country" ),
                      System.getProperty( "sun.jnu.encoding" ) ),
          String.format( "OS name: \"%s\", version: \"%s\", arch: \"%s\", family: \"%s\"",
                      System.getProperty( "os.name" ),
                      System.getProperty( "os.version" ),
                      System.getProperty( "os.arch" ),
                      getOsFamily() ),
          "                     .oooO                            ",
          "                     (   )   Oooo.                    ",
          "+---------------------\\ (----(   )--------------------+",
          "                       \\_)    ) /",
          "                             (_/"
      };

    }

    private static final String getOsFamily() {
        String osName = System.getProperty( "os.name" ).toLowerCase();
        String pathSep = System.getProperty( "path.separator" );

        if ( osName.indexOf( "windows" ) != -1 )
        {
            return "windows";
        }
        else if ( osName.indexOf( "os/2" ) != -1 )
        {
            return "os/2";
        }
        else if ( osName.indexOf( "z/os" ) != -1 || osName.indexOf( "os/390" ) != -1 )
        {
            return "z/os";
        }
        else if ( osName.indexOf( "os/400" ) != -1 )
        {
            return "os/400";
        }
        else if ( pathSep.equals( ";" ) )
        {
            return "dos";
        }
        else if ( osName.indexOf( "mac" ) != -1 )
        {
            if ( osName.endsWith( "x" ) )
            {
                return "mac"; // MACOSX
            }
            return "unix";
        }
        else if ( osName.indexOf( "nonstop_kernel" ) != -1 )
        {
            return "tandem";
        }
        else if ( osName.indexOf( "openvms" ) != -1 )
        {
            return "openvms";
        }
        else if ( pathSep.equals( ":" ) )
        {
            return "unix";
        }

        return "undefined";
    }
}
