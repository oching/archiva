package org.apache.archiva.scheduler.repository;

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

import junit.framework.TestCase;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.TestRepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * ArchivaRepositoryScanningTaskExecutorTest
 *
 * @version $Id$
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class ArchivaRepositoryScanningTaskExecutorTest
    extends TestCase
{
    @Inject
    @Named( value = "taskExecutor#test-repository-scanning" )
    private TaskExecutor taskExecutor;

    @Inject @Named(value = "archivaConfiguration#test-repository-scanning")
    private ArchivaConfiguration archivaConfig;

    @Inject
    @Named(value = "repositoryStatisticsManager#test")
    private RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    @Named( value = "knownRepositoryContentConsumer#test-consumer" )
    private TestConsumer testConsumer;

    @Inject
    @Named( value = "repositorySessionFactory#test" )
    private TestRepositorySessionFactory factory;

    private File repoDir;

    private static final String TEST_REPO_ID = "testRepo";

    private MetadataRepository metadataRepository;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        File sourceRepoDir = new File( "./src/test/repositories/default-repository" );
        repoDir = new File( "./target/default-repository" );

        FileUtils.deleteDirectory( repoDir );
        assertFalse( "Default Test Repository should not exist.", repoDir.exists() );

        repoDir.mkdir();

        FileUtils.copyDirectoryStructure( sourceRepoDir, repoDir );
        // set the timestamps to a time well in the past
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.YEAR, -1 );
        for ( File f : (List<File>) FileUtils.getFiles( repoDir, "**", null ) )
        {
            f.setLastModified( cal.getTimeInMillis() );
        }
        // TODO: test they are excluded instead
        for ( String dir : (List<String>) FileUtils.getDirectoryNames( repoDir, "**/.svn", null, false ) )
        {
            FileUtils.deleteDirectory( new File( repoDir, dir ) );
        }

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        assertNotNull( archivaConfig );

        // Create it
        ManagedRepositoryConfiguration repositoryConfiguration = new ManagedRepositoryConfiguration();
        repositoryConfiguration.setId( TEST_REPO_ID );
        repositoryConfiguration.setName( "Test Repository" );
        repositoryConfiguration.setLocation( repoDir.getAbsolutePath() );
        archivaConfig.getConfiguration().getManagedRepositories().clear();
        archivaConfig.getConfiguration().addManagedRepository( repositoryConfiguration );

        metadataRepository = mock( MetadataRepository.class );

        factory.setRepository( metadataRepository );
    }

    @After
    public void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( repoDir );

        assertFalse( repoDir.exists() );

        super.tearDown();
    }

    @Test
    public void testExecutor()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );

        taskExecutor.executeTask( repoTask );

        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected.", 8, unprocessedResultList.size() );
    }

    @Test
    public void testExecutorScanOnlyNewArtifacts()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( false );

        createAndSaveTestStats();

        taskExecutor.executeTask( repoTask );

        // check no artifacts processed
        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. No new artifacts should have been found.", 0,
                      unprocessedResultList.size() );

        // check correctness of new stats
        RepositoryStatistics newStats =
            repositoryStatisticsManager.getLastStatistics( metadataRepository, TEST_REPO_ID );
        assertEquals( 0, newStats.getNewFileCount() );
        assertEquals( 31, newStats.getTotalFileCount() );
        // FIXME: can't test these as they weren't stored in the database, move to tests for RepositoryStatisticsManager implementation
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
//        assertEquals( 14159, newStats.getTotalArtifactFileSize() );

        File newArtifactGroup = new File( repoDir, "org/apache/archiva" );

        FileUtils.copyDirectoryStructure( new File( "target/test-classes/test-repo/org/apache/archiva" ),
                                          newArtifactGroup );

        // update last modified date
        new File( newArtifactGroup, "archiva-index-methods-jar-test/1.0/pom.xml" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );
        new File( newArtifactGroup,
                  "archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );

        assertTrue( newArtifactGroup.exists() );

        taskExecutor.executeTask( repoTask );

        unprocessedResultList = testConsumer.getConsumed();
        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. One new artifact should have been found.", 1,
                      unprocessedResultList.size() );

        // check correctness of new stats
        RepositoryStatistics updatedStats =
            repositoryStatisticsManager.getLastStatistics( metadataRepository, TEST_REPO_ID );
        assertEquals( 2, updatedStats.getNewFileCount() );
        assertEquals( 33, updatedStats.getTotalFileCount() );
        // FIXME: can't test these as they weren't stored in the database, move to tests for RepositoryStatisticsManager implementation
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
//        assertEquals( 19301, updatedStats.getTotalArtifactFileSize() );
    }

    @Test
    public void testExecutorScanOnlyNewArtifactsChangeTimes()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( false );

        createAndSaveTestStats();

        File newArtifactGroup = new File( repoDir, "org/apache/archiva" );

        FileUtils.copyDirectoryStructure( new File( "target/test-classes/test-repo/org/apache/archiva" ),
                                          newArtifactGroup );

        // update last modified date, placing shortly after last scan
        new File( newArtifactGroup, "archiva-index-methods-jar-test/1.0/pom.xml" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );
        new File( newArtifactGroup,
                  "archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );

        assertTrue( newArtifactGroup.exists() );

        // scan using the really long previous duration
        taskExecutor.executeTask( repoTask );

        // check no artifacts processed
        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();
        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. One new artifact should have been found.", 1,
                      unprocessedResultList.size() );

        // check correctness of new stats
        RepositoryStatistics newStats =
            repositoryStatisticsManager.getLastStatistics( metadataRepository, TEST_REPO_ID );
        assertEquals( 2, newStats.getNewFileCount() );
        assertEquals( 33, newStats.getTotalFileCount() );
        // FIXME: can't test these as they weren't stored in the database, move to tests for RepositoryStatisticsManager implementation
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
//        assertEquals( 19301, newStats.getTotalArtifactFileSize() );
    }

    @Test
    public void testExecutorScanOnlyNewArtifactsMidScan()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( false );

        createAndSaveTestStats();

        File newArtifactGroup = new File( repoDir, "org/apache/archiva" );

        FileUtils.copyDirectoryStructure( new File( "target/test-classes/test-repo/org/apache/archiva" ),
                                          newArtifactGroup );

        // update last modified date, placing in middle of last scan
        new File( newArtifactGroup, "archiva-index-methods-jar-test/1.0/pom.xml" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() - 50000 );
        new File( newArtifactGroup,
                  "archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() - 50000 );

        assertTrue( newArtifactGroup.exists() );

        // scan using the really long previous duration
        taskExecutor.executeTask( repoTask );

        // check no artifacts processed
        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();
        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. One new artifact should have been found.", 1,
                      unprocessedResultList.size() );

        // check correctness of new stats
        RepositoryStatistics newStats =
            repositoryStatisticsManager.getLastStatistics( metadataRepository, TEST_REPO_ID );
        assertEquals( 2, newStats.getNewFileCount() );
        assertEquals( 33, newStats.getTotalFileCount() );
        // FIXME: can't test these as they weren't stored in the database, move to tests for RepositoryStatisticsManager implementation
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
//        assertEquals( 19301, newStats.getTotalArtifactFileSize() );
    }

    @Test
    public void testExecutorForceScanAll()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( true );

        Date date = Calendar.getInstance().getTime();
        repositoryStatisticsManager.addStatisticsAfterScan( metadataRepository, TEST_REPO_ID,
                                                            new Date( date.getTime() - 1234567 ), date, 8, 8 );

        taskExecutor.executeTask( repoTask );

        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected.", 8, unprocessedResultList.size() );
    }

    private void createAndSaveTestStats()
        throws MetadataRepositoryException
    {
        Date date = Calendar.getInstance().getTime();
        RepositoryStatistics stats = new RepositoryStatistics();
        stats.setScanStartTime( new Date( date.getTime() - 1234567 ) );
        stats.setScanEndTime( date );
        stats.setNewFileCount( 31 );
        stats.setTotalArtifactCount( 8 );
        stats.setTotalFileCount( 31 );
        stats.setTotalGroupCount( 3 );
        stats.setTotalProjectCount( 5 );
        stats.setTotalArtifactFileSize( 38545 );

        repositoryStatisticsManager.addStatisticsAfterScan( metadataRepository, TEST_REPO_ID,
                                                            new Date( date.getTime() - 1234567 ), date, 31, 31 );
    }
}
