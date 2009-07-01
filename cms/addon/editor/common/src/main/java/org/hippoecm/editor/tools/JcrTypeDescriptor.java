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
package org.hippoecm.editor.tools;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeDescriptorEvent;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeDescriptor extends JcrObject implements ITypeDescriptor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeDescriptor.class);

    private String name;
    private String type;
    private transient Map<String, IFieldDescriptor> fields;
    private transient Map<String, IObserver> observers = new TreeMap<String, IObserver>();
    private transient JcrFieldDescriptor primary;

    public JcrTypeDescriptor(JcrNodeModel nodeModel) throws RepositoryException {
        super(nodeModel);

        Node typeNode = nodeModel.getNode();
        Node templateTypeNode = typeNode;
        while (!templateTypeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            templateTypeNode = templateTypeNode.getParent();
        }

        String prefix = templateTypeNode.getParent().getName();
        if ("system".equals(prefix)) {
            name = templateTypeNode.getName();
        } else {
            name = prefix + ":" + templateTypeNode.getName();
        }

        if (typeNode.hasProperty(HippoNodeType.HIPPO_TYPE)) {
            type = typeNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
        } else {
            type = name;
        }

        primary = null;
        fields = loadFields();
        for (IFieldDescriptor field : fields.values()) {
            if (field.isPrimary()) {
                primary = (JcrFieldDescriptor) field;
                break;
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<String> getSuperTypes() {
        try {
            Node node = getNode();
            List<String> superTypes = new LinkedList<String>();
            if (node.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                Value[] values = node.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
                for (Value value : values) {
                    superTypes.add(value.getString());
                }
            }
            return superTypes;
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public void setSuperTypes(List<String> superTypes) {
        try {
            String[] types = superTypes.toArray(new String[superTypes.size()]);
            getNode().setProperty(HippoNodeType.HIPPO_SUPERTYPE, types);

            updateFields();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public Map<String, IFieldDescriptor> getFields() {
        return fields;
    }

    public IFieldDescriptor getField(String key) {
        return getFields().get(key);
    }

    public boolean isNode() {
        try {
            if (getNode().hasProperty(HippoNodeType.HIPPO_NODE)) {
                return getNode().getProperty(HippoNodeType.HIPPO_NODE).getBoolean();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return true;
    }

    public void setIsNode(boolean isNode) {
        setBoolean(HippoNodeType.HIPPO_NODE, isNode);
    }

    public boolean isMixin() {
        return getBoolean("hippo:mixin");
    }

    public void setIsMixin(boolean isMixin) {
        setBoolean("hippo:mixin", isMixin);
    }

    public Value createValue() {
        try {
            int propertyType = PropertyType.valueFromName(type);
            switch (propertyType) {
            case PropertyType.BOOLEAN:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(false);
            case PropertyType.DATE:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(
                        Calendar.getInstance());
            case PropertyType.DOUBLE:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(0.0);
            case PropertyType.LONG:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(0L);
            case PropertyType.NAME:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue("",
                        PropertyType.NAME);
            case PropertyType.PATH:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue("/",
                        PropertyType.PATH);
            case PropertyType.REFERENCE:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(
                        UUID.randomUUID().toString(), PropertyType.REFERENCE);
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue("",
                        PropertyType.STRING);
            default:
                return null;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    public void addField(IFieldDescriptor descriptor) {
        try {
            Node typeNode = getNode();
            Node field = typeNode.addNode(HippoNodeType.HIPPO_FIELD, HippoNodeType.NT_FIELD);
            JcrFieldDescriptor desc = new JcrFieldDescriptor(new JcrNodeModel(field), this);
            desc.copy(descriptor);

            updateFields();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void removeField(String field) {
        try {
            Node fieldNode = getFieldNode(field);
            if (fieldNode != null) {
                IFieldDescriptor descriptor = new JcrFieldDescriptor(new JcrNodeModel(fieldNode), this);

                if (descriptor.isPrimary()) {
                    primary = null;
                }
                fieldNode.remove();
            } else {
                log.warn("field " + field + " was not found in type " + type);
            }

            updateFields();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void setPrimary(String name) {
        getFields();
        if (primary != null) {
            if (!primary.getName().equals(name)) {
                primary.setPrimary(false);
                primary = null;
            } else {
                return;
            }
        }

        IFieldDescriptor field = fields.get(name);
        if (field != null) {
            ((JcrFieldDescriptor) field).setPrimary(true);
            primary = (JcrFieldDescriptor) field;
        } else {
            log.warn("field " + name + " was not found");
        }

        updateFields();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrTypeDescriptor) {
            JcrTypeDescriptor that = (JcrTypeDescriptor) object;
            return new EqualsBuilder().append(this.name, that.name).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(53, 7).append(this.name).toHashCode();
    }

    protected Map<String, IFieldDescriptor> loadFields() {
        Map<String, IFieldDescriptor> fields = new HashMap<String, IFieldDescriptor>();
        try {
            Node node = getNode();
            if (node != null) {
                NodeIterator it = node.getNodes();
                while (it.hasNext()) {
                    Node child = it.nextNode();
                    if (child != null && child.isNodeType(HippoNodeType.NT_FIELD)) {
                        JcrFieldDescriptor field = new JcrFieldDescriptor(new JcrNodeModel(child), this);
                        fields.put(field.getName(), field);
                    }
                }
            }
            Set<String> explicit = new HashSet<String>();
            for (IFieldDescriptor field : fields.values()) {
                if (!field.getPath().equals("*")) {
                    explicit.add(field.getPath());
                }
            }
            for (IFieldDescriptor field : fields.values()) {
                if (field.getPath().equals("*")) {
                    field.setExcluded(explicit);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return fields;
    }

    private boolean getBoolean(String path) {
        try {
            if (getNode().hasProperty(path)) {
                return getNode().getProperty(path).getBoolean();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return false;
    }

    private void setBoolean(String path, boolean value) {
        try {
            getNode().setProperty(path, value);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private Node getFieldNode(String field) throws RepositoryException {
        Node typeNode = getNode();
        NodeIterator fieldIter = typeNode.getNodes(HippoNodeType.HIPPO_FIELD);
        while (fieldIter.hasNext()) {
            Node fieldNode = fieldIter.nextNode();
            if (fieldNode.hasProperty(HippoNodeType.HIPPO_NAME)) {
                String name = fieldNode.getProperty(HippoNodeType.HIPPO_NAME).getString();
                if (name.equals(field)) {
                    return fieldNode;
                }
            }
        }
        return null;
    }

    @Override
    public void startObservation() {
        super.startObservation();
        for (IFieldDescriptor field : fields.values()) {
            subscribe(field);
        }
    }

    @Override
    public void stopObservation() {
        for (IFieldDescriptor field : fields.values()) {
            unsubscribe(field);
        }
        super.stopObservation();
    }
    
    private void subscribe(final IFieldDescriptor field) {
        final IObservationContext obContext = getObservationContext();
        IObserver observer = new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return field;
            }

            public void onEvent(Iterator<? extends IEvent> events) {
                EventCollection<TypeDescriptorEvent> collection = new EventCollection<TypeDescriptorEvent>();
                while (events.hasNext()) {
                    IEvent event = events.next();
                    if (event instanceof TypeDescriptorEvent) {
                        collection.add((TypeDescriptorEvent) event);
                    }
                }
                if (collection.size() > 0) {
                    obContext.notifyObservers(collection);
                }
            }

        };
        observers.put(field.getName(), observer);
        obContext.registerObserver(observer);
    }

    private void unsubscribe(IFieldDescriptor field) {
        final IObservationContext obContext = getObservationContext();
        IObserver observer = observers.remove(field.getName());
        obContext.unregisterObserver(observer);
    }

    protected void processEvents(EventCollection collection) {
        IObservationContext obContext = getObservationContext();
        Map<String, IFieldDescriptor> newFields = loadFields();
        for (String field : newFields.keySet()) {
            if (!fields.containsKey(field)) {
                fields.put(field, newFields.get(field));
                collection.add(new TypeDescriptorEvent(this, fields.get(field),
                        TypeDescriptorEvent.EventType.FIELD_ADDED));
                if (obContext != null) {
                    IFieldDescriptor descriptor = newFields.get(field);
                    subscribe(descriptor);
                }
            }
        }
        Iterator<Entry<String, IFieldDescriptor>> iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, IFieldDescriptor> entry = iter.next();
            if (!newFields.containsKey(entry.getKey())) {
                collection.add(new TypeDescriptorEvent(this, entry.getValue(),
                        TypeDescriptorEvent.EventType.FIELD_REMOVED));
                iter.remove();
                if (obContext != null) {
                    IFieldDescriptor descriptor = entry.getValue();
                    unsubscribe(descriptor);
                }
            }
        }
    }

    protected void updateFields() {
        EventCollection collection = new EventCollection();
        processEvents(collection);
        if (collection.size() > 0) {
            IObservationContext obContext = getObservationContext();
            if (obContext != null) {
                obContext.notifyObservers(collection);
            }
        }
    }

}
