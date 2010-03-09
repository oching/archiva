package org.apache.archiva.metadata.repository.storage.maven2;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator" role-hint="maven2"
 */
public class Maven2RepositoryPathTranslator
    implements RepositoryPathTranslator
{
    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    public File toFile( File basedir, String namespace, String projectId, String projectVersion, String filename )
    {
        return new File( basedir, toPath( namespace, projectId, projectVersion, filename ) );
    }

    public File toFile( File basedir, String namespace, String projectId, String projectVersion )
    {
        return new File( basedir, toPath( namespace, projectId, projectVersion ) );
    }

    public String toPath( String namespace, String projectId, String projectVersion, String filename )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceToProjectVersion( path, namespace, projectId, projectVersion );
        path.append( PATH_SEPARATOR );
        path.append( filename );

        return path.toString();
    }

    private void appendNamespaceToProjectVersion( StringBuilder path, String namespace, String projectId,
                                                  String projectVersion )
    {
        appendNamespaceAndProject( path, namespace, projectId );
        path.append( projectVersion );
    }

    public String toPath( String namespace, String projectId, String projectVersion )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceToProjectVersion( path, namespace, projectId, projectVersion );

        return path.toString();
    }

    public String toPath( String namespace )
    {
        StringBuilder path = new StringBuilder();

        appendNamespace( path, namespace );

        return path.toString();
    }

    public String toPath( String namespace, String projectId )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceAndProject( path, namespace, projectId );

        return path.toString();
    }

    private void appendNamespaceAndProject( StringBuilder path, String namespace, String projectId )
    {
        appendNamespace( path, namespace );
        path.append( projectId ).append( PATH_SEPARATOR );
    }

    private void appendNamespace( StringBuilder path, String namespace )
    {
        path.append( formatAsDirectory( namespace ) ).append( PATH_SEPARATOR );
    }

    public File toFile( File basedir, String namespace, String projectId )
    {
        return new File( basedir, toPath( namespace, projectId ) );
    }

    public File toFile( File basedir, String namespace )
    {
        return new File( basedir, toPath( namespace ) );
    }

    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }

    private static String parseTimestampedVersionFromId( String projectId, String projectVersion, String id )
    {
        String mainVersion = projectVersion.substring( 0, projectVersion.length() - 8 ); // 8 is length of "SNAPSHOT"
        if ( mainVersion.length() == 0 )
        {
            throw new IllegalArgumentException(
                "Timestamped snapshots must contain the main version, filename was '" + id + "'" );
        }
        Matcher m = Pattern.compile( projectId + "-" + mainVersion + "([0-9]{8}.[0-9]{6}-[0-9]+).*" ).matcher( id );
        m.matches();
        return mainVersion + m.group( 1 );
    }

    public ArtifactMetadata getArtifactFromId( String repoId, String namespace, String projectId, String projectVersion,
                                               String id )
    {
        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.setId( id );
        metadata.setProject( projectId );
        metadata.setNamespace( namespace );
        metadata.setRepositoryId( repoId );

        if ( VersionUtil.isGenericSnapshot( projectVersion ) )
        {
            String version = parseTimestampedVersionFromId( projectId, projectVersion, id );

            metadata.setVersion( version );
        }
        else
        {
            metadata.setVersion( projectVersion );
        }
        return metadata;
    }

    public ArtifactMetadata getArtifactForPath( String repoId, String relativePath )
    {
        String[] parts = relativePath.replace( '\\', '/' ).split( "/" );

        int len = parts.length;
        if ( len < 4 )
        {
            throw new IllegalArgumentException(
                "Not a valid artifact path in a Maven 2 repository, not enough directories: " + relativePath );
        }

        String id = parts[--len];
        String baseVersion = parts[--len];
        String artifactId = parts[--len];
        String groupId = StringUtils.join( Arrays.copyOfRange( parts, 0, len ), '.' );

        if ( !id.startsWith( artifactId + "-" ) )
        {
            throw new IllegalArgumentException( "Not a valid artifact path in a Maven 2 repository, filename '" + id +
                "' doesn't start with artifact ID '" + artifactId + "'" );
        }

        int index = artifactId.length() + 1;
        String version;
        if ( id.substring( index ).startsWith( baseVersion ) && !VersionUtil.isUniqueSnapshot( baseVersion ) )
        {
            // non-snapshot versions, or non-timestamped snapshot versions
            version = baseVersion;
        }
        else if ( VersionUtil.isGenericSnapshot( baseVersion ) )
        {
            // timestamped snapshots
            try
            {
                version = parseTimestampedVersionFromId( artifactId, baseVersion, id );
            }
            catch ( IllegalStateException e )
            {
                throw new IllegalArgumentException(
                    "Not a valid artifact path in a Maven 2 repository, filename '" + id +
                        "' doesn't contain a timestamped version matching snapshot '" + baseVersion + "'" );
            }
        }
        else
        {
            // invalid
            throw new IllegalArgumentException(
                "Not a valid artifact path in a Maven 2 repository, filename '" + id + "' doesn't contain version '" +
                    baseVersion + "'" );
        }

        String classifier;
        String ext;
        index += version.length();
        if ( index == id.length() )
        {
            // no classifier or extension
            classifier = null;
            ext = null;
        }
        else
        {
            char c = id.charAt( index );
            if ( c == '-' )
            {
                // classifier up until last '.'
                int extIndex = id.lastIndexOf( '.' );
                if ( extIndex > index )
                {
                    classifier = id.substring( index + 1, extIndex );
                    ext = id.substring( extIndex + 1 );
                }
                else
                {
                    classifier = id.substring( index + 1 );
                    ext = null;
                }
            }
            else if ( c == '.' )
            {
                // rest is the extension
                classifier = null;
                ext = id.substring( index + 1 );
            }
            else
            {
                throw new IllegalArgumentException(
                    "Not a valid artifact path in a Maven 2 repository, filename '" + id +
                        "' expected classifier or extension but got '" + id.substring( index ) + "'" );
            }
        }

        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.setId( id );
        metadata.setNamespace( groupId );
        metadata.setProject( artifactId );
        metadata.setRepositoryId( repoId );
        metadata.setVersion( version );
        // TODO: set classifier and extension on Maven facet
        return metadata;
    }
}
