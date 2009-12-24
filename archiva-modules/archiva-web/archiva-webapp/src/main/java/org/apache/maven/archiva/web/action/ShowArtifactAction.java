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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Browse the repository.
 *
 * TODO change name to ShowVersionedAction to conform to terminology.
 *
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="showArtifactAction" instantiation-strategy="per-lookup"
 */
public class ShowArtifactAction
    extends AbstractRepositoryBasedAction
    implements Validateable
{
    /* .\ Not Exposed \._____________________________________________ */

    /**
     * @plexus.requirement
     */
    private MetadataResolver metadataResolver;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    /* .\ Exposed Output Objects \.__________________________________ */

    private String groupId;

    private String artifactId;

    private String version;

    private String repositoryId;

    /**
     * The model of this versioned project.
     */
    private ProjectVersionMetadata model;

    /**
     * The list of artifacts that depend on this versioned project.
     */
    private List<ProjectVersionReference> dependees;

    private List<MailingList> mailingLists;

    private List<Dependency> dependencies;

    private Map<String, List<ArtifactDownloadInfo>> artifacts;

    private boolean dependencyTree = false;

    /**
     * Show the versioned project information tab.
     * TODO: Change name to 'project' - we are showing project versions here, not specific artifact information (though
     * that is rendered in the download box).
     */
    public String artifact()
    {
        ProjectVersionMetadata versionMetadata = null;
        artifacts = new LinkedHashMap<String, List<ArtifactDownloadInfo>>();

        List<String> repos = getObservableRepos();
        // In the future, this should be replaced by the repository grouping mechanism, so that we are only making
        // simple resource requests here and letting the resolver take care of it
        String errorMsg = null;
        for ( String repoId : repos )
        {
            if ( versionMetadata == null )
            {
                // we don't want the implementation being that intelligent - so another resolver to do the
                // "just-in-time" nature of picking up the metadata (if appropriate for the repository type) is used
                try
                {
                    versionMetadata = metadataResolver.getProjectVersion( repoId, groupId, artifactId, version );
                }
                catch ( MetadataResolutionException e )
                {
                    errorMsg = e.getMessage();
                }
                if ( versionMetadata != null )
                {
                    repositoryId = repoId;

                    List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>(
                        metadataResolver.getArtifacts( repoId, groupId, artifactId, version ) );
                    Collections.sort( artifacts, new Comparator<ArtifactMetadata>()
                    {
                        public int compare( ArtifactMetadata o1, ArtifactMetadata o2 )
                        {
                            // sort by version (reverse), then ID
                            // TODO: move version sorting into repository handling (maven2 specific), and perhaps add a 
                            //       way to get latest instead
                            int result = new DefaultArtifactVersion( o2.getVersion() ).compareTo(
                                new DefaultArtifactVersion( o1.getVersion() ) );
                            return result != 0 ? result : o1.getId().compareTo( o2.getId() );
                        }
                    } );

                    for ( ArtifactMetadata artifact : artifacts )
                    {
                        List<ArtifactDownloadInfo> l = this.artifacts.get( artifact.getVersion() );
                        if ( l == null )
                        {
                            l = new ArrayList<ArtifactDownloadInfo>();
                            this.artifacts.put( artifact.getVersion(), l );
                        }
                        l.add( new ArtifactDownloadInfo( artifact ) );
                    }
                }
            }
        }

        if ( versionMetadata == null )
        {
            addActionError( errorMsg != null ? errorMsg : "Artifact not found" );
            return ERROR;
        }
        model = versionMetadata;

        return SUCCESS;
    }

    /**
     * Show the artifact information tab.
     */
    public String dependencies()
    {
        String result = artifact();

        this.dependencies = model.getDependencies();

        return result;
    }

    /**
     * Show the mailing lists information tab.
     */
    public String mailingLists()
    {
        String result = artifact();

        this.mailingLists = model.getMailingLists();

        return result;
    }

    /**
     * Show the reports tab.
     */
    public String reports()
    {
        // TODO: hook up reports on project

        return SUCCESS;
    }

    /**
     * Show the dependees (other artifacts that depend on this project) tab.
     */
    public String dependees()
    {
        List<ProjectVersionReference> references = new ArrayList<ProjectVersionReference>();
        // TODO: what if we get duplicates across repositories?
        for ( String repoId : getObservableRepos() )
        {
            // TODO: what about if we want to see this irrespective of version?
            references.addAll( metadataResolver.getProjectReferences( repoId, groupId, artifactId, version ) );
        }

        this.dependees = references;

        // TODO: may need to note on the page that references will be incomplete if the other artifacts are not yet stored in the content repository
        // (especially in the case of pre-population import)

        return artifact();
    }

    /**
     * Show the dependencies of this versioned project tab.
     */
    public String dependencyTree()
    {
        // temporarily use this as we only need the model for the tag to perform, but we should be resolving the
        // graph here instead

        // TODO: may need to note on the page that tree will be incomplete if the other artifacts are not yet stored in the content repository
        // (especially in the case of pre-population import)

        // TODO: a bit ugly, should really be mapping all these results differently now
        this.dependencyTree = true;

        return artifact();
    }

    @Override
    public void validate()
    {
        if ( StringUtils.isBlank( groupId ) )
        {
            addActionError( "You must specify a group ID to browse" );
        }

        if ( StringUtils.isBlank( artifactId ) )
        {
            addActionError( "You must specify a artifact ID to browse" );
        }

        if ( StringUtils.isBlank( version ) )
        {
            addActionError( "You must specify a version to browse" );
        }
    }

    public ProjectVersionMetadata getModel()
    {
        return model;
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

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public List<MailingList> getMailingLists()
    {
        return mailingLists;
    }

    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    public List<ProjectVersionReference> getDependees()
    {
        return dependees;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public MetadataResolver getMetadataResolver()
    {
        return metadataResolver;
    }

    public Map<String, List<ArtifactDownloadInfo>> getArtifacts()
    {
        return artifacts;
    }

    public Collection<String> getSnapshotVersions()
    {
        return artifacts.keySet();
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }

    public boolean isDependencyTree()
    {
        return dependencyTree;
    }

    // TODO: move this into the artifact metadata itself via facets where necessary

    public class ArtifactDownloadInfo
    {
        private String type;

        private String namespace;

        private String project;

        private String size;

        private String id;

        private String repositoryId;

        private String version;

        private String path;

        public ArtifactDownloadInfo( ArtifactMetadata artifact )
        {
            repositoryId = artifact.getRepositoryId();

            // TODO: use metadata resolver capability instead
            ManagedRepositoryContent repo;
            try
            {
                repo = repositoryFactory.getManagedRepositoryContent( repositoryId );
            }
            catch ( RepositoryException e )
            {
                throw new RuntimeException( e );
            }

            ArtifactReference ref = new ArtifactReference();
            ref.setArtifactId( artifact.getProject() );
            ref.setGroupId( artifact.getNamespace() );
            ref.setVersion( artifact.getVersion() );
            path = repo.toPath( ref );
            path = path.substring( 0, path.lastIndexOf( "/" ) + 1 ) + artifact.getId();

            try
            {
                type = repo.toArtifactReference( path ).getType();
            }
            catch ( LayoutException e )
            {
                throw new RuntimeException( e );
            }

            namespace = artifact.getNamespace();
            project = artifact.getProject();

            // TODO: find a reusable formatter for this
            double s = artifact.getSize();
            String symbol = "b";
            if ( s > 1024 )
            {
                symbol = "K";
                s /= 1024;

                if ( s > 1024 )
                {
                    symbol = "M";
                    s /= 1024;

                    if ( s > 1024 )
                    {
                        symbol = "G";
                        s /= 1024;
                    }
                }
            }

            size = new DecimalFormat( "#,###.##" ).format( s ) + " " + symbol;
            id = artifact.getId();
            version = artifact.getVersion();
        }

        public String getNamespace()
        {
            return namespace;
        }

        public String getType()
        {
            return type;
        }

        public String getProject()
        {
            return project;
        }

        public String getSize()
        {
            return size;
        }

        public String getId()
        {
            return id;
        }

        public String getVersion()
        {
            return version;
        }

        public String getRepositoryId()
        {
            return repositoryId;
        }

        public String getPath()
        {
            return path;
        }
    }
}
