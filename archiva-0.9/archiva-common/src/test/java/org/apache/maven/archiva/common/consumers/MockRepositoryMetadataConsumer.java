package org.apache.maven.archiva.common.consumers;

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

import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * MockRepositoryMetadataConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.common.consumers.Consumers"
 *     role-hint="mock-metadata"
 *     instantiation-strategy="per-lookup"
 */
public class MockRepositoryMetadataConsumer
    extends GenericRepositoryMetadataConsumer
{
    private Map repositoryMetadataMap = new HashMap();

    private FileProblemsTracker problemsTracker = new FileProblemsTracker();

    public void processRepositoryMetadata( RepositoryMetadata metadata, BaseFile file )
    {
        repositoryMetadataMap.put( file.getRelativePath(), metadata );
    }

    public void processFileProblem( BaseFile file, String message )
    {
        problemsTracker.addProblem( file, message );
    }

    public Map getRepositoryMetadataMap()
    {
        return repositoryMetadataMap;
    }

    public String getName()
    {
        return "Mock RepositoryMetadata Consumer (Testing Only)";
    }

    public FileProblemsTracker getProblemsTracker()
    {
        return problemsTracker;
    }
}