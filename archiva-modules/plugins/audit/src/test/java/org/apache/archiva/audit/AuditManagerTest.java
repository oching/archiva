package org.apache.archiva.audit;

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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import junit.framework.TestCase;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

public class AuditManagerTest
    extends TestCase
{
    private DefaultAuditManager auditManager;

    private MockControl metadataRepositoryControl;

    private MetadataRepository metadataRepository;

    private static final String AUDIT_EVENT_BASE = "2010/01/18/123456.";

    private static final String TEST_REPO_ID = "test-repo";

    private static final String TEST_REPO_ID_2 = "repo2";

    private static final String TEST_USER = "test_user";

    private static final String TEST_RESOURCE_BASE = "test/resource";

    private static final String TEST_IP_ADDRESS = "127.0.0.1";

    private static final SimpleDateFormat TIMESTAMP_FORMAT = createTimestampFormat();

    private static final DecimalFormat MILLIS_FORMAT = new DecimalFormat( "000" );

    private static SimpleDateFormat createTimestampFormat()
    {
        SimpleDateFormat fmt = new SimpleDateFormat( AuditEvent.TIMESTAMP_FORMAT );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        auditManager = new DefaultAuditManager();

        metadataRepositoryControl = MockControl.createControl( MetadataRepository.class );
        metadataRepository = (MetadataRepository) metadataRepositoryControl.getMock();
        auditManager.setMetadataRepository( metadataRepository );

        ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();
        repository.setId( TEST_REPO_ID );
        repository.setLocation( "" );
        ManagedDefaultRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( repository );
        MockControl control = MockClassControl.createControl( RepositoryContentFactory.class );
        RepositoryContentFactory contentFactory = (RepositoryContentFactory) control.getMock();
        contentFactory.getManagedRepositoryContent( TEST_REPO_ID );
        control.setDefaultReturnValue( content );
        control.replay();
    }

    public void testGetMostRecentEvents()
        throws ParseException
    {
        int numEvents = 11;
        List<String> eventNames = new ArrayList<String>( numEvents );
        for ( int i = 0; i < numEvents; i++ )
        {
            eventNames.add( createEventName( TIMESTAMP_FORMAT.parse( AUDIT_EVENT_BASE + MILLIS_FORMAT.format( i ) ) ) );
        }

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ), eventNames );

        for ( String name : eventNames.subList( 1, eventNames.size() ) )
        {
            AuditEvent event = createTestEvent( name );

            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name ), event );
        }
        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getMostRecentAuditEvents( Collections.singletonList( TEST_REPO_ID ) );
        assertNotNull( events );
        assertEquals( numEvents - 1, events.size() );
        int expectedTimestampCounter = numEvents - 1;
        for ( AuditEvent event : events )
        {
            String num = MILLIS_FORMAT.format( expectedTimestampCounter );
            assertTestEvent( event, AUDIT_EVENT_BASE + num, getDefaultTestResourceName( num ) );
            expectedTimestampCounter--;
        }

        metadataRepositoryControl.verify();
    }

    public void testGetMostRecentEventsLessThan10()
        throws ParseException
    {
        int numEvents = 5;
        List<String> eventNames = new ArrayList<String>( numEvents );
        for ( int i = 0; i < numEvents; i++ )
        {
            eventNames.add( createEventName( TIMESTAMP_FORMAT.parse( AUDIT_EVENT_BASE + MILLIS_FORMAT.format( i ) ) ) );
        }

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ), eventNames );

        for ( String name : eventNames )
        {
            AuditEvent event = createTestEvent( name );

            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name ), event );
        }
        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getMostRecentAuditEvents( Collections.singletonList( TEST_REPO_ID ) );
        assertNotNull( events );
        assertEquals( numEvents, events.size() );
        int expectedTimestampCounter = numEvents - 1;
        for ( AuditEvent event : events )
        {
            String num = MILLIS_FORMAT.format( expectedTimestampCounter );
            assertTestEvent( event, AUDIT_EVENT_BASE + num, getDefaultTestResourceName( num ) );
            expectedTimestampCounter--;
        }

        metadataRepositoryControl.verify();
    }

    public void testGetMostRecentEventsInterleavedRepositories()
        throws ParseException
    {
        int numEvents = 11;
        Map<String, List<String>> eventNames = new LinkedHashMap<String, List<String>>();
        List<AuditEvent> events = new ArrayList<AuditEvent>();
        eventNames.put( TEST_REPO_ID, new ArrayList<String>() );
        eventNames.put( TEST_REPO_ID_2, new ArrayList<String>() );
        for ( int i = 0; i < numEvents; i++ )
        {
            String name = createEventName( TIMESTAMP_FORMAT.parse( AUDIT_EVENT_BASE + MILLIS_FORMAT.format( i ) ) );
            String repositoryId = i % 2 == 0 ? TEST_REPO_ID : TEST_REPO_ID_2;
            eventNames.get( repositoryId ).add( name );
            events.add( createTestEvent( repositoryId, name ) );
        }

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ), eventNames.get( TEST_REPO_ID ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID_2, AuditEvent.FACET_ID ),
            eventNames.get( TEST_REPO_ID_2 ) );

        for ( AuditEvent event : events.subList( 1, events.size() ) )
        {
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getMetadataFacet( event.getRepositoryId(), AuditEvent.FACET_ID, event.getName() ),
                event );
        }
        metadataRepositoryControl.replay();

        events = auditManager.getMostRecentAuditEvents( Arrays.asList( TEST_REPO_ID, TEST_REPO_ID_2 ) );
        assertNotNull( events );
        assertEquals( numEvents - 1, events.size() );
        int expectedTimestampCounter = numEvents - 1;
        for ( AuditEvent event : events )
        {
            String num = MILLIS_FORMAT.format( expectedTimestampCounter );
            String expectedRepoId = expectedTimestampCounter % 2 == 0 ? TEST_REPO_ID : TEST_REPO_ID_2;
            assertTestEvent( event, expectedRepoId, AUDIT_EVENT_BASE + num, getDefaultTestResourceName( num ) );
            expectedTimestampCounter--;
        }

        metadataRepositoryControl.verify();
    }

    public void testGetMostRecentEventsWhenEmpty()
    {
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ), Collections.emptyList() );
        metadataRepositoryControl.replay();

        assertTrue( auditManager.getMostRecentAuditEvents( Collections.singletonList( TEST_REPO_ID ) ).isEmpty() );

        metadataRepositoryControl.verify();
    }

    public void testAddAuditEvent()
        throws ParseException
    {
        String name = createEventName( new Date() );
        AuditEvent event = createTestEvent( name );

        metadataRepository.addMetadataFacet( TEST_REPO_ID, event );

        metadataRepositoryControl.replay();

        auditManager.addAuditEvent( event );

        metadataRepositoryControl.verify();
    }

    public void testAddAuditEventNoRepositoryId()
        throws ParseException
    {
        String name = createEventName( new Date() );
        AuditEvent event = createTestEvent( null, name );

        // should just be ignored

        metadataRepositoryControl.replay();

        auditManager.addAuditEvent( event );

        metadataRepositoryControl.verify();
    }

    public void testDeleteStats()
    {
        metadataRepository.removeMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID );

        metadataRepositoryControl.replay();

        auditManager.deleteAuditEvents( TEST_REPO_ID );

        metadataRepositoryControl.verify();
    }

    public void testGetEventsRangeInside()
        throws ParseException
    {
        Date current = new Date();

        String name1 = createEventName( new Date( current.getTime() - 12345 ) );
        Date expectedTimestamp = new Date( current.getTime() - 3000 );
        String name2 = createEventName( expectedTimestamp );
        AuditEvent expectedEvent = createTestEvent( name2 );
        String name3 = createEventName( new Date( current.getTime() - 1000 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ),
            Arrays.asList( name1, name2, name3 ) );

        // only match the middle one
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name2 ), expectedEvent );

        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getAuditEventsInRange( Collections.singletonList( TEST_REPO_ID ),
                                                                      new Date( current.getTime() - 4000 ),
                                                                      new Date( current.getTime() - 2000 ) );

        assertEquals( 1, events.size() );
        assertTestEvent( events.get( 0 ), TIMESTAMP_FORMAT.format( expectedTimestamp ), expectedEvent.getResource() );

        metadataRepositoryControl.verify();
    }

    public void testGetEventsRangeUpperOutside()
        throws ParseException
    {
        Date current = new Date();

        String name1 = createEventName( new Date( current.getTime() - 12345 ) );
        Date expectedTimestamp = new Date( current.getTime() - 3000 );
        String name2 = createEventName( expectedTimestamp );
        AuditEvent expectedEvent2 = createTestEvent( name2 );
        Date ts3 = new Date( current.getTime() - 1000 );
        String name3 = createEventName( ts3 );
        AuditEvent expectedEvent3 = createTestEvent( name3 );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ),
            Arrays.asList( name1, name2, name3 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name2 ), expectedEvent2 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name3 ), expectedEvent3 );

        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getAuditEventsInRange( Collections.singletonList( TEST_REPO_ID ),
                                                                      new Date( current.getTime() - 4000 ), current );

        assertEquals( 2, events.size() );
        assertTestEvent( events.get( 0 ), TIMESTAMP_FORMAT.format( ts3 ), expectedEvent3.getResource() );
        assertTestEvent( events.get( 1 ), TIMESTAMP_FORMAT.format( expectedTimestamp ), expectedEvent2.getResource() );

        metadataRepositoryControl.verify();
    }

    public void testGetEventsRangeLowerOutside()
        throws ParseException
    {
        Date current = new Date();

        Date ts1 = new Date( current.getTime() - 12345 );
        String name1 = createEventName( ts1 );
        AuditEvent expectedEvent1 = createTestEvent( name1 );
        Date expectedTimestamp = new Date( current.getTime() - 3000 );
        String name2 = createEventName( expectedTimestamp );
        AuditEvent expectedEvent2 = createTestEvent( name2 );
        String name3 = createEventName( new Date( current.getTime() - 1000 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ),
            Arrays.asList( name1, name2, name3 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name1 ), expectedEvent1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name2 ), expectedEvent2 );

        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getAuditEventsInRange( Collections.singletonList( TEST_REPO_ID ),
                                                                      new Date( current.getTime() - 20000 ),
                                                                      new Date( current.getTime() - 2000 ) );

        assertEquals( 2, events.size() );
        assertTestEvent( events.get( 0 ), TIMESTAMP_FORMAT.format( expectedTimestamp ), expectedEvent2.getResource() );
        assertTestEvent( events.get( 1 ), TIMESTAMP_FORMAT.format( ts1 ), expectedEvent1.getResource() );

        metadataRepositoryControl.verify();
    }

    public void testGetEventsRangeLowerAndUpperOutside()
        throws ParseException
    {
        Date current = new Date();

        Date ts1 = new Date( current.getTime() - 12345 );
        String name1 = createEventName( ts1 );
        AuditEvent expectedEvent1 = createTestEvent( name1 );
        Date expectedTimestamp = new Date( current.getTime() - 3000 );
        String name2 = createEventName( expectedTimestamp );
        AuditEvent expectedEvent2 = createTestEvent( name2 );
        Date ts3 = new Date( current.getTime() - 1000 );
        String name3 = createEventName( ts3 );
        AuditEvent expectedEvent3 = createTestEvent( name3 );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ),
            Arrays.asList( name1, name2, name3 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name1 ), expectedEvent1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name2 ), expectedEvent2 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name3 ), expectedEvent3 );

        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getAuditEventsInRange( Collections.singletonList( TEST_REPO_ID ),
                                                                      new Date( current.getTime() - 20000 ), current );

        assertEquals( 3, events.size() );
        assertTestEvent( events.get( 0 ), TIMESTAMP_FORMAT.format( ts3 ), expectedEvent3.getResource() );
        assertTestEvent( events.get( 1 ), TIMESTAMP_FORMAT.format( expectedTimestamp ), expectedEvent2.getResource() );
        assertTestEvent( events.get( 2 ), TIMESTAMP_FORMAT.format( ts1 ), expectedEvent1.getResource() );

        metadataRepositoryControl.verify();
    }

    public void testGetEventsWithResource()
        throws ParseException
    {
        Date current = new Date();

        Date ts1 = new Date( current.getTime() - 12345 );
        String name1 = createEventName( ts1 );
        AuditEvent expectedEvent1 = createTestEvent( name1 );
        Date expectedTimestamp = new Date( current.getTime() - 3000 );
        String name2 = createEventName( expectedTimestamp );
        AuditEvent expectedEvent2 = createTestEvent( name2 );
        expectedEvent2.setResource( "different-resource" );
        Date ts3 = new Date( current.getTime() - 1000 );
        String name3 = createEventName( ts3 );
        AuditEvent expectedEvent3 = createTestEvent( name3 );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ),
            Arrays.asList( name1, name2, name3 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name1 ), expectedEvent1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name2 ), expectedEvent2 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name3 ), expectedEvent3 );

        metadataRepositoryControl.replay();

        List<AuditEvent> events =
            auditManager.getAuditEventsInRange( Collections.singletonList( TEST_REPO_ID ), TEST_RESOURCE_BASE,
                                                new Date( current.getTime() - 20000 ), current );

        assertEquals( 2, events.size() );
        assertTestEvent( events.get( 0 ), TIMESTAMP_FORMAT.format( ts3 ), expectedEvent3.getResource() );
        assertTestEvent( events.get( 1 ), TIMESTAMP_FORMAT.format( ts1 ), expectedEvent1.getResource() );

        metadataRepositoryControl.verify();
    }

    public void testGetEventsWithNonExistantResource()
        throws ParseException
    {
        Date current = new Date();

        String name1 = createEventName( new Date( current.getTime() - 12345 ) );
        AuditEvent expectedEvent1 = createTestEvent( name1 );
        Date expectedTimestamp = new Date( current.getTime() - 3000 );
        String name2 = createEventName( expectedTimestamp );
        AuditEvent expectedEvent2 = createTestEvent( name2 );
        expectedEvent2.setResource( "different-resource" );
        String name3 = createEventName( new Date( current.getTime() - 1000 ) );
        AuditEvent expectedEvent3 = createTestEvent( name3 );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ),
            Arrays.asList( name1, name2, name3 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name1 ), expectedEvent1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name2 ), expectedEvent2 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name3 ), expectedEvent3 );

        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getAuditEventsInRange( Collections.singletonList( TEST_REPO_ID ), "foo",
                                                                      new Date( current.getTime() - 20000 ), current );

        assertEquals( 0, events.size() );

        metadataRepositoryControl.verify();
    }

    public void testGetEventsRangeMultipleRepositories()
        throws ParseException
    {
        Date current = new Date();

        Date ts1 = new Date( current.getTime() - 12345 );
        String name1 = createEventName( ts1 );
        AuditEvent expectedEvent1 = createTestEvent( TEST_REPO_ID, name1 );
        Date expectedTimestamp = new Date( current.getTime() - 3000 );
        String name2 = createEventName( expectedTimestamp );
        AuditEvent expectedEvent2 = createTestEvent( TEST_REPO_ID_2, name2 );
        Date ts3 = new Date( current.getTime() - 1000 );
        String name3 = createEventName( ts3 );
        AuditEvent expectedEvent3 = createTestEvent( TEST_REPO_ID, name3 );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ), Arrays.asList( name1, name3 ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID_2, AuditEvent.FACET_ID ), Arrays.asList( name2 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name1 ), expectedEvent1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID_2, AuditEvent.FACET_ID, name2 ), expectedEvent2 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, AuditEvent.FACET_ID, name3 ), expectedEvent3 );

        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getAuditEventsInRange( Arrays.asList( TEST_REPO_ID, TEST_REPO_ID_2 ),
                                                                      new Date( current.getTime() - 20000 ), current );

        assertEquals( 3, events.size() );
        assertTestEvent( events.get( 0 ), TEST_REPO_ID, TIMESTAMP_FORMAT.format( ts3 ), expectedEvent3.getResource() );
        assertTestEvent( events.get( 1 ), TEST_REPO_ID_2, TIMESTAMP_FORMAT.format( expectedTimestamp ),
                         expectedEvent2.getResource() );
        assertTestEvent( events.get( 2 ), TEST_REPO_ID, TIMESTAMP_FORMAT.format( ts1 ), expectedEvent1.getResource() );

        metadataRepositoryControl.verify();
    }

    public void testGetEventsRangeNotInside()
        throws ParseException
    {
        Date current = new Date();

        String name1 = createEventName( new Date( current.getTime() - 12345 ) );
        Date expectedTimestamp = new Date( current.getTime() - 3000 );
        String name2 = createEventName( expectedTimestamp );
        String name3 = createEventName( new Date( current.getTime() - 1000 ) );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, AuditEvent.FACET_ID ),
            Arrays.asList( name1, name2, name3 ) );

        metadataRepositoryControl.replay();

        List<AuditEvent> events = auditManager.getAuditEventsInRange( Collections.singletonList( TEST_REPO_ID ),
                                                                      new Date( current.getTime() - 20000 ),
                                                                      new Date( current.getTime() - 16000 ) );

        assertEquals( 0, events.size() );

        metadataRepositoryControl.verify();
    }

    private static String getDefaultTestResourceName( String num )
    {
        return TEST_RESOURCE_BASE + "/" + num + ".xml";
    }

    private static String createEventName( Date timestamp )
    {
        AuditEvent event = new AuditEvent();
        event.setTimestamp( timestamp );
        return event.getName();
    }

    private static AuditEvent createTestEvent( String name )
        throws ParseException
    {
        return createTestEvent( TEST_REPO_ID, name );
    }

    private static AuditEvent createTestEvent( String repoId, String name )
        throws ParseException
    {
        return createEvent( repoId, name, getDefaultTestResourceName( name.substring( name.length() - 3 ) ) );
    }

    private static AuditEvent createEvent( String repositoryId, String timestamp, String resource )
        throws ParseException
    {
        AuditEvent event = new AuditEvent();
        event.setTimestamp( TIMESTAMP_FORMAT.parse( timestamp ) );
        event.setAction( AuditEvent.UPLOAD_FILE );
        event.setRemoteIP( TEST_IP_ADDRESS );
        event.setRepositoryId( repositoryId );
        event.setUserId( TEST_USER );
        event.setResource( resource );
        return event;
    }

    private static void assertTestEvent( AuditEvent event, String timestamp, String resource )
    {
        assertTestEvent( event, TEST_REPO_ID, timestamp, resource );
    }

    private static void assertTestEvent( AuditEvent event, String repositoryId, String timestamp, String resource )
    {
        assertEquals( timestamp, TIMESTAMP_FORMAT.format( event.getTimestamp() ) );
        assertEquals( AuditEvent.UPLOAD_FILE, event.getAction() );
        assertEquals( TEST_IP_ADDRESS, event.getRemoteIP() );
        assertEquals( repositoryId, event.getRepositoryId() );
        assertEquals( TEST_USER, event.getUserId() );
        assertEquals( resource, event.getResource() );
    }
}