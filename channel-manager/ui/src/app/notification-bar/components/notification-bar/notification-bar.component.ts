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

import { ChangeDetectorRef, Component, ElementRef, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { XPageStatusInfo } from '../../../models/page-status-info.model';
import { XPageStatus } from '../../../models/xpage-status.enum';
import { PageService } from '../../../services/page.service';

const DANGER_XPAGE_STATUSES = [
  XPageStatus.RejectedRequest,
  XPageStatus.ProjectPageRejected,
  XPageStatus.EditingSharedContainers,
  XPageStatus.Locked,
];

@Component({
  templateUrl: 'notification-bar.component.html',
  styleUrls: ['notification-bar.component.scss'],
})
export class NotificationBarComponent implements OnInit, OnDestroy {
  pageStatusInfo: XPageStatusInfo | undefined;

  private readonly unsubscribe = new Subject();

  constructor(
    private readonly pageService: PageService,
    private readonly cd: ChangeDetectorRef,
    private readonly elfRef: ElementRef,
  ) {}

  ngOnInit(): void {
    this.pageService.pageStatusInfo$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(pageStatusInfo => {
      this.pageStatusInfo = pageStatusInfo;
      const danger = DANGER_XPAGE_STATUSES.some(x => x === pageStatusInfo?.status);

      if (danger) {
        this.elfRef.nativeElement.classList.add('danger');
      } else {
        this.elfRef.nativeElement.classList.remove('danger');
      }

      this.cd.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }
}
