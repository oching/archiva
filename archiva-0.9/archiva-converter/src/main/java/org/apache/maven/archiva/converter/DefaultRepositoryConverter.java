package org.apache.maven.archiva.converter;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.archiva.converter.transaction.FileTransaction;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.converter.ModelConverter;
import org.apache.maven.model.converter.PomTranslationException;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * Implementation of repository conversion class.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.archiva.converter.RepositoryConverter" role-hint="default"
 */
public class DefaultRepositoryConverter
    extends AbstractLogEnabled
    implements RepositoryConverter
{
    /**
     * {@link List}&lt;{@link Digester}>
     * 
     * @plexus.requirement role="org.codehaus.plexus.digest.Digester"
     */
    private List digesters;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private ModelConverter translator;

    /**
     * @plexus.requirement
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * @plexus.configuration default-value="false"
     */
    private boolean force;

    /**
     * @plexus.configuration default-value="false"
     */
    private boolean dryrun;

    /**
     * @plexus.requirement
     */
    private I18N i18n;

    private List listeners = new ArrayList();

    public void convert( Artifact artifact, ArtifactRepository targetRepository )
        throws RepositoryConversionException
    {
        if ( artifact.getRepository().getUrl().equals( targetRepository.getUrl() ) )
        {
            throw new RepositoryConversionException( getI18NString( "exception.repositories.match" ) );
        }

        if ( validateMetadata( artifact ) )
        {
            FileTransaction transaction = new FileTransaction();

            if ( copyPom( artifact, targetRepository, transaction ) )
            {
                if ( copyArtifact( artifact, targetRepository, transaction ) )
                {
                    Metadata metadata = createBaseMetadata( artifact );
                    Versioning versioning = new Versioning();
                    versioning.addVersion( artifact.getBaseVersion() );
                    metadata.setVersioning( versioning );
                    updateMetadata( new ArtifactRepositoryMetadata( artifact ), targetRepository, metadata, transaction );

                    metadata = createBaseMetadata( artifact );
                    metadata.setVersion( artifact.getBaseVersion() );
                    versioning = new Versioning();

                    Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
                    if ( matcher.matches() )
                    {
                        Snapshot snapshot = new Snapshot();
                        snapshot.setBuildNumber( Integer.valueOf( matcher.group( 3 ) ).intValue() );
                        snapshot.setTimestamp( matcher.group( 2 ) );
                        versioning.setSnapshot( snapshot );
                    }

                    // TODO: merge latest/release/snapshot from source instead
                    metadata.setVersioning( versioning );
                    updateMetadata( new SnapshotArtifactRepositoryMetadata( artifact ), targetRepository, metadata,
                                    transaction );

                    if ( !dryrun )
                    {
                        transaction.commit();
                    }
                }
            }
        }
    }

    private static Metadata createBaseMetadata( Artifact artifact )
    {
        Metadata metadata = new Metadata();
        metadata.setArtifactId( artifact.getArtifactId() );
        metadata.setGroupId( artifact.getGroupId() );
        return metadata;
    }

    private void updateMetadata( RepositoryMetadata artifactMetadata, ArtifactRepository targetRepository,
                                 Metadata newMetadata, FileTransaction transaction )
        throws RepositoryConversionException
    {
        File file = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );

        Metadata metadata;
        boolean changed;

        if ( file.exists() )
        {
            metadata = readMetadata( file );
            changed = metadata.merge( newMetadata );
        }
        else
        {
            changed = true;
            metadata = newMetadata;
        }

        if ( changed )
        {
            StringWriter writer = null;
            try
            {
                writer = new StringWriter();

                MetadataXpp3Writer mappingWriter = new MetadataXpp3Writer();

                mappingWriter.write( writer, metadata );

                transaction.createFile( writer.toString(), file, digesters );
            }
            catch ( IOException e )
            {
                throw new RepositoryConversionException( "Error writing target metadata", e );
            }
            finally
            {
                IOUtils.closeQuietly( writer );
            }
        }
    }

    private Metadata readMetadata( File file )
        throws RepositoryConversionException
    {
        Metadata metadata;
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader( file );
            metadata = reader.read( fileReader );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryConversionException( "Error reading target metadata", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryConversionException( "Error reading target metadata", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryConversionException( "Error reading target metadata", e );
        }
        finally
        {
            IOUtils.closeQuietly( fileReader );
        }
        return metadata;
    }

    private boolean validateMetadata( Artifact artifact )
        throws RepositoryConversionException
    {
        ArtifactRepository repository = artifact.getRepository();

        boolean result = true;

        RepositoryMetadata repositoryMetadata = new ArtifactRepositoryMetadata( artifact );
        File file = new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repositoryMetadata ) );
        if ( file.exists() )
        {
            Metadata metadata = readMetadata( file );
            result = validateMetadata( metadata, repositoryMetadata, artifact );
        }

        repositoryMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        file = new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repositoryMetadata ) );
        if ( file.exists() )
        {
            Metadata metadata = readMetadata( file );
            result = result && validateMetadata( metadata, repositoryMetadata, artifact );
        }

        return result;
    }

    private boolean validateMetadata( Metadata metadata, RepositoryMetadata repositoryMetadata, Artifact artifact )
    {
        String groupIdKey;
        String artifactIdKey = null;
        String snapshotKey = null;
        String versionKey = null;
        String versionsKey = null;

        if ( repositoryMetadata.storedInGroupDirectory() )
        {
            groupIdKey = "failure.incorrect.groupMetadata.groupId";
        }
        else if ( repositoryMetadata.storedInArtifactVersionDirectory() )
        {
            groupIdKey = "failure.incorrect.snapshotMetadata.groupId";
            artifactIdKey = "failure.incorrect.snapshotMetadata.artifactId";
            versionKey = "failure.incorrect.snapshotMetadata.version";
            snapshotKey = "failure.incorrect.snapshotMetadata.snapshot";
        }
        else
        {
            groupIdKey = "failure.incorrect.artifactMetadata.groupId";
            artifactIdKey = "failure.incorrect.artifactMetadata.artifactId";
            versionsKey = "failure.incorrect.artifactMetadata.versions";
        }

        boolean result = true;

        if ( metadata.getGroupId() == null || !metadata.getGroupId().equals( artifact.getGroupId() ) )
        {
            addFailure( artifact, groupIdKey );
            result = false;
        }
        if ( !repositoryMetadata.storedInGroupDirectory() )
        {
            if ( metadata.getGroupId() == null || !metadata.getArtifactId().equals( artifact.getArtifactId() ) )
            {
                addFailure( artifact, artifactIdKey );
                result = false;
            }
            if ( !repositoryMetadata.storedInArtifactVersionDirectory() )
            {
                // artifact metadata

                boolean foundVersion = false;
                if ( metadata.getVersioning() != null )
                {
                    for ( Iterator i = metadata.getVersioning().getVersions().iterator(); i.hasNext() && !foundVersion; )
                    {
                        String version = (String) i.next();
                        if ( version.equals( artifact.getBaseVersion() ) )
                        {
                            foundVersion = true;
                        }
                    }
                }

                if ( !foundVersion )
                {
                    addFailure( artifact, versionsKey );
                    result = false;
                }
            }
            else
            {
                // snapshot metadata
                if ( !artifact.getBaseVersion().equals( metadata.getVersion() ) )
                {
                    addFailure( artifact, versionKey );
                    result = false;
                }

                if ( artifact.isSnapshot() )
                {
                    Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
                    if ( matcher.matches() )
                    {
                        boolean correct = false;
                        if ( metadata.getVersioning() != null && metadata.getVersioning().getSnapshot() != null )
                        {
                            Snapshot snapshot = metadata.getVersioning().getSnapshot();
                            int build = Integer.valueOf( matcher.group( 3 ) ).intValue();
                            String ts = matcher.group( 2 );
                            if ( build == snapshot.getBuildNumber() && ts.equals( snapshot.getTimestamp() ) )
                            {
                                correct = true;
                            }
                        }

                        if ( !correct )
                        {
                            addFailure( artifact, snapshotKey );
                            result = false;
                        }
                    }
                }
            }
        }
        return result;
    }

    private void addFailure( Artifact artifact, String key )
    {
        addFailureWithReason( artifact, getI18NString( key ) );
    }

    private void addWarning( Artifact artifact, String message )
    {
        // TODO: should we be able to identify/fix these?
        // TODO: write archiva-artifact-repair module
        triggerConversionEvent( new ConversionEvent( artifact.getRepository(), ConversionEvent.WARNING, artifact,
                                                     message ) );
    }

    private void addFailureWithReason( Artifact artifact, String reason )
    {
        // TODO: should we be able to identify/fix these?
        triggerConversionEvent( new ConversionEvent( artifact.getRepository(), ConversionEvent.ERROR, artifact, reason ) );
    }

    private boolean copyPom( Artifact artifact, ArtifactRepository targetRepository, FileTransaction transaction )
        throws RepositoryConversionException
    {
        Artifact pom = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact
            .getVersion() );
        pom.setBaseVersion( artifact.getBaseVersion() );
        ArtifactRepository repository = artifact.getRepository();
        File file = new File( repository.getBasedir(), repository.pathOf( pom ) );

        boolean result = true;
        if ( file.exists() )
        {
            File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pom ) );

            String contents = null;
            boolean checksumsValid = false;
            try
            {
                if ( testChecksums( artifact, file ) )
                {
                    checksumsValid = true;
                }

                // Even if the checksums for the POM are invalid we should still convert the POM
                contents = FileUtils.readFileToString( file, null );
            }
            catch ( IOException e )
            {
                throw new RepositoryConversionException( "Unable to read source POM: " + e.getMessage(), e );
            }

            if ( checksumsValid && contents.indexOf( "modelVersion" ) >= 0 )
            {
                // v4 POM
                try
                {
                    boolean matching = false;
                    if ( !force && targetFile.exists() )
                    {
                        String targetContents = FileUtils.readFileToString( targetFile, null );
                        matching = targetContents.equals( contents );
                    }
                    if ( force || !matching )
                    {
                        transaction.createFile( contents, targetFile, digesters );
                    }
                }
                catch ( IOException e )
                {
                    throw new RepositoryConversionException( "Unable to write target POM: " + e.getMessage(), e );
                }
            }
            else
            {
                // v3 POM
                StringReader stringReader = new StringReader( contents );
                StringWriter writer = null;
                try
                {
                    MavenXpp3Reader v3Reader = new MavenXpp3Reader();
                    org.apache.maven.model.v3_0_0.Model v3Model = v3Reader.read( stringReader );

                    if ( doRelocation( artifact, v3Model, targetRepository, transaction ) )
                    {
                        Artifact relocatedPom = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact
                            .getArtifactId(), artifact.getVersion() );
                        targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( relocatedPom ) );
                    }

                    Model v4Model = translator.translate( v3Model );

                    translator.validateV4Basics( v4Model, v3Model.getGroupId(), v3Model.getArtifactId(), v3Model
                        .getVersion(), v3Model.getPackage() );

                    writer = new StringWriter();
                    MavenXpp3Writer Xpp3Writer = new MavenXpp3Writer();
                    Xpp3Writer.write( writer, v4Model );

                    transaction.createFile( writer.toString(), targetFile, digesters );

                    List warnings = translator.getWarnings();

                    for ( Iterator i = warnings.iterator(); i.hasNext(); )
                    {
                        String message = (String) i.next();
                        addWarning( artifact, message );
                    }
                }
                catch ( XmlPullParserException e )
                {
                    addFailureWithReason( artifact, getI18NString( "failure.invalid.source.pom", e.getMessage() ) );
                    result = false;
                }
                catch ( IOException e )
                {
                    throw new RepositoryConversionException( "Unable to write converted POM", e );
                }
                catch ( PomTranslationException e )
                {
                    addFailureWithReason( artifact, getI18NString( "failure.invalid.source.pom", e.getMessage() ) );
                    result = false;
                }
                finally
                {
                    IOUtils.closeQuietly( writer );
                }
            }
        }
        else
        {
            addWarning( artifact, getI18NString( "warning.missing.pom" ) );
        }
        return result;
    }

    private boolean doRelocation( Artifact artifact, org.apache.maven.model.v3_0_0.Model v3Model,
                                  ArtifactRepository repository, FileTransaction transaction )
        throws IOException
    {
        Properties properties = v3Model.getProperties();
        if ( properties.containsKey( "relocated.groupId" ) || properties.containsKey( "relocated.artifactId" )
            || properties.containsKey( "relocated.version" ) )
        {
            String newGroupId = properties.getProperty( "relocated.groupId", v3Model.getGroupId() );
            properties.remove( "relocated.groupId" );

            String newArtifactId = properties.getProperty( "relocated.artifactId", v3Model.getArtifactId() );
            properties.remove( "relocated.artifactId" );

            String newVersion = properties.getProperty( "relocated.version", v3Model.getVersion() );
            properties.remove( "relocated.version" );

            String message = properties.getProperty( "relocated.message", "" );
            properties.remove( "relocated.message" );

            if ( properties.isEmpty() )
            {
                v3Model.setProperties( null );
            }

            writeRelocationPom( v3Model.getGroupId(), v3Model.getArtifactId(), v3Model.getVersion(), newGroupId,
                                newArtifactId, newVersion, message, repository, transaction );

            v3Model.setGroupId( newGroupId );
            v3Model.setArtifactId( newArtifactId );
            v3Model.setVersion( newVersion );

            artifact.setGroupId( newGroupId );
            artifact.setArtifactId( newArtifactId );
            artifact.setVersion( newVersion );

            return true;
        }
        else
        {
            return false;
        }
    }

    private void writeRelocationPom( String groupId, String artifactId, String version, String newGroupId,
                                     String newArtifactId, String newVersion, String message,
                                     ArtifactRepository repository, FileTransaction transaction )
        throws IOException
    {
        Model pom = new Model();
        pom.setGroupId( groupId );
        pom.setArtifactId( artifactId );
        pom.setVersion( version );

        DistributionManagement dMngt = new DistributionManagement();

        Relocation relocation = new Relocation();
        relocation.setGroupId( newGroupId );
        relocation.setArtifactId( newArtifactId );
        relocation.setVersion( newVersion );
        if ( message != null && message.length() > 0 )
        {
            relocation.setMessage( message );
        }

        dMngt.setRelocation( relocation );

        pom.setDistributionManagement( dMngt );

        Artifact artifact = artifactFactory.createBuildArtifact( groupId, artifactId, version, "pom" );
        File pomFile = new File( repository.getBasedir(), repository.pathOf( artifact ) );

        StringWriter strWriter = new StringWriter();
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        pomWriter.write( strWriter, pom );

        transaction.createFile( strWriter.toString(), pomFile, digesters );
    }

    private String getI18NString( String key, String arg0 )
    {
        return i18n.format( getClass().getName(), Locale.getDefault(), key, arg0 );
    }

    private String getI18NString( String key )
    {
        return i18n.getString( getClass().getName(), Locale.getDefault(), key );
    }

    private boolean testChecksums( Artifact artifact, File file )
        throws IOException
    {
        boolean result = true;
        Iterator it = digesters.iterator();
        while ( it.hasNext() )
        {
            Digester digester = (Digester) it.next();
            result &= verifyChecksum( file, file.getName() + "." + getDigesterFileExtension( digester ), digester,
                                      artifact, "failure.incorrect." + getDigesterFileExtension( digester ) );
        }
        return result;
    }

    /**
     * File extension for checksums
     * TODO should be moved to plexus-digester ?
     */
    private String getDigesterFileExtension( Digester digester )
    {
        return digester.getAlgorithm().toLowerCase().replaceAll( "-", "" );
    }

    private boolean verifyChecksum( File file, String fileName, Digester digester, Artifact artifact, String key )
        throws IOException
    {
        boolean result = true;

        File checksumFile = new File( file.getParentFile(), fileName );
        if ( checksumFile.exists() )
        {
            String checksum = FileUtils.readFileToString( checksumFile, null );
            try
            {
                digester.verify( file, checksum );
            }
            catch ( DigesterException e )
            {
                addFailure( artifact, key );
                result = false;
            }
        }
        return result;
    }

    private boolean copyArtifact( Artifact artifact, ArtifactRepository targetRepository, FileTransaction transaction )
        throws RepositoryConversionException
    {
        File sourceFile = artifact.getFile();

        if ( sourceFile.getAbsolutePath().indexOf( "/plugins/" ) > -1 )
        {
            artifact.setArtifactHandler( artifactHandlerManager.getArtifactHandler( "maven-plugin" ) );
        }

        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );

        boolean result = true;
        try
        {
            boolean matching = false;
            if ( !force && targetFile.exists() )
            {
                matching = FileUtils.contentEquals( sourceFile, targetFile );
                if ( !matching )
                {
                    addFailure( artifact, "failure.target.already.exists" );
                    result = false;
                }
            }
            if ( result )
            {
                if ( force || !matching )
                {
                    if ( testChecksums( artifact, sourceFile ) )
                    {
                        transaction.copyFile( sourceFile, targetFile, digesters );
                    }
                    else
                    {
                        result = false;
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryConversionException( "Error copying artifact", e );
        }
        return result;
    }

    public void convert( List artifacts, ArtifactRepository targetRepository )
        throws RepositoryConversionException
    {
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            try
            {
                convert( artifact, targetRepository );
            }
            catch ( RepositoryConversionException e )
            {
                triggerConversionEvent( new ConversionEvent( targetRepository, ConversionEvent.ERROR, artifact, e ) );
            }
        }
    }

    /**
     * Add a listener to the conversion process.
     * 
     * @param listener the listener to add.
     */
    public void addConversionListener( ConversionListener listener )
    {
        listeners.add( listener );
    }

    /**
     * Remove a listener from the conversion process.
     * 
     * @param listener the listener to remove.
     */
    public void removeConversionListener( ConversionListener listener )
    {
        listeners.remove( listener );
    }

    private void triggerConversionEvent( ConversionEvent event )
    {
        Iterator it = listeners.iterator();
        while ( it.hasNext() )
        {
            ConversionListener listener = (ConversionListener) it.next();

            try
            {
                listener.conversionEvent( event );
            }
            catch ( Throwable t )
            {
                getLogger().warn( "ConversionEvent resulted in exception from listener: " + t.getMessage(), t );
            }
        }
    }
}