<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>ALA DOI Repository</title>

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
        <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
        <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

    %{-- Google Analytics --}%
    <script>
        window.ga = window.ga || function () {
                    (ga.q = ga.q || []).push(arguments)
                };
        ga.l = +new Date;
        ga('create', '${grailsApplication.config.googleAnalyticsId}', 'auto');
        ga('send', 'pageview');
    </script>
    <script async src='//www.google-analytics.com/analytics.js'></script>
    %{--End Google Analytics--}%

    <r:require modules="doi"/>
</head>

<body>
<ala:systemMessage/>

<div class="col-sm-12 col-md-9 col-lg-9">
    <h1 class="hidden">Welcome to the Atlas of Living Australia</h1>
    <ol class="breadcrumb hidden-print">
        <li><a class="font-xxsmall" href="${grailsApplication.config.ala.base.url}">Home</a></li>
        <li class="font-xxsmall active">DOI Search Index</li>
    </ol>

    <h2 class="heading-medium">DOI search</h2>

    <div class="row">
        <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
            <div class="panel panel-default">
                <div class="panel-body">

                    <div class="word-limit">
                        <h1 class="heading-xlarge">Search for DOI records</h1>

                        <p class="lead color--primary-red">
                            A digital object identifier (DOI) is a unique alphanumeric string assigned by a registration agency (the International DOI Foundation) to identify content and provide a persistent link to its location on the Internet.
                        </p>

                        <g:if test="${false}"><!-- filter to be implemented at a later date -->
                            <div class="well">
                                <form class="form-horizontal">
                                    <div class="form-group">
                                        <label for="filter" class="col-sm-2 control-label">Search filter</label>

                                        <div class="col-sm-10">
                                            <div class="input-group">
                                                <input id="filter" type="text" class="form-control"
                                                       placeholder="Refine your display results">
                                                <span class="input-group-btn">
                                                    <a class="btn btn-default" type="button">Filter</a>
                                                </span>
                                            </div>

                                            <p class="help-block">Use the search filter to refine the display results below</p>
                                        </div>
                                    </div>
                                </form>
                            </div>
                        </g:if>

                        <div class="row">
                            <div class="col-md-offset-1 col-md-10">

                                <ol class="list-unstyled">
                                    <g:each in="${dois}" var="doi">
                                        <li>
                                            <h4 class="search-result"><a
                                                    href="${request.contextPath}/doi/${doi.uuid}">${doi.title}</a>
                                            </h4>

                                            <p class="help-block"><strong>DOI:</strong> <a href="http://dx.doi.org/${doi.doi}">${doi.doi}</a></p>

                                            <p class="help-block"><strong>Created:</strong> ${doi.dateMinted}</p>

                                            <p class="help-block"><strong>Author(s):</strong> ${doi.authors}</p>

                                            <p class="help-block">
                                                ${doi.description}
                                            </p>
                                        </li>
                                    </g:each>
                                </ol>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <g:if test="${dois.totalCount > pageSize}">
                <div class="small pull-right">Showing ${offset + 1} to ${Math.min(dois.totalCount, offset + pageSize)} of ${dois.totalCount}</div>

                <div class="clear"></div>

                <div class="row-fluid">
                    <nav class="col-sm-12 col-centered text-center">
                        <div class="pagination pagination-lg">
                            <g:paginate total="${dois.totalCount}" controller="doiResolve" action="index"
                                        omitLast="false" omitFirst="false" prev="&laquo;" next="&raquo;"
                                        max="${pageSize}" offset="${offset}"/>
                        </div>
                    </nav>
                </div>
            </g:if>
        </div>
        <div style="color:white;" class="pull-right">
            <g:link style="color:#DDDDDD; font-weight:bold;" controller="admin">Admin tools (${grailsApplication.config.skin.orgNameShort} administrators only)</g:link>
        </div>
    </div>
</div>
</body>
</html>
