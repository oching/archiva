package org.apache.maven.archiva.indexer.bytecode;

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

import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.List;

/**
 * Lucene Record for Bytecode information.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class BytecodeRecord
    implements LuceneRepositoryContentRecord
{
    private String repositoryId;

    private ArchivaArtifact artifact;

    private String filename;

    private List classes;

    private List methods;

    private List files;

    public ArchivaArtifact getArtifact()
    {
        return artifact;
    }

    public List getClasses()
    {
        return classes;
    }

    public List getFiles()
    {
        return files;
    }

    public List getMethods()
    {
        return methods;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public String getPrimaryKey()
    {
        StringBuffer id = new StringBuffer();
        id.append( artifact.getGroupId() ).append( ":" );
        id.append( artifact.getArtifactId() ).append( ":" );
        id.append( artifact.getVersion() );

        if ( artifact.getClassifier() != null )
        {
            id.append( ":" ).append( artifact.getClassifier() );
        }

        id.append( ":" ).append( artifact.getType() );

        return id.toString();
    }

    public void setArtifact( ArchivaArtifact artifact )
    {
        this.artifact = artifact;
    }

    public void setClasses( List classes )
    {
        this.classes = classes;
    }

    public void setFiles( List files )
    {
        this.files = files;
    }

    public void setMethods( List methods )
    {
        this.methods = methods;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( artifact == null ) ? 0 : artifact.hashCode() );
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final BytecodeRecord other = (BytecodeRecord) obj;

        if ( artifact == null )
        {
            if ( other.artifact != null )
            {
                return false;
            }
        }
        else if ( !artifact.equals( other.artifact ) )
        {
            return false;
        }

        return true;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename( String filename )
    {
        this.filename = filename;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "BytecodeRecord[" );
        sb.append( "artifact=" ).append( artifact );
        sb.append( ",filename=" ).append( filename );
        sb.append( "]" );
        return sb.toString();
    }

}