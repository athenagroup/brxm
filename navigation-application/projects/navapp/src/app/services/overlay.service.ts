/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, scan } from 'rxjs/operators';

import { ConnectionService } from './connection.service';

@Injectable({
  providedIn: 'root',
})
export class OverlayService {

  visible$: Observable<boolean>;

  constructor(
    private connectionService: ConnectionService,
  ) {
    const counter = new BehaviorSubject<number>(0);
    this.connectionService.showMask$.subscribe(() => counter.next(+1));
    this.connectionService.hideMask$.subscribe(() => counter.next(-1));
    this.visible$ = counter.pipe(
      scan((acc, n) => acc + n),
      map(n => n > 0),
    );
  }
}
