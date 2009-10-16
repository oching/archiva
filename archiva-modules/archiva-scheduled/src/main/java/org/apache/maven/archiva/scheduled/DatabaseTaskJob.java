package org.apache.maven.archiva.scheduled;

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

import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.codehaus.plexus.scheduler.AbstractJob;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This class is the database job that is executed by the scheduler.
 */
public class DatabaseTaskJob
    extends AbstractJob
{
    /**
     * Execute the discoverer and the indexer.
     *
     * @param context
     * @throws org.quartz.JobExecutionException
     *
     */
    public void execute( JobExecutionContext context )
        throws JobExecutionException
    {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        setJobDataMap( dataMap );

        TaskQueue taskQueue = (TaskQueue) dataMap.get( DefaultArchivaTaskScheduler.TASK_QUEUE );

        Task task = new DatabaseTask();

        try
        {
            // The database job only needs to run one at a time
            if ( taskQueue.getQueueSnapshot().isEmpty() )
            {
                taskQueue.put( task );
            }
        }
        catch ( TaskQueueException e )
        {
            throw new JobExecutionException( e );
        }
    }
}
