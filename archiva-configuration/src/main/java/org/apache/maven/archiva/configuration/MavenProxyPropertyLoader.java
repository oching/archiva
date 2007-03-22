package org.apache.maven.archiva.configuration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author Ben Walding
 * @author Brett Porter
 */
public class MavenProxyPropertyLoader
{
    private static final String REPO_LOCAL_STORE = "repo.local.store";

    private static final String PROXY_LIST = "proxy.list";

    private static final String REPO_LIST = "repo.list";

    public void load( Properties props, Configuration configuration ) throws InvalidConfigurationException
    {
        // set up the managed repository
        String localCachePath = getMandatoryProperty( props, REPO_LOCAL_STORE );

        RepositoryConfiguration config = new RepositoryConfiguration();
        config.setUrl( toURL( localCachePath ) );
        config.setName( "Imported Maven-Proxy Cache" );
        config.setId( "maven-proxy" );
        configuration.addRepository( config );

        // Add the network proxies.
        String propertyList = props.getProperty( PROXY_LIST );
        if ( propertyList != null )
        {
            StringTokenizer tok = new StringTokenizer( propertyList, "," );
            while ( tok.hasMoreTokens() )
            {
                String key = tok.nextToken();
                if ( StringUtils.isNotEmpty( key ) )
                {
                    ProxyConfiguration proxy = new ProxyConfiguration();
                    proxy.setHost( getMandatoryProperty( props, "proxy." + key + ".host" ) );
                    proxy.setPort( Integer.parseInt( getMandatoryProperty( props, "proxy." + key + ".port" ) ) );

                    // the username and password isn't required
                    proxy.setUsername( props.getProperty( "proxy." + key + ".username" ) );
                    proxy.setPassword( props.getProperty( "proxy." + key + ".password" ) );

                    configuration.addNetworkProxy( proxy );
                }
            }
        }

        // Add the remote repository list
        String repoList = getMandatoryProperty( props, REPO_LIST );

        StringTokenizer tok = new StringTokenizer( repoList, "," );
        while ( tok.hasMoreTokens() )
        {
            String key = tok.nextToken();

            Properties repoProps = getSubset( props, "repo." + key + "." );
            String url = getMandatoryProperty( props, "repo." + key + ".url" );
            String proxyKey = repoProps.getProperty( "proxy" );

            int cachePeriod = Integer.parseInt( repoProps.getProperty( "cache.period", "60" ) );

            RepositoryConfiguration repository = new RepositoryConfiguration();
            repository.setId( key );
            repository.setName( "Imported Maven-Proxy Remote Proxy" );
            repository.setUrl( url );
            repository.setLayout( "legacy" );
            repository.setIndexed( false );
            repository.setReleases( true );
            repository.setSnapshots( false );
            
            configuration.addRepository( repository );

            RepositoryProxyConnectorConfiguration proxyConnector = new RepositoryProxyConnectorConfiguration();
            proxyConnector.setSourceRepoId( "maven-proxy" );
            proxyConnector.setTargetRepoId( key );
            proxyConnector.setProxyId( proxyKey );
            proxyConnector.setFailurePolicy( RepositoryProxyConnectorConfiguration.NOT_FOUND );
            proxyConnector.setSnapshotsPolicy( String.valueOf( cachePeriod ) );
            proxyConnector.setReleasesPolicy( RepositoryProxyConnectorConfiguration.NEVER );
            
            configuration.addProxyConnector( proxyConnector );
        }
    }

    private String toURL( String path )
    {
        File file = new File( path );
        try
        {
            return file.toURL().toExternalForm();
        }
        catch ( MalformedURLException e )
        {
            return "file://" + StringUtils.replaceChars( file.getAbsolutePath(), '\\', '/' );
        }
    }

    private Properties getSubset( Properties props, String prefix )
    {
        Enumeration keys = props.keys();
        Properties result = new Properties();
        while ( keys.hasMoreElements() )
        {
            String key = (String) keys.nextElement();
            String value = props.getProperty( key );
            if ( key.startsWith( prefix ) )
            {
                String newKey = key.substring( prefix.length() );
                result.setProperty( newKey, value );
            }
        }
        return result;
    }

    public void load( InputStream is, Configuration configuration ) throws IOException, InvalidConfigurationException
    {
        Properties props = new Properties();
        props.load( is );
        load( props, configuration );
    }

    private String getMandatoryProperty( Properties props, String key ) throws InvalidConfigurationException
    {
        String value = props.getProperty( key );

        if ( value == null )
        {
            throw new InvalidConfigurationException( key, "Missing required field: " + key );
        }

        return value;
    }
}