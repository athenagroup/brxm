<!doctype html>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%@ page isErrorPage="true" %>
<% response.setStatus(404); %>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <title>404 error</title>
</head>
<body>
<h2>Welcome to Hippo</h2>
<p>
  It appears that you just created an empty Hippo project from the archetype. There is nothing to
  show on the site yet. We recommend you use Hippo Essentials to start developing your project.
</p>
<h3>
  <a href="http://<%=request.getServerName() + ':' + request.getServerPort() + "/essentials"%>">Go to Hippo Essentials</a>
</h3>
</body>
</html>