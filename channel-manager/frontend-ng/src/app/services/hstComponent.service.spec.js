/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('HstComponentService', () => {
  let $q;
  let $rootScope;
  let ChannelService;
  let HstComponentService;
  let HstService;
  let CmsServiceMock;
  let onPathPicked;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    CmsServiceMock = jasmine.createSpyObj('CmsServiceMock', [
      'getConfig',
      'publish',
      'subscribe',
    ]);

    CmsServiceMock.subscribe.and.callFake((id, cb) => {
      onPathPicked = cb;
    });

    angular.mock.module(($provide) => {
      $provide.value('CmsService', CmsServiceMock);
    });

    inject((_$q_, _$rootScope_, _ChannelService_, _HstService_, _HstComponentService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      HstService = _HstService_;
      HstComponentService = _HstComponentService_;
    });
  });

  describe('interaction with the CMS through the "path-picked" event', () => {
    it('subscribes to the CMS event "path-picked" upon construction', () => {
      expect(CmsServiceMock.subscribe).toHaveBeenCalledWith('path-picked', jasmine.any(Function));
    });

    it('responds to callbacks with id "component-path-picker"', () => {
      const pathPickedHandler = spyOn(HstComponentService, 'pathPickedHandler');
      onPathPicked('random-id', '/path/picked');
      expect(pathPickedHandler).not.toHaveBeenCalled();

      onPathPicked('component-path-picker', '/path/picked');
      expect(pathPickedHandler).toHaveBeenCalledWith('/path/picked');
      expect(HstComponentService.pathPickedHandler).toBe(angular.noop);
    });

    it('resets the pathPickedHandler after a succesfull callback', () => {
      HstComponentService.pathPickedHandler = () => {};
      onPathPicked('random-id');
      expect(HstComponentService.pathPickedHandler).not.toBe(angular.noop);

      onPathPicked('component-path-picker');
      expect(HstComponentService.pathPickedHandler).toBe(angular.noop);
    });
  });

  describe('pickPath', () => {
    let pickPathPromise;

    beforeEach(() => {
      pickPathPromise = HstComponentService.pickPath('id', 'variant', 'name', 'value', 'pickerConfig', 'basePath');
    });

    it('publishes a "show-path-picker" event to the CMS application', () => {
      expect(CmsServiceMock.publish).toHaveBeenCalledWith('show-path-picker', 'component-path-picker', 'value', 'pickerConfig');
    });

    it('sets the picked path when the pathPickedHandler is invoked', () => {
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.resolve());

      HstComponentService.pathPickedHandler('selected-path');
      expect(HstComponentService.setPathParameter).toHaveBeenCalledWith('id', 'variant', 'name', 'selected-path', 'basePath');
    });

    it('return a promise that is resolved after the picked path is set', (done) => {
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.resolve());

      expect(pickPathPromise).toBeDefined();
      pickPathPromise.then(() => {
        done();
      });

      HstComponentService.pathPickedHandler('selected-path');
      $rootScope.$digest();
    });

    it('return a promise that is rejected if setting the picked path fails', (done) => {
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.reject());

      pickPathPromise.catch(() => {
        done();
      });

      HstComponentService.pathPickedHandler('selected-path');
      $rootScope.$digest();
    });
  });

  describe('setPathParameter', () => {
    it('calls setParameter after parsing input', () => {
      spyOn(HstComponentService, 'setParameter');

      HstComponentService.setPathParameter('a', 'b', 'c', '/path');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', '/path');
    });

    it('turns a relative path into an absolute path', () => {
      spyOn(HstComponentService, 'setParameter');

      HstComponentService.setPathParameter('a', 'b', 'c', 'path');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', '/path');
    });

    it('passes a relative path if basePath is set and path is a part of basePath', () => {
      spyOn(HstComponentService, 'setParameter');

      HstComponentService.setPathParameter('a', 'b', 'c', '/path', '/root');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', '/path');

      HstComponentService.setPathParameter('a', 'b', 'c', '/root', '/root');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', '/root');

      HstComponentService.setPathParameter('a', 'b', 'c', '/root/path', '/root');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', 'path');

      HstComponentService.setPathParameter('a', 'b', 'c', '/root/path', '/root/');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', 'path');
    });
  });

  describe('setParameter', () => {
    beforeEach(() => {
      spyOn(HstService, 'doPutForm');
      spyOn(ChannelService, 'recordOwnChange');
    });

    it('uses the HstService to store the parameter data of a component', () => {
      HstService.doPutForm.and.returnValue($q.resolve());

      HstComponentService.setParameter('id', 'variant', 'name', 'value');
      $rootScope.$digest();

      expect(HstService.doPutForm).toHaveBeenCalledWith({ name: 'value' }, 'id', 'variant');
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('does not record own change if parameter change fails', () => {
      HstService.doPutForm.and.returnValue($q.reject());

      HstComponentService.setParameter('id', 'variant', 'name', 'value');
      $rootScope.$digest();

      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });

    it('URI-encodes the variant name', () => {
      HstService.doPutForm.and.returnValue($q.resolve());

      HstComponentService.setParameter('id', '@variant', 'name', 'value');
      expect(HstService.doPutForm).toHaveBeenCalledWith({ name: 'value' }, 'id', '%40variant');
    });
  });
});
