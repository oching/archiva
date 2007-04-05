package org.apache.maven.archiva.repository.project.filters;

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

import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.codehaus.plexus.evaluator.DefaultExpressionEvaluator;
import org.codehaus.plexus.evaluator.EvaluatorException;
import org.codehaus.plexus.evaluator.ExpressionEvaluator;
import org.codehaus.plexus.evaluator.sources.PropertiesExpressionSource;
import org.codehaus.plexus.evaluator.sources.SystemPropertyExpressionSource;

import java.util.Iterator;
import java.util.List;

/**
 * ProjectModelExpressionExpander 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repository.project.ProjectModelExpressionExpander"
 */
public class ProjectModelExpressionExpander
{
    /**
     * Find and Evaluate the Expressions present in the model.
     * 
     * @param model the model to correct.
     */
    public static void evaluateExpressions( ArchivaProjectModel model )
        throws ProjectModelException
    {
        ExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

        if ( model.getProperties() != null )
        {
            PropertiesExpressionSource propsSource = new PropertiesExpressionSource();
            propsSource.setProperties( model.getProperties() );
            evaluator.addExpressionSource( propsSource );
        }

        evaluator.addExpressionSource( new SystemPropertyExpressionSource() );

        try
        {
            model.setVersion( evaluator.expand( model.getVersion() ) );
            model.setGroupId( evaluator.expand( model.getGroupId() ) );

            evaluateExpressionsInDependencyList( evaluator, model.getDependencies() );
            evaluateExpressionsInDependencyList( evaluator, model.getDependencyManagement() );
        }
        catch ( EvaluatorException e )
        {
            throw new ProjectModelException( "Unable to evaluate expression in model: " + e.getMessage(), e );
        }
    }

    private static void evaluateExpressionsInDependencyList( ExpressionEvaluator evaluator, List dependencies )
        throws EvaluatorException
    {
        if ( dependencies == null )
        {
            return;
        }

        Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dependency = (Dependency) it.next();
            dependency.setGroupId( evaluator.expand( dependency.getGroupId() ) );
            dependency.setVersion( evaluator.expand( dependency.getVersion() ) );
        }
    }
}
