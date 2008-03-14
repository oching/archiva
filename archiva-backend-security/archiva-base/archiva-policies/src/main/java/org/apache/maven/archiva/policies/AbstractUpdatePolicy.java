package org.apache.maven.archiva.policies;

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
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

/**
 * AbstractUpdatePolicy 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractUpdatePolicy
    extends AbstractLogEnabled
    implements PreDownloadPolicy
{
    /**
     * The DISABLED policy means that the artifact retrieval isn't even attempted,
     * let alone updated locally.
     */
    public static final String DISABLED = "disabled";

    /**
     * <p>
     * The DAILY policy means that the artifact retrieval occurs only if one of
     * the following conditions are met...
     * </p>
     * <ul>
     *   <li>The local artifact is not present.</li>
     *   <li>The local artifact has a last modified timestamp older than (now - 1 day).</li>
     * </ul>
     */
    public static final String DAILY = "daily";

    /**
     * <p>
     * The HOURLY policy means that the artifact retrieval occurs only if one of
     * the following conditions are met...
     * </p>
     * <ul>
     *   <li>The local artifact is not present.</li>
     *   <li>The local artifact has a last modified timestamp older than (now - 1 hour).</li>
     * </ul>
     */
    public static final String HOURLY = "hourly";

    /**
     * The ONCE policy means that the artifact retrieval occurs only if the
     * local artifact is not present.  This means that the retreival can only
     * occur once.
     */
    public static final String ONCE = "once";

    private List options = new ArrayList();

    public AbstractUpdatePolicy()
    {
        options.add( IGNORED );
        options.add( DISABLED );
        options.add( DAILY );
        options.add( HOURLY );
        options.add( ONCE );
    }

    protected abstract boolean isSnapshotPolicy();
    
    protected abstract String getUpdateMode();
    
    public List getOptions()
    {
        return options;
    }

    public boolean applyPolicy( String policySetting, Properties request, File localFile )
    {
        String version = request.getProperty( "version", "" );
        boolean isSnapshotVersion = false;

        if ( StringUtils.isNotBlank( version ) )
        {
            isSnapshotVersion = VersionUtil.isSnapshot( version );
        }

        if ( !options.contains( policySetting ) )
        {
            // No valid code? false it is then.
            getLogger().error( "Unknown artifact-update policyCode [" + policySetting + "]" );
            return false;
        }

        if ( IGNORED.equals( policySetting ) )
        {
            // Ignored means ok to update.
            getLogger().debug( "OK to update, " + getUpdateMode() + " policy set to IGNORED." );
            return true;
        }

        // Test for mismatches.
        if ( !isSnapshotVersion && isSnapshotPolicy() )
        {
            getLogger().debug( "OK to update, snapshot policy does not apply for non-snapshot versions." );
            return true;
        }

        if ( isSnapshotVersion && !isSnapshotPolicy() )
        {
            getLogger().debug( "OK to update, release policy does not apply for snapshot versions." );
            return true;
        }

        if ( DISABLED.equals( policySetting ) )
        {
            // Disabled means no.
            getLogger().debug( "NO to update, " + getUpdateMode() + " policy set to DISABLED." );
            return false;
        }

        if ( !localFile.exists() )
        {
            // No file means it's ok.
            getLogger().debug( "OK to update " + getUpdateMode() + ", local file does not exist." );
            return true;
        }

        if ( ONCE.equals( policySetting ) )
        {
            // File exists, but policy is once.
            getLogger().debug( "NO to update" + getUpdateMode() + ", local file exist (and policy is ONCE)." );
            return false;
        }

        if ( DAILY.equals( policySetting ) )
        {
            Calendar cal = Calendar.getInstance();
            cal.add( Calendar.DAY_OF_MONTH, -1 );
            Calendar fileCal = Calendar.getInstance();
            fileCal.setTimeInMillis( localFile.lastModified() );

            return cal.after( fileCal );
        }

        if ( HOURLY.equals( policySetting ) )
        {
            Calendar cal = Calendar.getInstance();
            cal.add( Calendar.HOUR, -1 );
            Calendar fileCal = Calendar.getInstance();
            fileCal.setTimeInMillis( localFile.lastModified() );

            return cal.after( fileCal );
        }

        getLogger().error( "Unhandled policyCode [" + policySetting + "]" );
        return false;
    }
}