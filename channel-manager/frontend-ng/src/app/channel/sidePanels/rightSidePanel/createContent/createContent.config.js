/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
 */

import step1Template from './step1/step1.html';
import step2Template from './step2/step2.html';

function config($stateProvider) {
  'ngInject';

  $stateProvider.state({
    name: 'hippo-cm.channel.create-content-step-1',
    url: 'create-content-step-1',
    params: {
      config: {},
    },
    views: {
      tools: {
        template: '', // no tools
      },
      icon: {
        template: '', // no icon
      },
      main: {
        controller: 'step1Ctrl',
        controllerAs: '$ctrl',
        template: step1Template,
      },
    },
  });

  $stateProvider.state({
    name: 'hippo-cm.channel.create-content-step-2',
    url: 'create-content-step-2',
    params: {
      document: {},
      url: '',
      locale: '',
      componentInfo: {},
      xpage: false,
    },
    views: {
      tools: {
        template: '', // no tools
      },
      icon: {
        template: '', // no icon
      },
      main: {
        controller: 'step2Ctrl',
        controllerAs: '$ctrl',
        template: step2Template,
      },
    },
  });
}

export default config;
