<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="headerName" type="java.lang.String"--%>
<div class="container-fluid">
  <div class="row-fluid">
    <div class="span2"></div>
    <div class="span8">
      <div class="navbar">
        <div class="navbar-inner">
          <h1 class="container">
            <hst:link var="homeLink" path="/"/>
            <h1><a class="brand" href="${homeLink}">${fn:escapeXml(headerName)}</a></h1>
          </h1>
        </div>
      </div>
    </div>
    <div class="span2"></div>
  </div>
</div>