/*!
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

import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavItemService } from '../services/nav-item.service';
import { NavigationService } from '../services/navigation.service';

import { BootstrapService } from './bootstrap.service';

describe('BootstrapService', () => {
  let service: BootstrapService;

  let appConnectedSubject: Subject<ClientApp>;
  let clientAppServiceInitResolve: () => void;
  let clientAppServiceMock: jasmine.SpyObj<ClientAppService>;

  let menuStateServiceInitResolve: () => void;
  let menuStateServiceMock: jasmine.SpyObj<MenuStateService>;

  let navigationServiceInitialNavigationResolve: () => void;
  let navigationServiceMock: jasmine.SpyObj<NavigationService>;

  let navItemServiceMock: jasmine.SpyObj<NavItemService>;

  beforeEach(() => {
    appConnectedSubject = new Subject();
    clientAppServiceMock = jasmine.createSpyObj('ClientAppService', {
      init: new Promise(r => {
        clientAppServiceInitResolve = r;
      }),
    });
    (clientAppServiceMock as any).appConnected$ = appConnectedSubject.asObservable();

    menuStateServiceMock = jasmine.createSpyObj('MenuStateService', {
      init: new Promise(r => {
        menuStateServiceInitResolve = r;
      }),
    });

    navigationServiceMock = jasmine.createSpyObj('NavigationService', {
      initialNavigation: new Promise(r => {
        navigationServiceInitialNavigationResolve = r;
      }),
    });

    navItemServiceMock = jasmine.createSpyObj('NavItemService', [
      'activateNavItems',
    ]);

    TestBed.configureTestingModule({
      providers: [
        BootstrapService,
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: NavItemService, useValue: navItemServiceMock },
      ],
    });

    service = TestBed.get(BootstrapService);
  });

  it('should call ClientAppService:init() first', fakeAsync(() => {
    service.bootstrap();

    tick();

    expect(clientAppServiceMock.init).toHaveBeenCalled();
    expect(menuStateServiceMock.init).not.toHaveBeenCalled();
    expect(navigationServiceMock.initialNavigation).not.toHaveBeenCalled();
  }));

  describe('when ClientAppService is initialized', () => {
    let bootstrapped = false;

    beforeEach(async(() => {
      service.bootstrap().then(() => bootstrapped = true);

      clientAppServiceInitResolve();
    }));

    it('should activate nav items for the connected app', () => {
      const expected = 'https://some-url';

      appConnectedSubject.next(new ClientApp(expected, {}));

      expect(navItemServiceMock.activateNavItems).toHaveBeenCalledWith(expected);
    });

    it('should call MenuStateService:init()', () => {
      expect(menuStateServiceMock.init).toHaveBeenCalled();
      expect(navigationServiceMock.initialNavigation).not.toHaveBeenCalled();
    });

    describe('when MenuStateService is initialized', () => {
      beforeEach(async(() => {
        menuStateServiceInitResolve();
      }));

      it('should call NavigationService:initialNavigation()', () => {
        expect(navigationServiceMock.initialNavigation).toHaveBeenCalled();
      });

      describe('after initial navigation', () => {
        beforeEach(async(() => {
          navigationServiceInitialNavigationResolve();
        }));

        it('should resolve the promise', () => {
          expect(bootstrapped).toBeTruthy();
        });
      });
    });
  });
});
