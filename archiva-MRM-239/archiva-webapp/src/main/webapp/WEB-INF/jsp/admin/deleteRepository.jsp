<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>

<html>
<head>
  <title>Configuration</title>
  <ww:head/>
</head>

<body>

<h1>Configuration</h1>

<div id="contentArea">

  <h2>Delete Managed Repository</h2>

  <blockquote>
    <strong><span class="statusFailed">WARNING:</span> This operation can not be undone.</strong>
  </blockquote>

  <ww:form method="post" action="deleteRepository" namespace="/admin" validate="true">
    <ww:hidden name="repoId"/>
    <ww:radio list="#@java.util.LinkedHashMap@{'delete-contents' : 'Remove the repository and delete its contents from disk',
    'delete-entry' : 'Remove the repository from the management list, but leave the contents unmodified',
    'unmodified' : 'Leave the repository unmodified'}" name="operation" theme="archiva"/>
    <ww:submit value="Go"/>
  </ww:form>
</div>

</body>
</html>