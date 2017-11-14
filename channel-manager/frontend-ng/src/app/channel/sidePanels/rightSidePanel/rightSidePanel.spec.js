/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

describe('RightSidePanel', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let SidePanelService;
  let CmsService;
  let ChannelService;

  let $ctrl;
  let $scope;
  let sidePanelHandlers;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_, _ChannelService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
    });

    SidePanelService = jasmine.createSpyObj('SidePanelService', ['initialize', 'isOpen', 'close']);

    CmsService = jasmine.createSpyObj('CmsService', ['reportUsageStatistic']);

    $scope = $rootScope.$new();
    const $element = angular.element('<div></div>');
    $ctrl = $componentController('rightSidePanel', {
      $scope,
      $element,
      CmsService,
      SidePanelService,
    });
    $rootScope.$apply();

    sidePanelHandlers = {
      onOpen: SidePanelService.initialize.calls.mostRecent().args[2],
      onClose: SidePanelService.initialize.calls.mostRecent().args[3],
    };
  });

  it('sets full width mode on and off', () => {
    $ctrl.setFullWidth(true);
    expect($ctrl.$element.hasClass('fullwidth')).toBe(true);
    expect($ctrl.isFullWidth).toBe(true);
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsFullScreen');

    CmsService.reportUsageStatistic.calls.reset();

    $ctrl.setFullWidth(false);
    expect($ctrl.$element.hasClass('fullwidth')).toBe(false);
    expect($ctrl.isFullWidth).toBe(false);
    expect(CmsService.reportUsageStatistic).not.toHaveBeenCalled();
  });

  it('updates local storage on resize', () => {
    $ctrl.onResize(800);

    expect($ctrl.lastSavedWidth).toBe('800px');
    expect($ctrl.localStorageService.get('rightSidePanelWidth')).toBe('800px');
  });

  it('loads last saved width of right side panel', () => {
    spyOn($ctrl.localStorageService, 'get').and.callFake(() => '800px');

    $ctrl.$onInit();

    expect($ctrl.localStorageService.get).toHaveBeenCalledWith('rightSidePanelWidth');
    expect($ctrl.lastSavedWidth).toBe('800px');

    $ctrl.localStorageService.get.and.callFake(() => null);

    $ctrl.$onInit();

    expect($ctrl.localStorageService.get).toHaveBeenCalledWith('rightSidePanelWidth');
    expect($ctrl.lastSavedWidth).toBe('440px');
  });

  describe('when initializing or opening the panel', () => {
    let testId;
    let testOptions;

    beforeEach(() => {
      spyOn($ctrl, '_onOpen').and.callThrough();
      testId = 'documentId';
      testOptions = { templateQuery: 'test-query' };
      $ctrl._resetBeforeStateChange();
    });

    it('initializes the channel right side panel service', () => {
      expect(SidePanelService.initialize).toHaveBeenCalled();
      expect($ctrl.options).not.toBeDefined();
      expect($ctrl.editing).not.toBeDefined();
      expect($ctrl.creating).not.toBeDefined();
    });

    it('resets state if beforeStateChange resolves', () => {
      spyOn($ctrl, '_resetState');
      sidePanelHandlers.onOpen('edit');
      $rootScope.$digest();

      sidePanelHandlers.onOpen('create');
      $rootScope.$digest();

      expect($ctrl._resetState).toHaveBeenCalledTimes(2);
    });

    it('initializes "edit" component when calling onOpen with id "edit"', () => {
      spyOn($ctrl, 'openInMode').and.callThrough();
      spyOn($ctrl, '_setMode').and.callThrough();
      sidePanelHandlers.onOpen('edit', testId);
      $rootScope.$digest();

      expect($ctrl._onOpen).toHaveBeenCalledWith('edit', testId);
      expect($ctrl.openInMode).toHaveBeenCalledWith($ctrl.modes.edit, testId);
      expect($ctrl._setMode).toHaveBeenCalledWith($ctrl.modes.edit, testId);
      expect($ctrl.mode).toBe($ctrl.modes.edit);
      expect($ctrl.options).toBe(testId);
    });

    it('initializes "create" component when calling onOpen with id "create"', () => {
      spyOn($ctrl, 'openInMode').and.callThrough();
      spyOn($ctrl, '_setMode').and.callThrough();
      sidePanelHandlers.onOpen('create', testOptions);
      $rootScope.$digest();

      expect($ctrl._onOpen).toHaveBeenCalledWith('create', testOptions);
      expect($ctrl.openInMode).toHaveBeenCalledWith($ctrl.modes.create, testOptions);
      expect($ctrl._setMode).toHaveBeenCalledWith($ctrl.modes.create, testOptions);
      expect($ctrl.mode).toBe($ctrl.modes.create);
      expect($ctrl.options).toBe(testOptions);
    });

    it('does not call beforeStateChange and lets editor handle pending changes', () => {
      $ctrl.options = 'test';
      $ctrl.mode = $ctrl.modes.edit;
      const spy = spyOn($ctrl, 'beforeStateChange');

      $ctrl._onOpen('edit', testId);
      expect(spy).not.toHaveBeenCalled();
    });

    it('opens a new document if beforeStateChange is resolved', () => {
      $ctrl.options = 'test';
      $ctrl.mode = $ctrl.modes.edit;
      spyOn($ctrl, 'beforeStateChange');
      $ctrl.beforeStateChange.and.returnValue($q.resolve());
      $ctrl._onOpen('edit', testId);
      $rootScope.$digest();

      expect($ctrl.mode).toBe($ctrl.modes.edit);
      expect($ctrl.options).toEqual(testId);
    });

    it('does not call beforeStateChange when changing between one createContent to another', () => {
      $ctrl.mode = $ctrl.modes.create;
      $ctrl._onOpen('edit');
      spyOn($ctrl, 'beforeStateChange');
      expect($ctrl.beforeStateChange).not.toHaveBeenCalled();
    });

    it('adds required view classes', () => {
      $ctrl.lastSavedWidth = '5px';
      $ctrl._onOpen('edit');
      $rootScope.$digest();
      expect($ctrl.$element.hasClass('sidepanel-open')).toEqual(true);
      expect($ctrl.$element.css('width')).toEqual('5px');
      expect($ctrl.$element.css('max-width')).toEqual('5px');
    });
  });

  it('sets isLockedOpen value in synchronization with SidePanelService.isOpen', () => {
    SidePanelService.isOpen.and.returnValue(true);
    expect($ctrl.isLockedOpen()).toBe(true);

    SidePanelService.isOpen.and.returnValue(false);
    expect($ctrl.isLockedOpen()).toBe(false);
  });

  it('closes the panel', () => {
    spyOn($ctrl, '_resetBeforeStateChange');
    spyOn($ctrl, 'setFullWidth');
    SidePanelService.close.and.returnValue($q.resolve());
    $ctrl.closePanel();
    $rootScope.$digest();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect($ctrl.setFullWidth).toHaveBeenCalledWith(false);

    $ctrl.mode = $ctrl.modes.edit;
    $ctrl.options = 'test';
    $ctrl.closePanel();
    $rootScope.$digest();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect($ctrl._resetBeforeStateChange).toHaveBeenCalled();
    expect($ctrl.mode).toBeUndefined();
    expect($ctrl.options).toBeUndefined();

  });

  it('resets right side panel state and sharedspace toolbar state', () => {
    spyOn($ctrl, '_resetState');
    spyOn(ChannelService, 'setToolbarDisplayed');
    ChannelService.isToolbarDisplayed = true;
    SidePanelService.close.and.returnValue($q.resolve(true));
    $ctrl.closePanel();
    $ctrl.$scope.$apply();
    expect(ChannelService.setToolbarDisplayed).toHaveBeenCalledWith(false);
    expect($ctrl._resetState).toHaveBeenCalled();
  });

  it('closes right side panel', () => {
    spyOn(ChannelService, 'setToolbarDisplayed');
    spyOn($ctrl, 'setFullWidth');
    SidePanelService.close.and.returnValue($q.resolve());

    ChannelService.isToolbarDisplayed = true;
    $ctrl.closePanel();
    $scope.$apply();

    expect(ChannelService.setToolbarDisplayed).toHaveBeenCalledWith(false);
    expect($ctrl.setFullWidth).toHaveBeenCalledWith(false);
  });

  describe('onBeforeStateChange', () => {
    it('sets and unsets a onBeforeStateChange callback', () => {
      const firstCallback = jasmine.createSpy('firstCallback', () => $q.resolve()).and.callThrough();
      $ctrl.onBeforeStateChange(firstCallback);
      $ctrl._onOpen('edit', null);
      expect(firstCallback).toHaveBeenCalled();
      SidePanelService.close.and.returnValue($q.resolve());
      $ctrl.closePanel();
      $rootScope.$digest();
      firstCallback.calls.reset();
      $ctrl._onOpen('edit', 'anotherTestId');
      expect(firstCallback).not.toHaveBeenCalled();
    });
  });
});
