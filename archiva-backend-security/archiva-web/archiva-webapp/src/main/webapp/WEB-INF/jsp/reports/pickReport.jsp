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
  <title>Reports</title>
  <ww:head/>
</head>

<body>
<h1>Reports</h1>

<div id="contentArea">

  <ww:form action="generateReport" namespace="/report" validate="true">
    <ww:textfield label="Row Count" name="rowCount" value="100"/>
    <ww:textfield label="Group ID" name="groupId"/>
    <ww:select label="Repository ID" name="repositoryId" list="repositoryIds"/>
    <ww:submit value="Show Report"/>
  </ww:form>

</div>

</body>
</html>