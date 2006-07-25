package org.apache.maven.repository.indexing.record;

import org.apache.maven.artifact.Artifact;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The layout of a record in a repository index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface RepositoryIndexRecordFactory
{
    /**
     * The Plexus role.
     */
    String ROLE = RepositoryIndexRecordFactory.class.getName();

    /**
     * Create an index record from an artifact.
     *
     * @param artifact the artifact
     * @return the index record
     */
    RepositoryIndexRecord createRecord( Artifact artifact );
}
