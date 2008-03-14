package org.apache.maven.archiva.converter.transaction;

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

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author Edwin Punzalan
 */
public class CreateFileEventTest
    extends AbstractFileEventTest
{
    private File testDir = new File( PlexusTestCase.getBasedir(), "target/transaction-tests/create-file" );

    public void testCreateCommitRollback()
        throws Exception
    {
        File testFile = new File( testDir, "test-file.txt" );

        CreateFileEvent event = new CreateFileEvent( "file contents", testFile, digesters );

        assertFalse( "Test file is not yet created", testFile.exists() );

        event.commit();

        assertTrue( "Test file has been created", testFile.exists() );

        assertChecksumCommit( testFile );

        event.rollback();

        assertFalse( "Test file is has been deleted after rollback", testFile.exists() );

        assertChecksumRollback( testFile );

        assertFalse( "Test file parent directories has been rolledback too", testDir.exists() );
        assertTrue( "target directory still exists", new File( PlexusTestCase.getBasedir(), "target" ).exists() );
    }

    public void testCreateCommitRollbackWithBackup()
        throws Exception
    {
        File testFile = new File( testDir, "test-file.txt" );

        testFile.getParentFile().mkdirs();

        testFile.createNewFile();

        FileUtils.writeStringToFile( testFile, "original contents", null );

        CreateFileEvent event = new CreateFileEvent( "modified contents", testFile, digesters );

        String contents = FileUtils.readFileToString( testFile, null );

        assertEquals( "Test contents have not changed", "original contents", contents );

        event.commit();

        contents = FileUtils.readFileToString( testFile, null );

        assertEquals( "Test contents have not changed", "modified contents", contents );

        assertChecksumCommit( testFile );

        event.rollback();

        contents = FileUtils.readFileToString( testFile, null );

        assertEquals( "Test contents have not changed", "original contents", contents );

        assertChecksumRollback( testFile );
    }

    public void testCreateRollbackCommit()
        throws Exception
    {
        File testFile = new File( testDir, "test-file.txt" );

        CreateFileEvent event = new CreateFileEvent( "file contents", testFile, digesters );

        assertFalse( "Test file is not yet created", testFile.exists() );

        event.rollback();

        assertFalse( "Test file is not yet created", testFile.exists() );

        event.commit();

        assertTrue( "Test file is not yet created", testFile.exists() );

        assertChecksumCommit( testFile );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        FileUtils.deleteDirectory( new File( PlexusTestCase.getBasedir(), "target/transaction-tests" ) );
    }
}