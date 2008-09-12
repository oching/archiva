package org.apache.maven.archiva.reporting.database;

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

import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;
import org.apache.maven.archiva.reporting.model.ArtifactResults;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.List;

/**
 * ArtifactResultsDatabaseTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArtifactResultsDatabaseTest
    extends AbstractRepositoryReportsTestCase
{
    private Artifact artifact;

    private String processor, problem, reason;

    private ArtifactResultsDatabase database;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        database = (ArtifactResultsDatabase) lookup( ArtifactResultsDatabase.ROLE );

        artifact = new DefaultArtifact( "group", "artifact", VersionRange.createFromVersion( "1.0" ), "scope", "type",
                                        "classifier", null );
        processor = "processor";
        problem = "problem";
        reason = "reason";
    }

    protected void tearDown()
        throws Exception
    {
        release( database );

        super.tearDown();
    }

    public void testAddNoticeArtifactStringStringString()
    {
        database.addNotice( artifact, processor, problem, reason );
        ArtifactResults artifactResults = database.getArtifactResults( artifact );

        assertEquals( 1, database.getNumNotices() );
        assertEquals( 1, artifactResults.getNotices().size() );

        database.addNotice( artifact, processor, problem, reason );
        artifactResults = database.getArtifactResults( artifact );

        assertEquals( 1, database.getNumNotices() );
        assertEquals( 1, artifactResults.getNotices().size() );
    }

    public void testAddWarningArtifactStringStringString()
    {
        database.addWarning( artifact, processor, problem, reason );
        ArtifactResults artifactResults = database.getArtifactResults( artifact );

        assertEquals( 1, database.getNumWarnings() );
        assertEquals( 1, artifactResults.getWarnings().size() );

        database.addWarning( artifact, processor, problem, reason );
        artifactResults = database.getArtifactResults( artifact );

        assertEquals( 1, database.getNumWarnings() );
        assertEquals( 1, artifactResults.getWarnings().size() );
    }

    public void testAddFailureArtifactStringStringString()
    {
        database.addFailure( artifact, processor, problem, reason );
        ArtifactResults artifactResults = database.getArtifactResults( artifact );

        assertEquals( 1, database.getNumFailures() );
        assertEquals( 1, artifactResults.getFailures().size() );

        database.addFailure( artifact, processor, problem, reason );
        artifactResults = database.getArtifactResults( artifact );

        assertEquals( 1, database.getNumFailures() );
        assertEquals( 1, artifactResults.getFailures().size() );
    }

    public void testFindArtifactResults()
    {
        String groupId = "org.test.group";

        Artifact bar = createArtifact( "org.bar", "bar", "2.0" );
        Artifact foo = createArtifact( groupId, "foo", "1.0" );
        Artifact fooSources = createArtifactWithClassifier( groupId, "foo", "1.0", "jar", "sources" );
        Artifact fooJavadoc = createArtifactWithClassifier( groupId, "foo", "1.0", "jar", "javadoc" );

        database.addFailure( bar, processor, problem, "A reason that should not be found." );

        String testprocessor = "test-processor";
        String testproblem = "test-problem";

        database.addFailure( foo, testprocessor, testproblem, "Test Reason on main jar." );
        database.addFailure( foo, testprocessor, testproblem, "Someone mistook this for an actual reason." );
        database.addWarning( foo, testprocessor, testproblem, "Congrats you have a test reason." );

        database.addFailure( fooSources, testprocessor, testproblem, "Sources do not seem to match classes." );
        database.addWarning( fooJavadoc, testprocessor, testproblem, "Javadoc content makes no sense." );

        ArtifactResults artifactResults = database.getArtifactResults( foo );

        assertEquals( 4, database.getNumFailures() );
        assertEquals( 2, artifactResults.getFailures().size() );

        List hits = database.findArtifactResults( groupId, "foo", "1.0" );
        assertNotNull( hits );

//        for ( Iterator it = hits.iterator(); it.hasNext(); )
//        {
//            ArtifactResults result = (ArtifactResults) it.next();
//            System.out.println( " result: " + result.getGroupId() + ":" + result.getArtifactId() + ":"
//                + result.getVersion() + ":" + result.getClassifier() + ":" + result.getType() );
//
//            for ( Iterator itmsgs = result.getFailures().iterator(); itmsgs.hasNext(); )
//            {
//                Result res = (Result) itmsgs.next();
//                String msg = (String) res.getReason();
//                System.out.println( "    failure: " + msg );
//            }
//
//            for ( Iterator itmsgs = result.getWarnings().iterator(); itmsgs.hasNext(); )
//            {
//                Result res = (Result) itmsgs.next();
//                String msg = (String) res.getReason();
//                System.out.println( "    warning: " + msg );
//            }
//
//            for ( Iterator itmsgs = result.getNotices().iterator(); itmsgs.hasNext(); )
//            {
//                Result res = (Result) itmsgs.next();
//                String msg = (String) res.getReason();
//                System.out.println( "    notice: " + msg );
//            }
//        }

        assertEquals( "Should find 3 artifacts", 3, hits.size() ); // 3 artifacts
    }
}