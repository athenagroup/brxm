/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.service.render;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.hippoecm.frontend.service.IFocusListener;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that bundles a lot of the functionality needed for visual
 * plugins.  It registers an {@link IRenderService}, with itself as the {@link Component}.
 * <p>
 * The configuration parameters are as follows, with the class of the expected
 * service between brackets:
 * <ul>
 * <li><b>wicket.model (IModelReference)</b>
 * The {@link IModel} that is available with the getModel() method, is the model
 * that is provided by a {@link IModelReference} service.  The name for this service
 * can be found in the configuration with the <code>wicket.model</code> key.  When
 * the model is changed with setModel(), this change is propagated to the IModelReference
 * service.  When another plugin changes the model or the model object changes, the
 * Component#onModelChanged() method is invoked.  It is recommended to override this method to
 * respond to changes.
 * <li><b>wicket.id (IRenderService)</b>
 * The primary task of the RenderPlugin is to provide an {@link IRenderService}
 * implementation.
 * <li><b>wicket.extensions (IRenderService)</b>
 * A list of service names for child render services.  These child services will be
 * added to the plugin directly.
 * <li><b>wicket.behavior</b>
 * A list of service names for {@link IBehaviorService}s.  The behaviors that are exposed
 * by these services are added to the Component.
 * <li><b>wicket.variant (Layout)</b>
 * The layout variantion to use.  In contrast with Wickets default, the variation is not
 * inherited from the parent.
 * <li><b>wicket.skin (CSS stylesheet)</b>
 * An array of stylesheets that will be added to the HTML head.
 * <li><b>wicket.feedback (CSS)</b>
 * An array of CSS class names that will be added to the {@link Component} that is provided
 * by this render service.
 * <li><b>translator.id (Localization)</b>
 * It is possible to retrieve translations by invoking the {@link ITranslateService}.  This
 * will be done when the <code>translator.id</code> service name is available.  The configured
 * services (it is a multi-valued property) are invoked in order.
 * </ul>
 */
