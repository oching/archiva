package org.apache.maven.archiva.web.action;

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

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.browsing.BrowsingResults;
import org.apache.maven.archiva.database.browsing.RepositoryBrowsing;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.security.*;

/**
 * Browse the repository.
 *
 * @todo cache browsing results.
 * @todo implement repository selectors (all or specific repository)
 * @todo implement security around browse (based on repository id at first)
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="browseAction" instantiation-strategy="per-lookup"
 */
public class BrowseAction
    extends PlexusActionSupport
{
    /**
     * @plexus.requirement role-hint="default"
     */
    private RepositoryBrowsing repoBrowsing;
    
    /**
     * @plexus.requirement
     */
    private UserRepositories userRepositories;
    
    private BrowsingResults results;

    private String groupId;

    private String artifactId;
    
    private String repositoryId;
    
    private ArchivaProjectModel sharedModel;
    
    public String browse()
    {
        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        this.results = repoBrowsing.getRoot( getPrincipal(), selectedRepos );
        return SUCCESS;
    }

    public String browseGroup()
    {
        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            return ERROR;
        }

        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        
        this.results = repoBrowsing.selectGroupId( getPrincipal(), selectedRepos, groupId );
        return SUCCESS;
    }

    public String browseArtifact()
    {
        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            return ERROR;
        }

        if ( StringUtils.isEmpty( artifactId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a artifact ID to browse" );
            return ERROR;
        }

        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }
        
        this.results = repoBrowsing.selectArtifactId( getPrincipal(), selectedRepos, groupId, artifactId );

        populateSharedModel();
        
        return SUCCESS;
    }

    private void populateSharedModel()
    {
        sharedModel = new ArchivaProjectModel();
        sharedModel.setGroupId( groupId );
        sharedModel.setArtifactId( artifactId );
        boolean isFirstVersion = true;
                
        for( String version :  this.results.getVersions() )
        {            
            try
            {
                ArchivaProjectModel model =
                    repoBrowsing.selectVersion( getPrincipal(), getObservableRepos(), groupId, artifactId, version );
                
                if( model == null )
                {
                    continue;
                }
                
                if( isFirstVersion )
                {
                    sharedModel = model;
                }
                else
                {
                    if ( sharedModel.getPackaging() != null &&
                        !StringUtils.equalsIgnoreCase( sharedModel.getPackaging(), model.getPackaging() ) )
                    {
                        sharedModel.setPackaging( null );
                    }
                    
                    if ( sharedModel.getName() != null &&
                        !StringUtils.equalsIgnoreCase( sharedModel.getName(), model.getName() ) )
                    {
                        sharedModel.setName( "" );
                    }

                    if ( sharedModel.getDescription() != null &&
                        !StringUtils.equalsIgnoreCase( sharedModel.getDescription(), model.getDescription() ) )
                    {
                        sharedModel.setDescription( null );
                    }

                    if ( sharedModel.getIssueManagement() != null && model.getIssueManagement() != null &&
                        !StringUtils.equalsIgnoreCase( sharedModel.getIssueManagement().getIssueManagementUrl(), model.getIssueManagement().getIssueManagementUrl() ) )
                    {
                        sharedModel.setIssueManagement( null );
                    }

                    if ( sharedModel.getCiManagement() != null && model.getCiManagement() != null &&
                        !StringUtils.equalsIgnoreCase( sharedModel.getCiManagement().getCiUrl(), model.getCiManagement().getCiUrl() ) )
                    {
                        sharedModel.setCiManagement( null );
                    }

                    if ( sharedModel.getOrganization() != null && model.getOrganization() != null && 
                        !StringUtils.equalsIgnoreCase( sharedModel.getOrganization().getOrganizationName(), model.getOrganization().getOrganizationName() ) )
                    {
                        sharedModel.setOrganization( null );
                    }

                    if ( sharedModel.getUrl() != null && !StringUtils.equalsIgnoreCase( sharedModel.getUrl(), model.getUrl() ) )
                    {
                        sharedModel.setUrl( null );
                    }
                }
                
                isFirstVersion = false;
            }
            catch ( ObjectNotFoundException e )
            {
                log.debug( e.getMessage(), e );
            }
            catch ( ArchivaDatabaseException e )
            {
                log.debug( e.getMessage(), e );
            }
        }        
    }
    
    private List<String> getObservableRepos()
    {
        try
        {
            return userRepositories.getObservableRepositoryIds( getPrincipal() );
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
            // TODO: pass this onto the screen.
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public BrowsingResults getResults()
    {
        return results;
    }
    
    public String getRepositoryId(){
    	
    	return repositoryId;
    }
    
    public void setRepositoryId(String repositoryId){
    	
    	this.repositoryId = repositoryId;
    }

    public ArchivaProjectModel getSharedModel()
    {
        return sharedModel;
    }

    public void setSharedModel( ArchivaProjectModel sharedModel )
    {
        this.sharedModel = sharedModel;
    }
}
