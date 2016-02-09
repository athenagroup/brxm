/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

function startsWith(str, prefix) {
  if (str === undefined || prefix === undefined) {
    return false;
  }
  return prefix === str.slice(0, prefix.length);
}

export class LinkProcessorService {

  constructor($translate) {
    'ngInject';

    this.$translate = $translate;
  }

  run(document, internalLinkPrefix) {
    angular.element(document).find('a').each((index, el) => {
      const link = angular.element(el);
      let url = link.prop('href');

      // handle links within SVG elements
      if (url instanceof SVGAnimatedString) {
        url = url.baseVal;
      }

      // intercept all clicks on external links: open them in a new tab if confirmed by the user
      if (!startsWith(url, internalLinkPrefix)) {
        link.attr('target', '_blank');
        link.click((event) => {
          if (!confirm(this.$translate.instant('CONFIRM_OPEN_EXTERNAL_LINK'))) {
            event.preventDefault();
          }
        });
      }
    });
  }
}
