/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

export default class LinkProcessorService {
  constructor($document, $rootScope, $translate, $window) {
    'ngInject';

    this.$document = $document;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.$window = $window;

    this._onClick = this._onClick.bind(this);
    this._onPageChange = this._onPageChange.bind(this);
  }

  initialize() {
    this.$rootScope.$on('page:change', this._onPageChange);
  }

  _onPageChange() {
    this.$document.find('a').each((index, el) => {
      const link = angular.element(el);
      const url = link.attr('href') || link.attr('xlink:href');

      if (!this._isExternal(url)) {
        return;
      }

      // Intercept all clicks on external links: open them in a new tab if confirmed by the user
      link
        .attr('target', '_blank')
        .off('click', this._onClick)
        .on('click', this._onClick);
    });
  }

  _isExternal(url) {
    // In preview mode the HST will render all internal links as paths, even the fully qualified
    // ones. So any link that starts with a scheme is an external one.
    return /^(?:[a-z][a-z0-9]+:)?\/\//.test(url);
  }

  _onClick(event) {
    // TODO: should use proper dialog!!
    if (!this.$window.confirm(this.$translate.instant('CONFIRM_OPEN_EXTERNAL_LINK'))) {
      event.preventDefault();
    }
  }
}
