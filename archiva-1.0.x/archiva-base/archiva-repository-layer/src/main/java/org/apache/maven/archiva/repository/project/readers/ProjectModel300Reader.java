package org.apache.maven.archiva.repository.project.readers;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.License;
import org.apache.maven.archiva.model.MailingList;
import org.apache.maven.archiva.model.Organization;
import org.apache.maven.archiva.model.ProjectRepository;
import org.apache.maven.archiva.model.Scm;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLReader;
import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * ProjectModel300Reader 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.repository.project.ProjectModelReader"
 *      role-hint="model300"
 */
public class ProjectModel300Reader
    implements ProjectModelReader
{

    public ArchivaProjectModel read( File pomFile )
        throws ProjectModelException
    {
        try
        {
            XMLReader xml = new XMLReader( "project", pomFile );

            ArchivaProjectModel model = new ArchivaProjectModel();

            xml.removeNamespaces();

            Element project = xml.getElement( "//project" );

            // TODO: Handle <extend /> ?? (is this even possible?)

            model.setGroupId( project.elementTextTrim( "groupId" ) );
            model.setArtifactId( project.elementTextTrim( "artifactId" ) );
            // TODO: Handle <id />
            model.setVersion( project.elementTextTrim( "currentVersion" ) );
            model.setName( project.elementTextTrim( "name" ) );
            model.setDescription( project.elementTextTrim( "description" ) );
            // TODO: what to do with <shortDescription /> ?
            model.setUrl( project.elementTextTrim( "url" ) );
            // TODO: Handle <logo />
            // TODO: Handle <inceptionYear />

            model.setIssueManagement( getIssueManagement( xml ) );
            // TODO: What to do with <gumpRepositoryId /> ?
            // TODO: Handle <siteAddress />
            // TODO: Handle <siteDirectory /> ?
            // TODO: Handle <distributionSite />
            // TODO: Handle <distributionDirectory />
            model.setMailingLists( getMailingLists( xml ) );
            model.setIndividuals( getIndividuals( xml ) );
            model.setLicenses( getLicenses( xml ) );
            model.setReports( getReports( xml ) );
            model.setRepositories( getRepositories( xml ) );
            model.setScm( getSCM( xml ) );
            model.setOrganization( getOrganization( xml ) );
            model.setProperties( getProperties( xml.getElement( "//project/properties" ) ) );
            model.setDependencies( getDependencies( xml ) );
            
            /* Following are not valid for <pomVersion>3</pomVersion> / Maven 1 pom files.
             * 
             * model.setDependencyManagement()
             * model.setPlugins()
             * model.setParentProject()
             * model.setPackaging()
             * model.setCiManagement()
             * model.setBuildExtensions()
             * model.setRelocation()
             */

            return model;
        }
        catch ( XMLException e )
        {
            throw new ProjectModelException( e.getMessage(), e );
        }
    }

    private ArtifactReference getArtifactReference( Element elemPlugin, String defaultType )
    {
        ArtifactReference reference = new ArtifactReference();

        reference.setGroupId( StringUtils.defaultString( elemPlugin.elementTextTrim( "groupId" ) ) );
        reference.setArtifactId( elemPlugin.elementTextTrim( "artifactId" ) );
        reference.setVersion( StringUtils.defaultString( elemPlugin.elementTextTrim( "version" ) ) );
        reference.setClassifier( StringUtils.defaultString( elemPlugin.elementTextTrim( "classifier" ) ) );
        reference.setType( StringUtils.defaultIfEmpty( elemPlugin.elementTextTrim( "type" ), defaultType ) );

        return reference;
    }

    /**
     * Get List of {@link ArtifactReference} objects from xpath expr.
     */
    private List<ArtifactReference> getArtifactReferenceList( XMLReader xml, String xpathExpr, String defaultType )
        throws XMLException
    {
        List<ArtifactReference> refs = new ArrayList<ArtifactReference>();

        Iterator<Element> it = xml.getElementList( xpathExpr ).iterator();
        while ( it.hasNext() )
        {
            Element elemPlugin = it.next();

            refs.add( getArtifactReference( elemPlugin, defaultType ) );
        }

        return refs;
    }

    private List<Dependency> getDependencies( XMLReader xml )
        throws XMLException
    {
        return getDependencyList( xml, new String[] { "dependencies" } );
    }

    private List<Dependency> getDependencyList( XMLReader xml, String parts[] )
        throws XMLException
    {
        List<Dependency> dependencyList = new ArrayList<Dependency>();

        Element project = xml.getElement( "//project" );

        Element depsParent = project;

        for ( String part : parts )
        {
            depsParent = depsParent.element( part );
            if ( depsParent == null )
            {
                return dependencyList;
            }
        }

        Iterator<Element> it = depsParent.elementIterator( "dependency" );
        while ( it.hasNext() )
        {
            Element elemDependency = it.next();
            Dependency dependency = new Dependency();

            dependency.setGroupId( elemDependency.elementTextTrim( "groupId" ) );
            dependency.setArtifactId( elemDependency.elementTextTrim( "artifactId" ) );
            dependency.setVersion( elemDependency.elementTextTrim( "version" ) );

            dependency.setType( StringUtils.defaultIfEmpty( elemDependency.elementTextTrim( "type" ), "jar" ) );
            dependency.setUrl( elemDependency.elementTextTrim( "url" ) );
            /* Following are not valid for <pomVersion>3</pomVersion> / Maven 1 pom files.
             * 
             * dependency.setClassifier( StringUtils.defaultString( elemDependency.elementTextTrim( "classifier" ) ) );
             * dependency.setScope( StringUtils.defaultIfEmpty( elemDependency.elementTextTrim( "scope" ), "compile" ) );
             * dependency.setOptional( toBoolean( elemDependency.elementTextTrim( "optional" ), false ) );
             */

            dependency.setSystemPath( elemDependency.elementTextTrim( "jar" ) );

            if ( dependencyList.contains( dependency ) )
            {
                // TODO: throw into monitor as "duplicate dependency" issue.
            }

            dependencyList.add( dependency );
        }

        return dependencyList;
    }

    private List<Individual> getIndividuals( XMLReader xml )
        throws XMLException
    {
        List<Individual> individuals = new ArrayList<Individual>();

        individuals.addAll( getIndividuals( xml, true, "//project/developers/developer" ) );
        individuals.addAll( getIndividuals( xml, false, "//project/contributors/contributor" ) );

        return individuals;
    }

    private List<Individual> getIndividuals( XMLReader xml, boolean isCommitor, String xpathExpr )
        throws XMLException
    {
        List<Individual> ret = new ArrayList<Individual>();

        List<Element> modelPersonList = xml.getElementList( xpathExpr );

        Iterator<Element> iter = modelPersonList.iterator();
        while ( iter.hasNext() )
        {
            Element elemPerson = iter.next();
            Individual individual = new Individual();

            if ( isCommitor )
            {
                individual.setPrincipal( elemPerson.elementTextTrim( "id" ) );
            }

            individual.setCommitor( isCommitor );
            individual.setEmail( elemPerson.elementTextTrim( "email" ) );
            individual.setName( elemPerson.elementTextTrim( "name" ) );
            individual.setOrganization( elemPerson.elementTextTrim( "organization" ) );
            individual.setOrganizationUrl( elemPerson.elementTextTrim( "organizationUrl" ) );
            individual.setUrl( elemPerson.elementTextTrim( "url" ) );
            individual.setTimezone( elemPerson.elementTextTrim( "timezone" ) );

            // Roles
            Element elemRoles = elemPerson.element( "roles" );
            if ( elemRoles != null )
            {
                List<Element> roleNames = elemRoles.elements( "role" );
                Iterator<Element> itRole = roleNames.iterator();
                while ( itRole.hasNext() )
                {
                    Element role = itRole.next();
                    individual.addRole( role.getTextTrim() );
                }
            }

            // Properties
            individual.setProperties( getProperties( elemPerson.element( "properties" ) ) );

            ret.add( individual );
        }

        return ret;
    }

    private IssueManagement getIssueManagement( XMLReader xml )
        throws XMLException
    {
        Element issueTrackingUrlElem = xml.getElement( "//project/issueTrackingUrl" );

        if ( issueTrackingUrlElem == null )
        {
            return null;
        }

        String issueTrackingUrl = issueTrackingUrlElem.getTextTrim();
        if ( StringUtils.isBlank( issueTrackingUrl ) )
        {
            return null;
        }

        IssueManagement issueMgmt = new IssueManagement();
        issueMgmt.setUrl( issueTrackingUrl );

        return issueMgmt;
    }

    private List<License> getLicenses( XMLReader xml )
        throws XMLException
    {
        List<License> licenses = new ArrayList<License>();

        Element elemLicenses = xml.getElement( "//project/licenses" );

        if ( elemLicenses != null )
        {
            List<Element> licenseList = elemLicenses.elements( "license" );
            for ( Element elemLicense : licenseList )
            {
                License license = new License();

                // TODO: Create LicenseIdentity class to managed license ids.
                // license.setId( elemLicense.elementTextTrim("id") );
                license.setName( elemLicense.elementTextTrim( "name" ) );
                license.setUrl( elemLicense.elementTextTrim( "url" ) );
                license.setComments( elemLicense.elementTextTrim( "comments" ) );

                licenses.add( license );
            }
        }

        return licenses;
    }

    private List<MailingList> getMailingLists( XMLReader xml )
        throws XMLException
    {
        List<MailingList> mailingLists = new ArrayList<MailingList>();

        List<Element> mailingListElems = xml.getElementList( "//project/mailingLists/mailingList" );
        for ( Element elemMailingList : mailingListElems )
        {
            MailingList mlist = new MailingList();

            mlist.setName( elemMailingList.elementTextTrim( "name" ) );
            mlist.setSubscribeAddress( elemMailingList.elementTextTrim( "subscribe" ) );
            mlist.setUnsubscribeAddress( elemMailingList.elementTextTrim( "unsubscribe" ) );
            mlist.setPostAddress( elemMailingList.elementTextTrim( "post" ) );
            mlist.setMainArchiveUrl( elemMailingList.elementTextTrim( "archive" ) );

            Element elemOtherArchives = elemMailingList.element( "otherArchives" );
            if ( elemOtherArchives != null )
            {
                List<String> otherArchives = new ArrayList<String>();
                List<Element> others = elemOtherArchives.elements( "otherArchive" );
                for ( Element other : others )
                {
                    String otherArchive = other.getTextTrim();
                    otherArchives.add( otherArchive );
                }

                mlist.setOtherArchives( otherArchives );
            }

            mailingLists.add( mlist );
        }

        return mailingLists;
    }

    private Organization getOrganization( XMLReader xml )
        throws XMLException
    {
        Element elemOrg = xml.getElement( "//project/organization" );
        if ( elemOrg != null )
        {
            Organization org = new Organization();

            org.setName( elemOrg.elementTextTrim( "name" ) );
            org.setUrl( elemOrg.elementTextTrim( "url" ) );
            // TODO: Handle <logo />

            return org;
        }

        return null;
    }

    private Properties getProperties( Element elemProperties )
    {
        if ( elemProperties == null )
        {
            return null;
        }

        Properties ret = new Properties();

        Iterator<Element> itProps = elemProperties.elements().iterator();
        while ( itProps.hasNext() )
        {
            Element elemProp = (Element) itProps.next();
            ret.setProperty( elemProp.getName(), elemProp.getText() );
        }

        return ret;
    }

    private List<ArtifactReference> getReports( XMLReader xml )
        throws XMLException
    {
        return getArtifactReferenceList( xml, "//project/reports/plugins/plugin", "maven-plugin" );
    }

    private List<ProjectRepository> getRepositories( XMLReader xml )
        throws XMLException
    {
        List<ProjectRepository> repos = new ArrayList<ProjectRepository>();

        // Repositories are not stored within the maven 1 pom.

        return repos;
    }

    private Scm getSCM( XMLReader xml )
        throws XMLException
    {
        Element elemScm = xml.getElement( "//project/repository" );

        if ( elemScm != null )
        {
            Scm scm = new Scm();

            scm.setConnection( elemScm.elementTextTrim( "connection" ) );
            scm.setDeveloperConnection( elemScm.elementTextTrim( "developerConnection" ) );
            scm.setUrl( elemScm.elementTextTrim( "url" ) );

            return scm;
        }

        return null;
    }
}