package org.apache.maven.archiva.proxy;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.File;

/**
 * ManagedDefaultTransferTest
 *
 * @author Brett Porter
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedDefaultTransferTest
    extends AbstractProxyTestCase
{
    public void testGetDefaultLayoutNotPresent()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Ensure file isn't present first.
        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.NO );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File sourceFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, sourceFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * The attempt here should result in no file being transferred.
     * <p/>
     * The file exists locally, and the policy is ONCE.
     *
     * @throws Exception
     */
    public void testGetDefaultLayoutAlreadyPresentPolicyOnce()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );

        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.NO );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertFileEquals( expectedFile, downloadedFile, expectedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * <p>
     * Request a file, that exists locally, and remotely.
     * </p>
     * <p>
     * All policies are set to IGNORE.
     * </p>
     * <p>
     * Managed file is newer than remote file.
     * </p>
     * <p>
     * Transfer should not have occured, as managed file is newer.
     * </p>
     *
     * @throws Exception
     */
    public void testGetDefaultLayoutAlreadyPresentNewerThanRemotePolicyIgnored()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        File remoteFile = new File( REPOPATH_PROXIED1, path );
        
        // Set the managed File to be newer than local.
        setManagedNewerThanRemote( expectedFile, remoteFile );

        long originalModificationTime = expectedFile.lastModified();
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.NO );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertNotModified( expectedFile, originalModificationTime );
        assertNoTempFiles( expectedFile );
    }
    
    /**
     * <p>
     * Request a file, that exists locally, and remotely.
     * </p>
     * <p>
     * All policies are set to IGNORE.
     * </p>
     * <p>
     * Managed file is older than Remote file.
     * </p>
     * <p>
     * Transfer should have occured, as managed file is older than remote.
     * </p>
     *
     * @throws Exception
     */
    public void testGetDefaultLayoutAlreadyPresentOlderThanRemotePolicyIgnored()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        File remoteFile = new File( REPOPATH_PROXIED1, path );
        
        // Set the managed file to be newer than remote file.
        setManagedOlderThanRemote( expectedFile, remoteFile );
    
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.NO );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * The attempt here should result in file being transferred.
     * <p/>
     * The file exists locally, is over 6 years old, and the policy is DAILY.
     *
     * @throws Exception
     */
    public void testGetDefaultLayoutRemoteUpdate()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );
        expectedFile.setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.DAILY, SnapshotsPolicy.DAILY,
                       CachedFailuresPolicy.NO );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetWhenInBothProxiedRepos()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-both-proxies/1.0/get-in-both-proxies-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1 );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2 );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        File proxied2File = new File( REPOPATH_PROXIED2, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );

        // TODO: is this check even needed if it passes above? 
        String actualContents = FileUtils.readFileToString( downloadedFile, null );
        String badContents = FileUtils.readFileToString( proxied2File, null );
        assertFalse( "Downloaded file contents should not be that of proxy 2",
                     StringUtils.equals( actualContents, badContents ) );
    }

    public void testGetInSecondProxiedRepo()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1 );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2 );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied2File = new File( REPOPATH_PROXIED2, path );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );
    }

    public void testNotFoundInAnyProxies()
        throws Exception
    {
        String path = "org/apache/maven/test/does-not-exist/1.0/does-not-exist-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1 );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2 );
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNull( "File returned was: " + downloadedFile + "; should have got a not found exception",
                    downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetInSecondProxiedRepoFirstFails()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRemoteRepositoryConfig( "badproxied", "Bad Proxied", "test://bad.machine.com/repo/", "default" );

        wagonMock.get( path, new File( expectedFile.getAbsolutePath() + ".tmp" ) );
        wagonMockControl.setThrowable( new ResourceDoesNotExistException( "transfer failed" ) );
        wagonMockControl.replay();

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied" );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2 );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        wagonMockControl.verify();

        File proxied2File = new File( REPOPATH_PROXIED2, path );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );
    }

    public void testGetAllRepositoriesFail()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir.getAbsoluteFile(), path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRemoteRepositoryConfig( "badproxied1", "Bad Proxied 1", "test://bad.machine.com/repo/", "default" );
        saveRemoteRepositoryConfig( "badproxied2", "Bad Proxied 2", "test://dead.machine.com/repo/", "default" );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied1" );
        saveConnector( ID_DEFAULT_MANAGED, "badproxied2" );

        File tmpFile = new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" );
        wagonMock.get( path, tmpFile );
        wagonMockControl.setThrowable( new ResourceDoesNotExistException( "Can't find resource." ) );

        wagonMock.get( path, tmpFile );
        wagonMockControl.setThrowable( new ResourceDoesNotExistException( "Can't find resource." ) );

        wagonMockControl.replay();

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );

        wagonMockControl.verify();
        assertNoTempFiles( expectedFile );

        // TODO: do not want failures to present as a not found [MRM-492]
        // TODO: How much information on each failure should we pass back to the user vs. logging in the proxy? 
    }

    public void testGetFromLegacyProxyAlreadyPresentInManaged_NewerThanRemote()
        throws Exception
    {
        String legacyPath = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        File remoteFile = new File( REPOPATH_PROXIED_LEGACY, legacyPath );
        
        // Set the managed file to be newer than remote.
        setManagedNewerThanRemote( expectedFile, remoteFile );
        long expectedTimestamp = expectedFile.lastModified();
        
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );
        
        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertNotModified( expectedFile, expectedTimestamp );
        assertNoTempFiles( expectedFile );
    }
    
    public void testGetFromLegacyProxyAlreadyPresentInManaged_OlderThanRemote()
        throws Exception
    {
        String legacyPath = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        File remoteFile = new File( REPOPATH_PROXIED_LEGACY, legacyPath );

        // Set the managed file to be older than remote.
        setManagedOlderThanRemote( expectedFile, remoteFile );

        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY, legacyPath );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetFromLegacyProxyNotPresentInManaged()
        throws Exception
    {
        String legacyPath = "org.apache.maven.test/jars/example-lib-2.2.jar";
        String path = "org/apache/maven/test/example-lib/2.2/example-lib-2.2.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY, legacyPath );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetFromLegacyProxyPluginNotPresentInManaged()
        throws Exception
    {
        String legacyPath = "org.apache.maven.test/plugins/example-maven-plugin-0.42.jar";
        String path = "org/apache/maven/test/example-maven-plugin/0.42/example-maven-plugin-0.42.jar";
        setupTestableManagedRepository( path );

        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY, legacyPath );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }
}
