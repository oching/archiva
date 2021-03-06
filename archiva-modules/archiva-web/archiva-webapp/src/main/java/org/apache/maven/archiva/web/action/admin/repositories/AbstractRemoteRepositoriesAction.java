package org.apache.maven.archiva.web.action.admin.repositories;

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManagerException;

import java.io.IOException;

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

/**
 * AbstractRemoteRepositoriesAction 
 *
 * @version $Id$
 */
public class AbstractRemoteRepositoriesAction
    extends AbstractRepositoriesAdminAction
{
    protected void addRepository( RemoteRepositoryConfiguration repository, Configuration configuration )
        throws IOException, RoleManagerException
    {
        configuration.addRemoteRepository( repository );
    }

    protected void removeRepository( String repoId, Configuration configuration )
    {
        RemoteRepositoryConfiguration toremove = configuration.findRemoteRepositoryById( repoId );
        if ( toremove != null )
        {
            configuration.removeRemoteRepository( toremove );
        }
    }
}
