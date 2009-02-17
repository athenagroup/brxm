package org.hippoecm.hst.core.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hippoecm.hst.configuration.BasicHstSiteMapMatcher;
import org.hippoecm.hst.configuration.ConfigurationViewUtilities;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher.MatchResult;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

public class TestConfiguration extends AbstractSpringTestCase {

    protected static final String TESTPROJECT_NAME = "testproject";

    private HstSites hstSites;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.hstSites = (HstSites) getComponent(HstSites.class.getName());
    }

    @Test
    public void testConfiguration() {
        HstSite s = hstSites.getSite(TESTPROJECT_NAME);

        HstSite s2 = hstSites.getSite("nonexistingproject");
        assertNull(s2);

        HstSiteMapItem sItem = s.getSiteMap().getSiteMapItem("news");
        HstComponentConfiguration c = s.getComponentsConfiguration().getComponentConfiguration(
                sItem.getComponentConfigurationId());
        assertNotNull(c);

    }

    public void testPathMatcher() {

        StringBuffer buf = new StringBuffer();

        HstSite hstSite = hstSites.getSite(TESTPROJECT_NAME);

        HstSiteMapMatcher hstSiteMapMatcher = new BasicHstSiteMapMatcher();

        MatchResult matchNoResult = hstSiteMapMatcher.match("/non/exist/ing", hstSite);
        assertNull(matchNoResult.getSiteMapItem());
        assertNull(matchNoResult.getCompontentConfiguration());
        assertEquals(matchNoResult.getRemainder(), "non/exist/ing");

        MatchResult matchResult = hstSiteMapMatcher.match("/news/foo/bar", hstSite);
        assertEquals(matchResult.getRemainder(), "foo/bar");
        assertEquals(matchResult.getSiteMapItem().getId(), "news");

        ConfigurationViewUtilities.view(buf, matchResult);
        assertTrue("Buffer should not be empty", buf.length() > 0);

        assertEquals(matchResult.getSiteMapItem().getChild("inland").getId(), "news/inland");
        assertEquals(matchResult.getCompontentConfiguration().getId(), "pages/newsoverview");

        matchResult = hstSiteMapMatcher.match("/news/foo/bar/", hstSite);
        assertEquals(matchResult.getRemainder(), "foo/bar");

        matchResult = hstSiteMapMatcher.match("/news", hstSite);
        assertEquals(matchResult.getRemainder(), "");

    }

}
