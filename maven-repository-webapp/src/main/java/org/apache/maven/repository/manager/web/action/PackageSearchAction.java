package org.apache.maven.repository.manager.web.action;

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

import com.opensymphony.xwork.Action;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.DefaultRepositoryIndexSearcher;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.codehaus.plexus.scheduler.Scheduler;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

/**
 * TODO: Description.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.PackageSearchAction"
 */
public class PackageSearchAction
    implements Action
{
    private String packageName;

    private String md5;

    /**
     * @plexus.requirement
     */
    private Scheduler scheduler;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexingFactory factory;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement role-hint="legacy"
     */
    private ArtifactRepositoryLayout layout;

    private List artifacts;

    public String execute()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException
    {
        String searchTerm;
        String key;
        if ( packageName != null && packageName.length() != 0 )
        {
            searchTerm = packageName;
            key = "packages";
        }
        else if ( md5 != null && md5.length() != 0 )
        {
            searchTerm = md5;
            key = "md5";
        }
        else
        {
            return ERROR;
        }

        // TODO: better config
        String indexPath = "c:/home/brett/repository/.index";

        // TODO: reduce the amount of lookup?
        ArtifactRepository repository = repositoryFactory.createArtifactRepository( "repository", new File(
            indexPath ).toURL().toString(), layout, null, null );

        ArtifactRepositoryIndex index = factory.createArtifactRepositoryIndex( indexPath, repository );

        DefaultRepositoryIndexSearcher searcher = factory.createDefaultRepositoryIndexSearcher( index );

        return SUCCESS;
    }

    public void setPackageName( String packageName )
    {
        this.packageName = packageName;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }

    public List getArtifacts()
    {
        return artifacts;
    }
}
