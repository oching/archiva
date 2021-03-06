package org.apache.archiva.repository.scanner;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;

/**
 * ScanConsumer 
 *
 * @version $Id$
 */
public class KnownScanConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private int processCount = 0;

    private List<String> includes = new ArrayList<String>();

    private boolean processUnmodified = false;

    public List<String> getExcludes()
    {
        return null;
    }

    public void setIncludes( String includesArray[] )
    {
        this.includes.clear();
        this.includes.addAll( Arrays.asList( includesArray ) );
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public String getId()
    {
        return "test-scan-consumer";
    }

    public String getDescription()
    {
        return "Scan Consumer (for testing)";
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered )
        throws ConsumerException
    {
        /* do nothing */
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );
    }

    public void processFile( String path )
        throws ConsumerException
    {
        this.processCount++;
    }

    public void processFile( String path, boolean executeOnEntireRepo )
        throws Exception
    {
        processFile( path );
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
       completeScan();
    }

    public int getProcessCount()
    {
        return processCount;
    }

    public void setProcessCount( int processCount )
    {
        this.processCount = processCount;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public boolean isProcessUnmodified()
    {
        return processUnmodified;
    }

    public void setProcessUnmodified( boolean processUnmodified )
    {
        this.processUnmodified = processUnmodified;
    }
}
