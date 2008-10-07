package org.apache.maven.archiva.reporting;

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

import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.model.RepositoryContentStatistics;

/**
 * RepositoryStatisticsReportGenerator
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id: RepositoryStatisticsReportGenerator.java
 */
public interface RepositoryStatisticsReportGenerator
{
    public static final String JAR_TYPE = "jar";
    
    public static final String WAR_TYPE = "war";
    
    public static final String POM_TYPE = "pom";
    
    public static final String MAVEN_PLUGIN = "maven-plugin";
    
    public static final String ARCHETYPE = "archetype";
    
    public List<RepositoryStatistics> generateReport( List<RepositoryContentStatistics> repoContentStats, String repository, Date startDate, Date endDate, DataLimits limits )
        throws ArchivaReportException;
    
    public List<RepositoryStatistics> generateReport( List<RepositoryContentStatistics> repoContentStats, String repository, Date startDate, Date endDate )
        throws ArchivaReportException;
}