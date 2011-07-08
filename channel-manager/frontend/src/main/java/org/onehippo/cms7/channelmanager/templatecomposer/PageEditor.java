/**
  * Copyright 2011 Hippo
  *
  * Licensed under the Apache License, Version 2.0 (the  "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
*/
package org.onehippo.cms7.channelmanager.templatecomposer;

import java.util.Arrays;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.templatecomposer.iframe.IFrameBundle;
import org.onehippo.cms7.jquery.JQueryBundle;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;

@ExtClass("Hippo.App.PageEditor")
public class PageEditor extends ExtComponent {

    @ExtProperty
    public Boolean debug = false;

    @ExtProperty
    public String composerMountUrl = "/site/manager";

    @ExtProperty
    public String renderHostSubMountPath = "";

    @ExtProperty
    public String renderHost = "localhost";

    @ExtProperty
    public String renderHostParameterName = ContainerConstants.RENDERING_HOST;

    public PageEditor(final String id, final IPluginConfig config) {
        super(id);
        this.composerMountUrl = config.getString("composerMountUrl", "/site/manager");
        this.renderHost = config.getString("renderHost", "localhost");
        this.renderHostSubMountPath = config.getString("renderHostSubMountPath", "");
        this.debug = Application.get().getDebugSettings().isAjaxDebugModeEnabled();

        add(CSSPackageResource.getHeaderContribution(PageEditor.class, "plugins/colorfield/colorfield.css"));
    }

    @Override
    public void buildInstantiationJs(StringBuilder js, String extClass, JSONObject properties) {
        js.append(String.format(" try { Ext.namespace(\"%s\"); window.%s = new %s(%s); } catch (e) { Ext.Msg.alert(e); }; \n", getMarkupId(), getMarkupId(), extClass, properties.toString()));
    }

    @Override
    public String getMarkupId() {
        return "Hippo.App.Main";
    }

    @Override
    protected void onRenderProperties(final JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        RequestCycle rc = RequestCycle.get();
        properties.put("iFrameCssHeadContributions", Arrays.asList(
            rc.urlFor(new ResourceReference(PageEditor.class, "pageeditor/PageEditor.css")).toString())
        );
        if (debug) {
            properties.put("iFrameJsHeadContributions", Arrays.asList(
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CORE)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_CLASS_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_NAMESPACE_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_HEAD_PLUGIN)).toString(),
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_UI)).toString(),

                rc.urlFor(new ResourceReference(PageEditor.class, "globals.js")).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.UTIL)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.FACTORY)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.MANAGER)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.WIDGETS)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.MAIN)).toString()
            ));
        } else {
            properties.put("iFrameJsHeadContributions", Arrays.asList(
                rc.urlFor(new ResourceReference(JQueryBundle.class, JQueryBundle.JQUERY_ALL_MIN)).toString(),
                rc.urlFor(new ResourceReference(IFrameBundle.class, IFrameBundle.ALL)).toString()
            ));
        }
    }
}
