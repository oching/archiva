package org.apache.archiva.metadata.repository.storage.maven2;

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

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.RepositoryStorage;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataInvalidException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataNotFoundException;
import org.apache.maven.archiva.common.proxy.WagonFactory;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maven 2 repository format storage implementation. This class currently takes parameters to indicate the repository to
 * deal with rather than being instantiated per-repository.
 * FIXME: instantiate one per repository and allocate permanently from a factory (which can be obtained within the session).
 * TODO: finish Maven 1 implementation to prove this API
 *
 * The session is passed in as an argument to obtain any necessary resources, rather than the class being instantiated
 * within the session in the context of a single managed repository's resolution needs.
 *
 * @plexus.component role="org.apache.archiva.metadata.repository.storage.RepositoryStorage" role-hint="maven2"
 */
public class Maven2RepositoryStorage
    implements RepositoryStorage
{
    /**
     * @plexus.requirement
     */
    private ModelBuilder builder;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role-hint="maven2"
     */
    private RepositoryPathTranslator pathTranslator;

    /**
     * @plexus.requirement
     */
    private WagonFactory wagonFactory;

    private final static Logger log = LoggerFactory.getLogger( Maven2RepositoryStorage.class );

    private static final String METADATA_FILENAME = "maven-metadata.xml";

    public ProjectMetadata readProjectMetadata( String repoId, String namespace, String projectId )
    {
        // TODO: could natively implement the "shared model" concept from the browse action to avoid needing it there?
        return null;
    }

    public ProjectVersionMetadata readProjectVersionMetadata( String repoId, String namespace, String projectId,
                                                              String projectVersion )
        throws RepositoryStorageMetadataNotFoundException, RepositoryStorageMetadataInvalidException
    {
        ManagedRepositoryConfiguration repositoryConfiguration =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repoId );

        String artifactVersion = projectVersion;

        File basedir = new File( repositoryConfiguration.getLocation() );
        if ( VersionUtil.isSnapshot( projectVersion ) )
        {
            File metadataFile = pathTranslator.toFile( basedir, namespace, projectId, projectVersion,
                                                       METADATA_FILENAME );
            try
            {
                MavenRepositoryMetadata metadata = MavenRepositoryMetadataReader.read( metadataFile );

                // re-adjust to timestamp if present, otherwise retain the original -SNAPSHOT filename
                MavenRepositoryMetadata.Snapshot snapshotVersion = metadata.getSnapshotVersion();
                if ( snapshotVersion != null )
                {
                    artifactVersion = artifactVersion.substring( 0, artifactVersion.length() -
                        8 ); // remove SNAPSHOT from end
                    artifactVersion =
                        artifactVersion + snapshotVersion.getTimestamp() + "-" + snapshotVersion.getBuildNumber();
                }
            }
            catch ( XMLException e )
            {
                // unable to parse metadata - log it, and continue with the version as the original SNAPSHOT version
                log.warn( "Invalid metadata: " + metadataFile + " - " + e.getMessage() );
            }
        }

        // TODO: won't work well with some other layouts, might need to convert artifact parts to ID by path translator
        String id = projectId + "-" + artifactVersion + ".pom";
        File file = pathTranslator.toFile( basedir, namespace, projectId, projectVersion, id );

        if ( !file.exists() )
        {
            // metadata could not be resolved
            throw new RepositoryStorageMetadataNotFoundException(
                "The artifact's POM file '" + file.getAbsolutePath() + "' was missing" );
        }

        List<RemoteRepositoryConfiguration> remoteRepositories = new ArrayList<RemoteRepositoryConfiguration>();
        Map<String, ProxyInfo> networkProxies = new HashMap<String, ProxyInfo>();

        Map<String, List<ProxyConnectorConfiguration>> proxyConnectorsMap = archivaConfiguration.getConfiguration().getProxyConnectorAsMap();
        List<ProxyConnectorConfiguration> proxyConnectors = proxyConnectorsMap.get( repoId );
        if( proxyConnectors != null )
        {
            for( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
            {
                RemoteRepositoryConfiguration remoteRepoConfig = archivaConfiguration.getConfiguration().findRemoteRepositoryById(
                    proxyConnector.getTargetRepoId() );

                if( remoteRepoConfig != null )
                {
                    remoteRepositories.add( remoteRepoConfig );

                    NetworkProxyConfiguration networkProxyConfig = archivaConfiguration.getConfiguration().getNetworkProxiesAsMap().get(
                        proxyConnector.getProxyId() );

                    if( networkProxyConfig != null )
                    {
                        ProxyInfo proxy = new ProxyInfo();
                        proxy.setType( networkProxyConfig.getProtocol() );
                        proxy.setHost( networkProxyConfig.getHost() );
                        proxy.setPort( networkProxyConfig.getPort() );
                        proxy.setUserName( networkProxyConfig.getUsername() );
                        proxy.setPassword( networkProxyConfig.getPassword() );

                        // key/value: remote repo ID/proxy info
                        networkProxies.put( proxyConnector.getTargetRepoId(), proxy );
                    }
                }
            }
        }

        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins( false );
        req.setPomFile( file );

        // MRM-1411
        req.setModelResolver( new RepositoryModelResolver( basedir, pathTranslator, wagonFactory, remoteRepositories,
                                                           networkProxies, repositoryConfiguration ) );
        req.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );

        Model model;
        try
        {
            model = builder.build( req ).getEffectiveModel();
        }
        catch ( ModelBuildingException e )
        {
            String msg = "The artifact's POM file '" + file + "' was invalid: " + e.getMessage();

            List<ModelProblem> modelProblems = e.getProblems();
            for( ModelProblem problem : modelProblems )
            {
                // MRM-1411, related to MRM-1335
                // this means that the problem was that the parent wasn't resolved!
                if( problem.getException() instanceof FileNotFoundException && e.getModelId() != null && 
                    !e.getModelId().equals( problem.getModelId() ) )
                {
                    log.warn( "The artifact's parent POM file '" + file + "' cannot be resolved. " +
                        "Using defaults for project version metadata.." );

                    ProjectVersionMetadata metadata = new ProjectVersionMetadata();
                    metadata.setId( projectVersion );

                    MavenProjectFacet facet = new MavenProjectFacet();
                    facet.setGroupId( namespace );
                    facet.setArtifactId( projectId );
                    facet.setPackaging( "jar" );
                    metadata.addFacet( facet );

                    return metadata;
                }
            }

            throw new RepositoryStorageMetadataInvalidException( "invalid-pom", msg, e );
        }

        // Check if the POM is in the correct location
        boolean correctGroupId = namespace.equals( model.getGroupId() );
        boolean correctArtifactId = projectId.equals( model.getArtifactId() );
        boolean correctVersion = projectVersion.equals( model.getVersion() );
        if ( !correctGroupId || !correctArtifactId || !correctVersion )
        {
            StringBuilder message = new StringBuilder( "Incorrect POM coordinates in '" + file + "':" );
            if ( !correctGroupId )
            {
                message.append( "\nIncorrect group ID: " ).append( model.getGroupId() );
            }
            if ( !correctArtifactId )
            {
                message.append( "\nIncorrect artifact ID: " ).append( model.getArtifactId() );
            }
            if ( !correctVersion )
            {
                message.append( "\nIncorrect version: " ).append( model.getVersion() );
            }

            throw new RepositoryStorageMetadataInvalidException( "mislocated-pom", message.toString() );
        }

        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setCiManagement( convertCiManagement( model.getCiManagement() ) );
        metadata.setDescription( model.getDescription() );
        metadata.setId( projectVersion );
        metadata.setIssueManagement( convertIssueManagement( model.getIssueManagement() ) );
        metadata.setLicenses( convertLicenses( model.getLicenses() ) );
        metadata.setMailingLists( convertMailingLists( model.getMailingLists() ) );
        metadata.setDependencies( convertDependencies( model.getDependencies() ) );
        metadata.setName( model.getName() );
        metadata.setOrganization( convertOrganization( model.getOrganization() ) );
        metadata.setScm( convertScm( model.getScm() ) );
        metadata.setUrl( model.getUrl() );

        MavenProjectFacet facet = new MavenProjectFacet();
        facet.setGroupId( model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId() );
        facet.setArtifactId( model.getArtifactId() );
        facet.setPackaging( model.getPackaging() );
        if ( model.getParent() != null )
        {
            MavenProjectParent parent = new MavenProjectParent();
            parent.setGroupId( model.getParent().getGroupId() );
            parent.setArtifactId( model.getParent().getArtifactId() );
            parent.setVersion( model.getParent().getVersion() );
            facet.setParent( parent );
        }
        metadata.addFacet( facet );

        return metadata;
    }

    public void setWagonFactory( WagonFactory wagonFactory )
    {
        this.wagonFactory = wagonFactory;
    }

    private List<org.apache.archiva.metadata.model.Dependency> convertDependencies( List<Dependency> dependencies )
    {
        List<org.apache.archiva.metadata.model.Dependency> l =
            new ArrayList<org.apache.archiva.metadata.model.Dependency>();
        for ( Dependency dependency : dependencies )
        {
            org.apache.archiva.metadata.model.Dependency newDependency =
                new org.apache.archiva.metadata.model.Dependency();
            newDependency.setArtifactId( dependency.getArtifactId() );
            newDependency.setClassifier( dependency.getClassifier() );
            newDependency.setGroupId( dependency.getGroupId() );
            newDependency.setOptional( dependency.isOptional() );
            newDependency.setScope( dependency.getScope() );
            newDependency.setSystemPath( dependency.getSystemPath() );
            newDependency.setType( dependency.getType() );
            newDependency.setVersion( dependency.getVersion() );
            l.add( newDependency );
        }
        return l;
    }

    private org.apache.archiva.metadata.model.Scm convertScm( Scm scm )
    {
        org.apache.archiva.metadata.model.Scm newScm = null;
        if ( scm != null )
        {
            newScm = new org.apache.archiva.metadata.model.Scm();
            newScm.setConnection( scm.getConnection() );
            newScm.setDeveloperConnection( scm.getDeveloperConnection() );
            newScm.setUrl( scm.getUrl() );
        }
        return newScm;
    }

    private org.apache.archiva.metadata.model.Organization convertOrganization( Organization organization )
    {
        org.apache.archiva.metadata.model.Organization org = null;
        if ( organization != null )
        {
            org = new org.apache.archiva.metadata.model.Organization();
            org.setName( organization.getName() );
            org.setUrl( organization.getUrl() );
        }
        return org;
    }

    private List<org.apache.archiva.metadata.model.License> convertLicenses( List<License> licenses )
    {
        List<org.apache.archiva.metadata.model.License> l = new ArrayList<org.apache.archiva.metadata.model.License>();
        for ( License license : licenses )
        {
            org.apache.archiva.metadata.model.License newLicense = new org.apache.archiva.metadata.model.License();
            newLicense.setName( license.getName() );
            newLicense.setUrl( license.getUrl() );
            l.add( newLicense );
        }
        return l;
    }

    private List<org.apache.archiva.metadata.model.MailingList> convertMailingLists( List<MailingList> mailingLists )
    {
        List<org.apache.archiva.metadata.model.MailingList> l =
            new ArrayList<org.apache.archiva.metadata.model.MailingList>();
        for ( MailingList mailingList : mailingLists )
        {
            org.apache.archiva.metadata.model.MailingList newMailingList =
                new org.apache.archiva.metadata.model.MailingList();
            newMailingList.setName( mailingList.getName() );
            newMailingList.setMainArchiveUrl( mailingList.getArchive() );
            newMailingList.setPostAddress( mailingList.getPost() );
            newMailingList.setSubscribeAddress( mailingList.getSubscribe() );
            newMailingList.setUnsubscribeAddress( mailingList.getUnsubscribe() );
            newMailingList.setOtherArchives( mailingList.getOtherArchives() );
            l.add( newMailingList );
        }
        return l;
    }

    private org.apache.archiva.metadata.model.IssueManagement convertIssueManagement( IssueManagement issueManagement )
    {
        org.apache.archiva.metadata.model.IssueManagement im = null;
        if ( issueManagement != null )
        {
            im = new org.apache.archiva.metadata.model.IssueManagement();
            im.setSystem( issueManagement.getSystem() );
            im.setUrl( issueManagement.getUrl() );
        }
        return im;
    }

    private org.apache.archiva.metadata.model.CiManagement convertCiManagement( CiManagement ciManagement )
    {
        org.apache.archiva.metadata.model.CiManagement ci = null;
        if ( ciManagement != null )
        {
            ci = new org.apache.archiva.metadata.model.CiManagement();
            ci.setSystem( ciManagement.getSystem() );
            ci.setUrl( ciManagement.getUrl() );
        }
        return ci;
    }

    public Collection<String> listRootNamespaces( String repoId, Filter<String> filter )
    {
        File dir = getRepositoryBasedir( repoId );

        return getSortedFiles( dir, filter );
    }

    private static Collection<String> getSortedFiles( File dir, Filter<String> filter )
    {
        List<String> fileNames;
        String[] files = dir.list( new DirectoryFilter( filter ) );
        if ( files != null )
        {
            fileNames = new ArrayList<String>( Arrays.asList( files ) );
            Collections.sort( fileNames );
        }
        else
        {
            fileNames = Collections.emptyList();
        }
        return fileNames;
    }

    private File getRepositoryBasedir( String repoId )
    {
        ManagedRepositoryConfiguration repositoryConfiguration =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repoId );

        return new File( repositoryConfiguration.getLocation() );
    }

    public Collection<String> listNamespaces( String repoId, String namespace, Filter<String> filter )
    {
        File dir = pathTranslator.toFile( getRepositoryBasedir( repoId ), namespace );

        // scan all the directories which are potential namespaces. Any directories known to be projects are excluded
        List<String> namespaces = new ArrayList<String>();
        File[] files = dir.listFiles( new DirectoryFilter( filter ) );
        if ( files != null )
        {
            for ( File file : files )
            {
                if ( !isProject( file, filter ) )
                {
                    namespaces.add( file.getName() );
                }
            }
        }
        Collections.sort( namespaces );
        return namespaces;
    }

    public Collection<String> listProjects( String repoId, String namespace, Filter<String> filter )
    {
        File dir = pathTranslator.toFile( getRepositoryBasedir( repoId ), namespace );

        // scan all directories in the namespace, and only include those that are known to be projects
        List<String> projects = new ArrayList<String>();
        File[] files = dir.listFiles( new DirectoryFilter( filter ) );
        if ( files != null )
        {
            for ( File file : files )
            {
                if ( isProject( file, filter ) )
                {
                    projects.add( file.getName() );
                }
            }
        }
        Collections.sort( projects );
        return projects;
    }

    public Collection<String> listProjectVersions( String repoId, String namespace, String projectId,
                                                   Filter<String> filter )
    {
        File dir = pathTranslator.toFile( getRepositoryBasedir( repoId ), namespace, projectId );

        // all directories in a project directory can be considered a version
        return getSortedFiles( dir, filter );
    }

    public Collection<ArtifactMetadata> readArtifactsMetadata( String repoId, String namespace, String projectId,
                                                               String projectVersion, Filter<String> filter )
    {
        File dir = pathTranslator.toFile( getRepositoryBasedir( repoId ), namespace, projectId, projectVersion );

        // all files that are not metadata and not a checksum / signature are considered artifacts
        File[] files = dir.listFiles( new ArtifactDirectoryFilter( filter ) );

        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();
        if ( files != null )
        {
            for ( File file : files )
            {
                ArtifactMetadata metadata = getArtifactFromFile( repoId, namespace, projectId, projectVersion, file );
                artifacts.add( metadata );
            }
        }
        return artifacts;
    }

    public ArtifactMetadata readArtifactMetadataFromPath( String repoId, String path )
    {
        ArtifactMetadata metadata = pathTranslator.getArtifactForPath( repoId, path );

        populateArtifactMetadataFromFile( metadata, new File( getRepositoryBasedir( repoId ), path ) );

        return metadata;
    }

    private ArtifactMetadata getArtifactFromFile( String repoId, String namespace, String projectId,
                                                  String projectVersion, File file )
    {
        ArtifactMetadata metadata = pathTranslator.getArtifactFromId( repoId, namespace, projectId, projectVersion,
                                                                      file.getName() );

        populateArtifactMetadataFromFile( metadata, file );

        return metadata;
    }

    private static void populateArtifactMetadataFromFile( ArtifactMetadata metadata, File file )
    {
        metadata.setWhenGathered( new Date() );
        metadata.setFileLastModified( file.lastModified() );
        ChecksummedFile checksummedFile = new ChecksummedFile( file );
        try
        {
            metadata.setMd5( checksummedFile.calculateChecksum( ChecksumAlgorithm.MD5 ) );
        }
        catch ( IOException e )
        {
            log.error( "Unable to checksum file " + file + ": " + e.getMessage() );
        }
        try
        {
            metadata.setSha1( checksummedFile.calculateChecksum( ChecksumAlgorithm.SHA1 ) );
        }
        catch ( IOException e )
        {
            log.error( "Unable to checksum file " + file + ": " + e.getMessage() );
        }
        metadata.setSize( file.length() );
    }

    private boolean isProject( File dir, Filter<String> filter )
    {
        // scan directories for a valid project version subdirectory, meaning this must be a project directory
        File[] files = dir.listFiles( new DirectoryFilter( filter ) );
        if ( files != null )
        {
            for ( File file : files )
            {
                if ( isProjectVersion( file ) )
                {
                    return true;
                }
            }
        }

        // if a metadata file is present, check if this is the "artifactId" directory, marking it as a project
        MavenRepositoryMetadata metadata = readMetadata( dir );
        if ( metadata != null && dir.getName().equals( metadata.getArtifactId() ) )
        {
            return true;
        }

        return false;
    }

    private boolean isProjectVersion( File dir )
    {
        final String artifactId = dir.getParentFile().getName();
        final String projectVersion = dir.getName();

        // check if there is a POM artifact file to ensure it is a version directory
        File[] files;
        if ( VersionUtil.isSnapshot( projectVersion ) )
        {
            files = dir.listFiles( new FilenameFilter()
            {
                public boolean accept( File dir, String name )
                {
                    if ( name.startsWith( artifactId + "-" ) && name.endsWith( ".pom" ) )
                    {
                        String v = name.substring( artifactId.length() + 1, name.length() - 4 );
                        v = VersionUtil.getBaseVersion( v );
                        if ( v.equals( projectVersion ) )
                        {
                            return true;
                        }
                    }
                    return false;
                }
            } );
        }
        else
        {
            final String pomFile = artifactId + "-" + projectVersion + ".pom";
            files = dir.listFiles( new FilenameFilter()
            {
                public boolean accept( File dir, String name )
                {
                    return pomFile.equals( name );
                }
            } );
        }
        if ( files != null && files.length > 0 )
        {
            return true;
        }

        // if a metadata file is present, check if this is the "version" directory, marking it as a project version
        MavenRepositoryMetadata metadata = readMetadata( dir );
        if ( metadata != null && projectVersion.equals( metadata.getVersion() ) )
        {
            return true;
        }

        return false;
    }

    private MavenRepositoryMetadata readMetadata( File directory )
    {
        MavenRepositoryMetadata metadata = null;
        File metadataFile = new File( directory, METADATA_FILENAME );
        if ( metadataFile.exists() )
        {
            try
            {
                metadata = MavenRepositoryMetadataReader.read( metadataFile );
            }
            catch ( XMLException e )
            {
                // ignore missing or invalid metadata
            }
        }
        return metadata;
    }

    private static class DirectoryFilter
        implements FilenameFilter
    {
        private final Filter<String> filter;

        public DirectoryFilter( Filter<String> filter )
        {
            this.filter = filter;
        }

        public boolean accept( File dir, String name )
        {
            if ( !filter.accept( name ) )
            {
                return false;
            }
            else if ( name.startsWith( "." ) )
            {
                return false;
            }
            else if ( !new File( dir, name ).isDirectory() )
            {
                return false;
            }
            return true;
        }
    }

    private class ArtifactDirectoryFilter
        implements FilenameFilter
    {
        private final Filter<String> filter;

        public ArtifactDirectoryFilter( Filter<String> filter )
        {
            this.filter = filter;
        }

        public boolean accept( File dir, String name )
        {
            // TODO compare to logic in maven-repository-layer
            if ( !filter.accept( name ) )
            {
                return false;
            }
            else if ( name.startsWith( "." ) )
            {
                return false;
            }
            else if ( name.endsWith( ".md5" ) || name.endsWith( ".sha1" ) || name.endsWith( ".asc" ) )
            {
                return false;
            }
            else if ( name.equals( METADATA_FILENAME ) )
            {
                return false;
            }
            else if ( new File( dir, name ).isDirectory() )
            {
                return false;
            }
            return true;
        }
    }
}
