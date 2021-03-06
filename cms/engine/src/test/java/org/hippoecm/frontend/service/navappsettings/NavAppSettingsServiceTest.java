/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.service.navappsettings;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.string.StringValue;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.filter.NavAppRedirectFilter;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.AppSettings;
import org.hippoecm.frontend.service.INavAppSettingsService;
import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.NavAppSettings;
import org.hippoecm.frontend.service.NgxLoggerLevel;
import org.hippoecm.frontend.service.ResourceType;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.repository.security.SessionUser;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hippoecm.frontend.service.NgxLoggerLevel.INFO;
import static org.hippoecm.frontend.service.NgxLoggerLevel.OFF;
import static org.hippoecm.frontend.service.NgxLoggerLevel.TRACE;
import static org.hippoecm.frontend.service.navappsettings.NavAppSettingsService.NAVIGATIONITEMS_ENDPOINT;
import static org.hippoecm.frontend.usagestatistics.UsageStatisticsSettings.SYSPROP_SEND_USAGE_STATISTICS_TO_HIPPO;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class NavAppSettingsServiceTest {

    static final String CMS_ICON_PNG = "cms-icon.png";
    @Mock
    private ServletWebRequest request;
    @Mock
    private IRequestParameters parameters;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private PluginUserSession userSession;
    @Mock
    private IPluginContext context;
    @Mock
    private IPluginConfig config;
    @Mock
    private NavAppResourceService navAppResourceService;

    private final String scheme = "scheme";
    private final String host = "cms.host.name";
    private final String contextPath = "/context-path";

    private INavAppSettingsService navAppSettingsService;

    @Mock
    private WebApplication webApplication;
    @Mock
    private HippoSession hippoSession;
    @Mock
    private SessionUser user;

    private SessionAttributeStore sessionAttributeStore;

    @Before
    public void setUp() throws RepositoryException {

        expect(request.getContainerRequest()).andReturn(servletRequest).anyTimes();
        expect(request.getQueryParameters()).andStubReturn(parameters);
        replay(request);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);

        expect(servletRequest.getHeader("X-Forwarded-Proto")).andReturn(scheme);
        expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn(host).times(2);
        replay(servletRequest);

        expect(userSession.getJcrSession()).andReturn(hippoSession);
        expect(userSession.getUserName()).andReturn("userName");
        expect(userSession.getLocale()).andReturn(Locale.CANADA);
        expect(userSession.getTimeZone()).andReturn(TimeZone.getDefault());
        replay(userSession);


        expect(hippoSession.getUser()).andReturn(user);
        replay(hippoSession);

        expect(user.getEmail()).andReturn("email");
        expect(user.getFirstName()).andReturn("firstname");
        expect(user.getLastName()).andReturn("lastname");
        replay(user);

        expect(config.getString(INavAppSettingsService.SERVICE_ID, INavAppSettingsService.SERVICE_ID)).andReturn(null);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 60_000)).andReturn(10_000);
        expect(config.getString(NavAppSettingsService.LOG_LEVEL, OFF.name())).andReturn(OFF.name());
        replay(config);

        sessionAttributeStore = new SessionAttributeStore() {

            final private Map<String, Serializable> attributes = new HashMap<>();

            @Override
            public Serializable getAttribute(final String name) {
                return attributes.get(name);
            }

            @Override
            public Session setAttribute(final String name, final Serializable value) {
                attributes.put(name, value);
                return null;
            }
        };
        expect(navAppResourceService.getLoginResources()).andStubReturn(Collections.emptySet());
        expect(navAppResourceService.getLogoutResources()).andStubReturn(Collections.emptySet());
        expect(navAppResourceService.getNavigationItemsResources()).andStubReturn(Collections.emptySet());
        expect(navAppResourceService.getNavAppResourceUrl()).andStubReturn(Optional.empty());
        replay(navAppResourceService);

        this.navAppSettingsService = new NavAppSettingsService(context, config, () -> userSession, () -> sessionAttributeStore, navAppResourceService);

        ThreadContext.setApplication(webApplication);
        System.setProperty(SYSPROP_SEND_USAGE_STATISTICS_TO_HIPPO, Boolean.TRUE.toString());
    }

    @After
    public void tearDown() {
        ThreadContext.setApplication(null);
        System.clearProperty(SYSPROP_SEND_USAGE_STATISTICS_TO_HIPPO);
    }


    @Test
    public void loads_extra_navcfg_resources_from_config() {

        reset(servletRequest);
        expect(servletRequest.getHeader("X-Forwarded-Proto")).andReturn(scheme);
        expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn(null).times(2);
        expect(servletRequest.getHeader("Host")).andReturn(host).times(2);
        expect(servletRequest.getContextPath()).andReturn("/context-path");
        replay(servletRequest);

        final NavAppResource navAppResource1 = new NavAppResourceBuilder().resourceType(ResourceType.IFRAME).resourceUrl(URI.create("some-other-url1")).build();
        final NavAppResource navAppResource2 = new NavAppResourceBuilder().resourceType(ResourceType.INTERNAL_REST).resourceUrl(URI.create("some-other-url2")).build();

        reset(navAppResourceService);
        expect(navAppResourceService.getLoginResources()).andStubReturn(Collections.emptySet());
        expect(navAppResourceService.getLogoutResources()).andStubReturn(Collections.emptySet());
        expect(navAppResourceService.getNavigationItemsResources()).andReturn(Stream.of(navAppResource1, navAppResource2).collect(Collectors.toSet()));
        expect(navAppResourceService.getNavAppResourceUrl()).andStubReturn(null);
        expect(navAppResourceService.getNavAppResourceUrl()).andReturn(Optional.empty());
        replay(navAppResourceService);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        final List<NavAppResource> navConfigResources = navAppSettings.getAppSettings().getNavConfigResources();
        assertThat(navConfigResources.size(), is(3));
        testAppSettingsAssertions(navAppSettings.getAppSettings());

        final NavAppResource iframeResource = navConfigResources.stream().filter(r -> ResourceType.IFRAME == r.getResourceType()).findFirst().orElseThrow(() -> new RuntimeException("IFRAME resource not found"));
        assertThat(iframeResource.getUrl(), is(URI.create("some-other-url1")));

        verify(config);
    }


    @Test
    public void is_resilient_to_RepositoryExceptions() throws RepositoryException {
        reset(hippoSession);
        expect(hippoSession.getUser()).andThrow(new ItemNotFoundException("can always happen"));
        replay(hippoSession);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getUserSettings().getEmail(), is(nullValue()));

        verify(hippoSession);
    }

    @Test
    public void load_login_and_logout_resources() {

        final NavAppResource navAppResource = createMock(NavAppResource.class);
        expect(navAppResource.getResourceType()).andReturn(ResourceType.IFRAME);
        expect(navAppResource.getResourceType()).andReturn(ResourceType.REST);
        replay(navAppResource);

        reset(navAppResourceService);
        expect(navAppResourceService.getNavigationItemsResources()).andReturn(Collections.emptySet());
        expect(navAppResourceService.getLoginResources()).andReturn(Collections.singleton(navAppResource));
        expect(navAppResourceService.getLogoutResources()).andReturn(Collections.singleton(navAppResource));
        expect(navAppResourceService.getNavAppResourceUrl()).andReturn(Optional.empty());
        replay(navAppResourceService);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);

        final List<NavAppResource> loginResources = navAppSettings.getAppSettings().getLoginResources();
        assertThat(loginResources.size(), is(1));
        assertThat(loginResources.get(0).getResourceType(), is(ResourceType.IFRAME));

        final List<NavAppResource> logoutResources = navAppSettings.getAppSettings().getLogoutResources();
        assertThat(loginResources.size(), is(1));
        assertThat(logoutResources.get(0).getResourceType(), is(ResourceType.REST));

        verify(config);
    }

    @Test
    public void initial_path_is_set() {
        reset(parameters);
        final String someInitialPath = "a/b/c?x=y&p=q";
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andReturn(StringValue.valueOf(someInitialPath));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);
        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getInitialPath(), is(someInitialPath));
    }

    @Test
    public void uuid_parameter_is_set() {
        reset(parameters);
        final String someUUID = "{mock-uuid}";
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andReturn(StringValue.valueOf(someUUID));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);
        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getInitialPath(), is("/content/uuid/" + someUUID));
    }

    @Test
    public void path_parameter_is_set() {
        reset(parameters);
        final String somePath = "  / //some/path /to /document";
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf(somePath));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);
        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getInitialPath(), is("/content/path/some/path/to/document"));
    }

    @Test
    public void loads_no_resources_for_logintype_local() {

        reset(config);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 60_000)).andReturn(10_000);
        expect(config.getString(NavAppSettingsService.LOG_LEVEL, OFF.name())).andReturn(OFF.name());
        replay(config);

        reset(parameters);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf("local"));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getNavConfigResources().size(), is(1));
        verify(config, parameters);
    }

    @Test
    public void loads_no_resources_for_logintype_local_reload() {

        reset(config);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 60_000)).andReturn(10_000);
        expect(config.getString(NavAppSettingsService.LOG_LEVEL, OFF.name())).andReturn(OFF.name());
        replay(config);


        reset(parameters);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);
        sessionAttributeStore.setAttribute(NavAppSettingsService.LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME, true);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getNavConfigResources().size(), is(1));
        verify(config, parameters);
    }

    @Test
    public void uses_logLevel_from_repository() {

        final NgxLoggerLevel info = INFO;
        reset(config);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 60_000)).andReturn(10_000);
        expect(config.getString(NavAppSettingsService.LOG_LEVEL, OFF.name())).andReturn(info.name());
        replay(config);


        reset(parameters);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);
        sessionAttributeStore.setAttribute(NavAppSettingsService.LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME, true);

        final AppSettings appSettings = navAppSettingsService.getNavAppSettings(request).getAppSettings();
        assertThat(appSettings.getLogLevel(), is(info));

        verify(config, parameters);
    }

    @Test
    public void uses_logLevel_from_query_parameter() {

        reset(config);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 60_000)).andReturn(10_000);
        replay(config);

        reset(parameters);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andReturn(StringValue.valueOf(("Trace")));
        replay(parameters);
        sessionAttributeStore.setAttribute(NavAppSettingsService.LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME, true);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getLogLevel(), is(TRACE));
        verify(config, parameters);
    }

    @Test
    public void returns_default_if_logLevel_from_query_parameter_is_invalid() {

        reset(config);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 60_000)).andReturn(10_000);
        replay(config);

        reset(parameters);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOG_LEVEL))
                .andReturn(StringValue.valueOf("TRACY"));
        replay(parameters);
        sessionAttributeStore.setAttribute(NavAppSettingsService.LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME, true);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        testAppSettingsAssertions(navAppSettings.getAppSettings());
        verify(config, parameters);
    }


    private void testAppSettingsAssertions(AppSettings appSettings) {
        // First resource must always be present
        final List<NavAppResource> navConfigResources = appSettings.getNavConfigResources();
        assertThat(navConfigResources.get(0).getResourceType(), is(ResourceType.INTERNAL_REST));
        assertThat(navConfigResources.get(0).getUrl(), is(URI.create(NAVIGATIONITEMS_ENDPOINT)));
        assertThat(appSettings.getInitialPath(), is("/"));
        assertThat(appSettings.getLogLevel(), is(OFF));
        assertThat(appSettings.getNavAppResourceLocation(), is(URI.create("angular/navapp")));
        assertThat(appSettings.isCmsServingNavAppResources(), is(true));
        assertThat(appSettings.isUsageStatisticsEnabled(), is(true));
    }

}
