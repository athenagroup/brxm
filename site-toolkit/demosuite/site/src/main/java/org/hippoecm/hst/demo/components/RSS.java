package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.beans.GeneralPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSS extends BaseHstComponent {
    
    public static final Logger log = LoggerFactory.getLogger(RSS.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        // TODO Auto-generated method stub
        super.doBeforeRender(request, response);

        HippoBean contentBean = this.getContentBean(request);
        if (contentBean == null) {
            log.error("Content path defined for RSS feed is invalid.");
            return;
        }

        HstQueryManager manager = getQueryManager();
        try {

            final HstQuery query = manager.createQuery(request.getRequestContext(), contentBean);
            query.addOrderByDescending("demosite:date");
            
            final HstQueryResult result = query.execute();
            List<HippoBean> results = new ArrayList<HippoBean>();

            final HippoBeanIterator iterator = result.getHippoBeans();

            while (iterator.hasNext()) {
                HippoBean bean = iterator.nextHippoBean();
                // note: bean can be null
                if (bean != null && bean instanceof GeneralPage) {
                    GeneralPage pageBean = (GeneralPage)bean;
                    results.add(bean);
                }
            }
            request.setAttribute("items", results);

        } catch (QueryException e) {
            log.error("Exception in RSS feed component: " + e);
        }
        
        request.setAttribute("today", new Date());
    
    }

}
