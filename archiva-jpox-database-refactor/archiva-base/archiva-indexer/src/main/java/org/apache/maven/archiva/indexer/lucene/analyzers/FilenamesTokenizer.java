package org.apache.maven.archiva.indexer.lucene.analyzers;

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

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.maven.archiva.indexer.bytecode.BytecodeKeys;

import java.io.Reader;

/**
 * Lucene Tokenizer for {@link BytecodeKeys#FILES} fields. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FilenamesTokenizer extends CharTokenizer
{
    public FilenamesTokenizer( Reader reader )
    {
        super( reader );
    }

    /**
     * Determine Token Character.
     * 
     * The field is a list of full filenames "/home/archiva/foo/readme.txt" seperated by
     * newline characters. "\n".
     * 
     * Identify newline "\n" and "/" as the token delimiters.
     */
    protected boolean isTokenChar( char c )
    {
        return ( ( c != '\n' ) && ( c != '/' ) );
    }
}
