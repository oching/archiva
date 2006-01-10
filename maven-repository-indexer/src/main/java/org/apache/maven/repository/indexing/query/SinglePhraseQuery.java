package org.apache.maven.repository.indexing.query;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Class to hold a single field search condition
 *
 * @author Edwin Punzalan
 */
public class SinglePhraseQuery
    implements Query
{
    private String field;

    private String value;

    /**
     * Class constructor
     *
     * @param field the index field to search
     * @param value the index value requirement
     */
    public SinglePhraseQuery( String field, String value )
    {
        this.field = field;
        this.value = value;
    }

    /**
     * Method to retrieve the name of the index field searched
     *
     * @return the name of the index field
     */
    public String getField()
    {
        return field;
    }

    /**
     * Method to retrieve the value used in searching the index field
     *
     * @return the value to corresspond the index field
     */
    public String getValue()
    {
        return value;
    }
}
