/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Inject, Injectable } from '@angular/core';

import { Ng1PageStructureService, NG1_PAGE_STRUCTURE_SERVICE } from './ng1/page-structure.ng1.service';

@Injectable({
  providedIn: 'root',
})
export class PageStructureService {
  pageParsed$ = this.ng1PageStructureService.pageParsed$;

  constructor(
    @Inject(NG1_PAGE_STRUCTURE_SERVICE) private readonly ng1PageStructureService: Ng1PageStructureService,
  ) { }

  getPage(): any {
    return this.ng1PageStructureService.getPage();
  }

  getUnpublishedVariantId(): string {
    return this.ng1PageStructureService
      .getPage()
      ?.getMeta()
      ?.getUnpublishedVariantId();
  }
}
