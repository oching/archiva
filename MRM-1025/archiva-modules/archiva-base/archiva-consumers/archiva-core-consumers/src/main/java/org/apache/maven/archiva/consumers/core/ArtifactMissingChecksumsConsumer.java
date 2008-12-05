package org.apache.maven.archiva.consumers.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.codehaus.plexus.digest.ChecksumFile;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ArtifactMissingChecksumsConsumer - Create missing checksums for the artifact.
 *
 * @version $Id$
 */
public class ArtifactMissingChecksumsConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener, Initializable
{
    private String id;

    private String description;

    private ArchivaConfiguration configuration;

    private FileTypes filetypes;

    private Digester digestSha1;

    private Digester digestMd5;

    private ChecksumFile checksum;

    private static final String TYPE_CHECKSUM_NOT_FILE = "checksum-bad-not-file";

    private static final String TYPE_CHECKSUM_CANNOT_CALC = "checksum-calc-failure";

    private static final String TYPE_CHECKSUM_CANNOT_CREATE = "checksum-create-failure";

    private File repositoryDir;
    
    private List<String> includes = new ArrayList<String>();

    public ArtifactMissingChecksumsConsumer(String id,
            String description,
            ArchivaConfiguration configuration,
            FileTypes filetypes,
            Digester digestSha1,
            Digester digestMd5,
            ChecksumFile checksum) {
        this.id = id;
        this.description = description;
        this.configuration = configuration;
        this.filetypes = filetypes;
        this.digestSha1 = digestSha1;
        this.digestMd5 = digestMd5;
        this.checksum = checksum;
    }

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan( ManagedRepositoryConfiguration repo, Date whenGathered )
        throws ConsumerException
    {
        this.repositoryDir = new File( repo.getLocation() );
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List<String> getExcludes()
    {
        return getDefaultArtifactExclusions();
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        createIfMissing( path, digestSha1 );
        createIfMissing( path, digestMd5 );
    }

    private void createIfMissing( String path, Digester digester )
    {
        File checksumFile = new File( this.repositoryDir, path + digester.getFilenameExtension() );
        if ( !checksumFile.exists() )
        {
            try
            {
                checksum.createChecksum( new File( this.repositoryDir, path ), digester );
                triggerConsumerInfo( "Created missing checksum file " + checksumFile.getAbsolutePath() );
            }
            catch ( DigesterException e )
            {
                triggerConsumerError( TYPE_CHECKSUM_CANNOT_CALC,
                                      "Cannot calculate checksum for file " + checksumFile + ": " + e.getMessage() );
            }
            catch ( IOException e )
            {
                triggerConsumerError( TYPE_CHECKSUM_CANNOT_CREATE,
                                      "Cannot create checksum for file " + checksumFile + ": " + e.getMessage() );
            }
        }
        else if ( !checksumFile.isFile() )
        {
            triggerConsumerWarning( TYPE_CHECKSUM_NOT_FILE,
                                    "Checksum file " + checksumFile.getAbsolutePath() + " is not a file." );
        }

    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {             
        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        includes.clear();

        includes.addAll( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
    }

    public void initialize()
        throws InitializationException
    {
        configuration.addChangeListener( this );
        
        initIncludes();
    }
}