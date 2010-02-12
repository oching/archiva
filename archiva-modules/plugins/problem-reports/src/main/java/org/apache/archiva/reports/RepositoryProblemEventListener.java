package org.apache.archiva.reports;

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

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.maven.archiva.repository.events.RepositoryListener;

/**
 * Process repository management events and respond appropriately.
 *
 * @plexus.component role="org.apache.maven.archiva.repository.events.RepositoryListener" role-hint="problem-reports"
 */
public class RepositoryProblemEventListener
    implements RepositoryListener
{
    /**
     * @plexus.requirement
     */
    private MetadataRepository metadataRepository;

    public void deleteArtifact( String repositoryId, String namespace, String project, String version, String id )
    {
        String name = RepositoryProblemFacet.createName( namespace, project, version, id );

        metadataRepository.removeMetadataFacet( repositoryId, RepositoryProblemFacet.FACET_ID, name );
    }
}