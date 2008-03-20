package org.apache.maven.archiva.model;

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

import java.util.Date;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/**
 * ArchivaModelClonerTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id: ArchivaModelClonerTest.java 525951 2007-04-05 20:11:19Z joakime $
 */
public class ArchivaArtifactTest
    extends PlexusInSpringTestCase
{
    public void testArtifactModelProcessed()
    {
        ArchivaArtifactModel model = new ArchivaArtifactModel();

        assertNull( "whenProcessed", model.getWhenProcessed() );
        assertFalse( "isProcessed", model.isProcessed() );

        model.setWhenProcessed( new Date() );

        assertTrue( "isProcessed", model.isProcessed() );
    }

}