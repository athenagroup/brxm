/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('DocumentLocationField', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let ChannelService;
  let Step1Service;

  let component;
  let getFolderSpy;
  let getChannelSpy;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((
      _$componentController_,
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _Step1Service_,
    ) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      Step1Service = _Step1Service_;
    });

    component = $componentController('documentLocationField');

    getFolderSpy = spyOn(Step1Service, 'getFolders').and.returnValue($q.resolve());
    getChannelSpy = spyOn(ChannelService, 'getChannel').and.returnValue({ contentRoot: '/channel/content' });
    component.changeLocale = () => angular.noop();
  });


  it('detects the root path depth', () => {
    component.rootPath = '/root';
    component.$onInit();
    expect(component.rootPathDepth).toBe(1);

    component.rootPath = '/root/path/';
    component.$onInit();
    expect(component.rootPathDepth).toBe(2);

    component.rootPath = 'some/path/';
    component.$onInit();
    expect(component.rootPathDepth).toBe(4);
  });

  describe('parsing the defaultPath @Input', () => {
    it('throws an error if defaultPath is absolute', () => {
      component.defaultPath = '/path';
      expect(() => component.$onInit()).toThrow(new Error('The defaultPath option can only be a relative path: /path'));
    });
  });

  describe('setting the document location', () => {
    it('stores the path of the last folder returned by the create-content-service', () => {
      const folders = [{ path: '/root' }, { path: '/root/path' }];
      getFolderSpy.and.returnValue($q.resolve(folders));

      component.$onInit();
      $rootScope.$apply();

      expect(component.documentLocation).toBe('/root/path');
    });

    it('stores the value of defaultPath returned by the create-content-service', () => {
      component.rootPath = '/root';
      const folders = [{ name: 'root' }, { name: 'default' }, { name: 'path' }];
      getFolderSpy.and.returnValue($q.resolve(folders));

      component.$onInit();
      $rootScope.$apply();

      expect(component.defaultPath).toBe('default/path');
    });
  });

  describe('setting the document location label', () => {
    const setup = (rootPath, defaultPath, displayNames) => {
      const folders = [];
      displayNames.forEach((displayName) => {
        folders.push({ displayName, path: '' });
      });
      getFolderSpy.and.returnValue($q.resolve(folders));

      component.rootPath = rootPath;
      component.defaultPath = defaultPath;
      component.$onInit();
      $rootScope.$apply();
    };

    it('uses displayName(s) for the document location label', () => {
      setup('/root', '', ['R00T']);
      expect(component.documentLocationLabel).toBe('R00T');

      setup('/root', 'bloom', ['R00T', 'bl00m']);
      expect(component.documentLocationLabel).toBe('R00T/bl00m');
    });

    it('uses only one folder of root path if default path is empty', () => {
      setup('', '', ['channel', 'content']);
      expect(component.documentLocationLabel).toBe('content');

      setup('root', '', ['channel', 'content', 'root']);
      expect(component.documentLocationLabel).toBe('root');

      setup('/root', '', ['root']);
      expect(component.documentLocationLabel).toBe('root');

      setup('root/path', '', ['channel', 'content', 'root', 'path']);
      expect(component.documentLocationLabel).toBe('path');

      setup('/root/path', '', ['root', 'path']);
      expect(component.documentLocationLabel).toBe('path');
    });

    it('uses only one folder of root path if default path depth is less than 3', () => {
      setup('', 'some', ['channel', 'content', 'some']);
      expect(component.documentLocationLabel).toBe('content/some');

      setup('', 'some/folder', ['channel', 'content', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('content/some/folder');

      setup('root', 'some/folder', ['channel', 'content', 'root', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('root/some/folder');

      setup('/root', 'some/folder', ['root', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('root/some/folder');
    });

    it('always shows a maximum of 3 folders', () => {
      setup('root', 'folder/with/document', ['channel', 'content', 'root', 'folder', 'with', 'document']);
      expect(component.documentLocationLabel).toBe('folder/with/document');

      setup('/root', 'folder/with/document', ['root', 'folder', 'with', 'document']);
      expect(component.documentLocationLabel).toBe('folder/with/document');

      setup('/root', 'folder/with/some/document', ['root', 'folder', 'with', 'some', 'document']);
      expect(component.documentLocationLabel).toBe('with/some/document');

      setup('/root', 'folder/with/some/nested/document', ['root', 'folder', 'with', 'some', 'nested', 'document']);
      expect(component.documentLocationLabel).toBe('some/nested/document');

      setup('/root/path', 'folder/with/some/nested/document', ['root', 'path', 'folder', 'with', 'some', 'nested', 'document']);
      expect(component.documentLocationLabel).toBe('some/nested/document');
    });
  });

  describe('the locale @Output', () => {
    it('emits the locale when component is initialized', () => {
      spyOn(component, 'changeLocale');
      const folders = [{ path: '/root' }, { path: '/root/path', locale: 'de' }];
      getFolderSpy.and.returnValue($q.resolve(folders));

      component.$onInit();
      $rootScope.$apply();

      expect(component.changeLocale).toHaveBeenCalledWith({ locale: 'de' });
    });
  });
});
