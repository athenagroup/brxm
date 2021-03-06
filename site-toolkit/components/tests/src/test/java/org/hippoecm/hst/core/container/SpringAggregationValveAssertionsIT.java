/**
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.core.container;

import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

@Ignore
public class SpringAggregationValveAssertionsIT extends AbstractTestConfigurations {

    @Test
    public void confirm_default_site_pipeline_uses_own_instance_of_componentWindowResponseAppenders() {
        final AggregationValve aggregationValve = componentManager.getComponent("aggregationValve");

        final AggregationValve selectiveRenderingAggregationValve = componentManager.getComponent("selectiveRenderingAggregationValve");

        assertFalse("The aggregationValve and selectiveRenderingAggregationValve MUST have a different instance of the " +
                "List<ComponentWindowResponseAppender> because they need not share all appenders",
                aggregationValve.getComponentWindowResponseAppenders() == selectiveRenderingAggregationValve.getComponentWindowResponseAppenders());

    }

}
