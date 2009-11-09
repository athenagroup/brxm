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
package org.hippoecm.frontend.widgets;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateFieldWidget extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DateFieldWidget.class);

    public DateFieldWidget(String id, IModel model) {
        this(id, model, false);
    }
    
    public DateFieldWidget(String id, IModel model, boolean todayLinkVisible) {
        super(id, model);

        final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) getModel();
        Date date;
        try {
            date = valueModel.getValue().getDate().getTime();
        } catch (RepositoryException ex) {
            // FIXME:  log error
            date = null;
        }

        add(new AjaxDateTimeField("widget", new Model(date) {
            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(Object object) {
                Calendar calendar = new GregorianCalendar();
                calendar.setTime((Date) object);
                try {
                    valueModel.setValue(valueModel.getJcrPropertymodel().getProperty().getSession().getValueFactory()
                            .createValue(calendar));
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage(), ex);
                }
                super.setObject(object);
            }

        }, todayLinkVisible));
    }

}
