/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

const REST_API_PATH = 'ws/projects';

class ProjectService {

  constructor($http, ConfigService, PathService) {
    'ngInject';

    this.$http = $http;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
  }

  projects(id) {
    const url = `${this.ConfigService.getCmsContextPath()}${REST_API_PATH}/${id}/channel`;
    return this.$http({ method: 'GET', url, headers: {}, data: {} })
      .then(result => result.data);
  }

  doCreateBranch(project) {
    // todo(mrop) add url
    const url = '';
    return this.$http({ method: 'POST', url, headers: {}, data: project });
  }

  doSelectBranch(project) {
    // todo(mrop) add url
    const url = '';
    return this.$http({ method: 'POST', url, headers: {}, data: project });
  }

}

export default ProjectService;