public abstract class AbstractRenderService<T> extends Panel implements IObserver, IRenderService,
        IStringResourceProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AbstractRenderService.class);

    public static final String WICKET_ID = "wicket.id";
    public static final String MODEL_ID = "wicket.model";
    public static final String VARIANT_ID = "wicket.variant";
    public static final String SKIN_ID = "wicket.skin";
    public static final String CSS_ID = "wicket.css";
    public static final String EXTENSIONS_ID = "wicket.extensions";
    public static final String FEEDBACK = "wicket.feedback";
    public static final String BEHAVIOR = "wicket.behavior";

    private boolean redraw;
    private String wicketServiceId;
    private String wicketId;
    private String cssClasses;

    private final IPluginContext context;
    private final IPluginConfig config;
    private final IModelReference modelReference;
    protected final LinkedHashMap<String, ExtensionPoint> children;
    private IRenderService parent;

    public AbstractRenderService(IPluginContext context, IPluginConfig properties) {
        super("id", getPluginModel(context, properties));
        this.context = context;
        this.config = properties;

        setOutputMarkupId(true);
        redraw = false;

        wicketId = "service.render";

        this.children = new LinkedHashMap<String, ExtensionPoint>();

        this.wicketServiceId = properties.getName() + ".service.wicket.id";

        if (properties.getString(MODEL_ID) != null) {
            modelReference = context.getService(properties.getString(MODEL_ID), IModelReference.class);
            if (modelReference != null) {
                context.registerService(new IObserver<IModelReference<?>>() {
                    private static final long serialVersionUID = 1L;

                    public IModelReference<?> getObservable() {
                        return modelReference;
                    }

                    public void onEvent(Iterator<? extends IEvent<IModelReference<?>>> event) {
                        updateModel(modelReference.getModel());
                    }

                }, IObserver.class.getName());
            }
        } else {
            modelReference = null;
        }
        if (getDefaultModel() instanceof IObservable) {
            context.registerService(this, IObserver.class.getName());
        }

        String[] extensions = config.getStringArray(EXTENSIONS_ID);
        if (extensions != null) {
            for (String extension : extensions) {
                addExtensionPoint(extension);
            }
        }

        StringBuffer sb;

        cssClasses = null;
        String[] classes = config.getStringArray(CSS_ID);
        if (classes != null) {
            sb = null;
            for (String cssClass : classes) {
                if (sb == null) {
                    sb = new StringBuffer(cssClass);
                } else {
                    sb.append(" ").append(cssClass);
                }
            }
            if (sb != null) {
                cssClasses = sb.toString();
            }
        }

        String[] skins = config.getStringArray(SKIN_ID);
        if (skins != null) {
            for (String skin : skins) {
                HeaderContributor cssContributor = HeaderContributorHelper.forCss(skin);
                add(cssContributor);
                context.registerService(cssContributor, IDialogService.class.getName());
            }
        }

        if (config.getString(FEEDBACK) != null) {
            context.registerService(new ContainerFeedbackMessageFilter(this), config.getString(FEEDBACK));
        } else {
            log.debug("No feedback id {} defined to register message filter", FEEDBACK);
        }

        if (config.getStringArray(BEHAVIOR) != null) {
            ServiceTracker<IBehaviorService> tracker = new ServiceTracker<IBehaviorService>(IBehaviorService.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onServiceAdded(IBehaviorService service, String name) {
                    String path = service.getComponentPath();
                    if (path != null) {
                        Component component = get(path);
                        if (component != null) {
                            component.add(service.getBehavior());
                        } else {
                            log.warn("No component found under {}", path);
                        }
                    } else {
                        add(service.getBehavior());
                    }
                }

                @Override
                public void onRemoveService(IBehaviorService service, String name) {
                    String path = service.getComponentPath();
                    if (path != null) {
                        Component component = get(service.getComponentPath());
                        if (component != null) {
                            component.remove(service.getBehavior());
                        }
                    } else {
                        remove(service.getBehavior());
                    }
                }
            };

            for (String name : config.getStringArray(BEHAVIOR)) {
                context.registerTracker(tracker, name);
            }
        }

        context.registerService(this, config.getString("wicket.id"));
    }

    // utility methods

    @SuppressWarnings("unchecked")
    public IModel<T> getModel() {
        return (IModel<T>) getDefaultModel();
    }

    @SuppressWarnings("unchecked")
    public T getModelObject() {
        return (T) getDefaultModelObject();
    }

    public final void setModel(IModel<T> model) {
        setDefaultModel(model);
    }

    public final void setModelObject(T object) {
        setDefaultModelObject(object);
    }

    // override model change methods

    @Override
    public MarkupContainer setDefaultModel(IModel<?> model) {
        if (modelReference != null) {
            modelReference.setModel(model);
        } else {
            updateModel(model);
        }
        return this;
    }

    public final void updateModel(IModel model) {
        if (getDefaultModel() instanceof IObservable) {
            context.unregisterService(this, IObserver.class.getName());
        }
        super.setDefaultModel(model);
        if (getDefaultModel() instanceof IObservable) {
            context.registerService(this, IObserver.class.getName());
        }
    }

    public IObservable getObservable() {
        return (IObservable) getDefaultModel();
    }

    public void onEvent(Iterator event) {
        modelChanged();
    }

    // override methods with configuration data

    @Override
    public String getVariation() {
        if (config.getString(VARIANT_ID) != null) {
            return config.getString(VARIANT_ID);
        }
        // don't inherit variation from Wicket ancestor
        return null;
    }

    // utility routines for subclasses

    /**
     * The {@link IPluginContext} that was used to create the service.
     */
    protected IPluginContext getPluginContext() {
        return context;
    }

    /**
     * The {@link IPluginConfig} that was used to create the service.
     */
    protected IPluginConfig getPluginConfig() {
        return config;
    }

    /**
     * Utility method for subclasses to redraw the complete {@link Panel}.
     * When invoked during request processing or event handling, the service will
     * be rendered during the rendering phase.
     * <p>
     * For more fine-grained redrawing, subclasses should override the
     * {@link AbstractRenderService#render(PluginRequestTarget)} method.
     */
    protected void redraw() {
        redraw = true;
    }

    protected void addExtensionPoint(final String extension) {
        ExtensionPoint extPt = createExtensionPoint(extension);
        children.put(extension, extPt);
        extPt.register();
    }

    /**
     * Create an extension point with the specified name.
     */
    protected abstract ExtensionPoint createExtensionPoint(String extension);

    protected void removeExtensionPoint(String name) {
        ExtensionPoint extPt = children.get(name);
        extPt.unregister();
        children.remove(name);
    }

    /**
     * Utility method to retrieve the {@link IDialogService}} from the plugin
     * framework.  The dialog service is guaranteed to be available.
     */
    protected IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    // allow styling

    @Override
    public void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);

        if (cssClasses != null) {
            tag.put("class", cssClasses);
        }
    }

    // implement IRenderService

    /**
     * {@inheritDoc}
     */
    public Component getComponent() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void render(PluginRequestTarget target) {
        if (redraw) {
            if (target != null) {
                target.addComponent(this);
            }
        }
        for (Map.Entry<String, ExtensionPoint> entry : children.entrySet()) {
            for (IRenderService service : entry.getValue().getChildren()) {
                service.render(target);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void focus(IRenderService child) {
        List<IFocusListener> listeners = context.getServices(context.getReference(this).getServiceId(),
                IFocusListener.class);
        for (IFocusListener listener : listeners) {
            listener.onFocus(this);
        }
        IRenderService parent = getParentService();
        if (parent != null) {
            parent.focus(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return wicketId;
    }

    @Override
    public String getMarkupId(boolean createIfDoesNotExist) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(wicketServiceId.getBytes(), 0, wicketServiceId.length());
            return new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void bind(IRenderService parent, String wicketId) {
        this.parent = parent;
        this.wicketId = wicketId;
    }

    public void unbind() {
        this.parent = null;
        wicketId = "service.render.unbound";
    }

    /**
     * {@inheritDoc}
     */
    public IRenderService getParentService() {
        return parent;
    }

    // implement IStringResourceProvider
    public String getString(Map<String, String> criteria) {
        String[] translators = config.getStringArray(ITranslateService.TRANSLATOR_ID);
        if (translators != null) {
            for (String translatorId : translators) {
                ITranslateService translator = (ITranslateService) context.getService(translatorId,
                        ITranslateService.class);
                if (translator != null) {
                    String translation = translator.translate(criteria);
                    if (translation != null) {
                        return translation;
                    }
                }
            }
        }
        return null;
    }

    private static IModel getPluginModel(IPluginContext context, IPluginConfig properties) {
        String modelId = properties.getString(MODEL_ID);
        if (modelId != null) {
            IModelReference service = context.getService(modelId, IModelReference.class);
            if (service != null) {
                return service.getModel();
            }
        }
        return null;
    }

    @Override
    protected void onBeforeRender() {
        redraw = false;
        super.onBeforeRender();
    }
    
    /**
     * Base class for extension points.  Registers as a {@link ServiceTracker} for a
     * {@link IRenderService} extension.
     */
    protected abstract class ExtensionPoint extends ServiceTracker<IRenderService> {
        private static final long serialVersionUID = 1L;

        private final List<IRenderService> list;
        protected String extension;

        protected ExtensionPoint(String extension) {
            super(IRenderService.class);
            this.extension = extension;
            this.list = new LinkedList<IRenderService>();
        }

        protected void register() {
            if (config.containsKey(extension)) {
                context.registerTracker(this, config.getString(extension));
            } else {
                log.debug("Extension {} has not been configured; not registering tracker", extension);
            }
        }

        protected void unregister() {
            if (config.containsKey(extension)) {
                context.unregisterTracker(this, config.getString(extension));
            }
        }

        public List<IRenderService> getChildren() {
            return list;
        }

        @Override
        public void onServiceAdded(IRenderService service, String name) {
            list.add(service);
            redraw();
        }

        @Override
        public void onServiceChanged(IRenderService service, String name) {
        }

        @Override
        public void onRemoveService(IRenderService service, String name) {
            list.remove(service);
            redraw();
        }
    }
}
