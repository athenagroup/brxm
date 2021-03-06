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

const LS_KEY_PANEL_WIDTH = 'channelManager.sidePanel.right.width';
const MIN_WIDTH = 400;

class RightSidePanelCtrl {
  constructor(
    $element,
    $mdConstant,
    $scope,
    $transitions,
    $window,
    SidePanelService,
    localStorageService,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$scope = $scope;
    this.$transitions = $transitions;
    this.$window = $window;

    this.SidePanelService = SidePanelService;
    this.localStorageService = localStorageService;
    this.RightSidePanelService = RightSidePanelService;

    // Prevent the default closing action bound to the escape key by Angular Material.
    // We should show the "unsaved changes" dialog first.
    $element.on('keydown', (e) => {
      if (e.which === $mdConstant.KEY_CODE.ESCAPE) {
        e.stopImmediatePropagation();
        this.close();
      }
    });
  }

  $onInit() {
    this.sideNavElement = this.$element.find('.right-side-panel');
    this.width = Math.max(this._getStoredWidth(), MIN_WIDTH);

    this.$transitions.onBefore({ from: 'hippo-cm.channel', to: 'hippo-cm.channel.*.**' }, () => this._openPanel());
    this.$transitions.onSuccess({ from: 'hippo-cm.channel.**', to: 'hippo-cm.channel' }, () => this._closePanel());
    this.$transitions.onSuccess({ from: 'hippo-cm.channel.*', to: 'hippo-cm.channel.*.**' }, () => this._focusPanel());
    this.$transitions.onError({ from: 'hippo-cm.channel.**' }, () => this._focusPanel());
  }

  _getStoredWidth() {
    return parseInt(this.localStorageService.get(LS_KEY_PANEL_WIDTH), 10) || -1;
  }

  $postLink() {
    this.SidePanelService.initialize('right', this.$element, this.sideNavElement);
  }

  close() {
    this.RightSidePanelService.close();
  }

  onResize(newWidth) {
    if (newWidth < MIN_WIDTH && this.width === MIN_WIDTH) {
      return;
    }

    this.width = Math.max(newWidth, MIN_WIDTH);
    this.localStorageService.set(LS_KEY_PANEL_WIDTH, this.width);
    this.$scope.$digest();
  }

  isLoading() {
    return this.RightSidePanelService.isLoading();
  }

  getTitle() {
    return this.RightSidePanelService.getTitle();
  }

  getTooltip() {
    return this.RightSidePanelService.getTooltip();
  }

  getContext() {
    return this.RightSidePanelService.getContext();
  }

  isLockedOpen() {
    return this.SidePanelService.isOpen('right');
  }

  _openPanel() {
    this.SidePanelService.open('right');
  }

  _closePanel() {
    this.setFullScreen(false);
    this.SidePanelService.close('right');
  }

  _focusPanel() {
    this.SidePanelService.focus('right');
  }

  isFullScreen() {
    return this.SidePanelService.isFullScreen('right');
  }

  setFullScreen(fullScreen) {
    this.SidePanelService.setFullScreen('right', fullScreen);
    // to trigger hiding/showing pagination handles of md-tabs
    this.$window.dispatchEvent(new Event('resize'));
  }
}

export default RightSidePanelCtrl;
