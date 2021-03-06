package org.apache.maven.archiva.web.action.admin.repositories;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.audit.AuditEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * DeleteRemoteRepositoryAction
 *
 * @version $Id$
 * plexus.component role="com.opensymphony.xwork2.Action" role-hint="deleteRemoteRepositoryAction" instantiation-strategy="per-lookup"
 */
@Controller( "deleteRemoteRepositoryAction" )
@Scope( "prototype" )
public class DeleteRemoteRepositoryAction
    extends AbstractRemoteRepositoriesAction
    implements Preparable
{
    private RemoteRepositoryConfiguration repository;

    private String repoid;

    public void prepare()
    {
        if ( StringUtils.isNotBlank( repoid ) )
        {
            this.repository = archivaConfiguration.getConfiguration().findRemoteRepositoryById( repoid );
        }
    }

    public String confirmDelete()
    {
        if ( StringUtils.isBlank( repoid ) )
        {
            addActionError( "Unable to delete remote repository: repository id was blank." );
            return ERROR;
        }

        return INPUT;
    }

    public String delete()
    {
        String result = SUCCESS;
        RemoteRepositoryConfiguration existingRepository = repository;
        if ( existingRepository == null )
        {
            addActionError( "A repository with that id does not exist" );
            return ERROR;
        }

        Configuration configuration = archivaConfiguration.getConfiguration();
        removeRepository( repoid, configuration );
        triggerAuditEvent( repoid, null, AuditEvent.DELETE_REMOTE_REPO );
        result = saveConfiguration( configuration );

        cleanupRepositoryData( existingRepository );

        return result;
    }

    private void cleanupRepositoryData( RemoteRepositoryConfiguration existingRepository )
    {
        // [MRM-520] Proxy Connectors are not deleted with the deletion of a Repository.

        List<ProxyConnectorConfiguration> proxyConnectors = getProxyConnectors();
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getTargetRepoId(), existingRepository.getId() ) )
            {
                archivaConfiguration.getConfiguration().removeProxyConnector( proxyConnector );
            }
        }
    }

    public RemoteRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( RemoteRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }
}
