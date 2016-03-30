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

export class HippoIframeCtrl {
  constructor(
    $q,
    $element,
    $mdDialog,
    $rootScope,
    $scope,
    $translate,
    ChannelService,
    CmsService,
    DragDropService,
    hstCommentsProcessorService,
    linkProcessorService,
    OverlaySyncService,
    PageStructureService,
    PageMetaDataService,
    ScalingService,
    HippoIframeService
  ) {
    'ngInject';

    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.$mdDialog = $mdDialog;
    this.linkProcessorService = linkProcessorService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.CmsService = CmsService;
    this.ChannelService = ChannelService;
    this.PageStructureService = PageStructureService;
    this.PageMetaDataService = PageMetaDataService;
    this.OverlaySyncService = OverlaySyncService;
    this.DragDropService = DragDropService;
    this.HippoIframeService = HippoIframeService;

    this.iframeJQueryElement = $element.find('iframe');
    this.iframeJQueryElement.on('load', () => this.onLoad());

    HippoIframeService.initialize(this.iframeJQueryElement);

    OverlaySyncService.init(this.iframeJQueryElement, $element.find('.overlay'));
    ScalingService.init($element);
    DragDropService.init(this.iframeJQueryElement, $element.find('.channel-iframe-base'));

    CmsService.subscribe('delete-component', (componentId) => {
      this.deleteComponent(componentId);
    });

    $scope.$watch('iframe.editMode', () => this._updateDragDrop());
  }

  onLoad() {
    this.$rootScope.$apply(() => {
      this._parseHstComments();
      this._updateDragDrop();
      this._updateChannelIfSwitched().then(() => {
        this._parseLinks();
        this.HippoIframeService.signalPageLoadCompleted();
      });
      // TODO: handle error.
      // show dialog explaining that for this channel, the CM can currently not be used,
      // and return to the channel overview upon confirming?
    });
  }

  deleteComponent(componentId) {
    this._confirmDelete()
      .then(() => this.PageStructureService.removeComponentById(componentId)
        .then(({ oldContainer, newContainer }) => this.DragDropService.replaceContainer(oldContainer, newContainer))
      )
      .catch(() => this.PageStructureService.showComponentProperties(this.selectedComponent));
  }

  _confirmDelete() {
    const confirm = this.$mdDialog
      .confirm()
      .title(this.$translate.instant('CONFIRM_DELETE_COMPONENT_TITLE'))
      .textContent(this.$translate.instant('CONFIRM_DELETE_COMPONENT_MESSAGE', {
        component: this.selectedComponent.getLabel(),
      }))
      .ok(this.$translate.instant('BUTTON_YES'))
      .cancel(this.$translate.instant('BUTTON_NO'));

    return this.$mdDialog.show(confirm);
  }

  _updateDragDrop() {
    if (this.editMode) {
      this.DragDropService.enable(this.PageStructureService.containers);
    } else {
      this.DragDropService.disable();
    }
  }

  startDragOrClick($event, component) {
    this.selectedComponent = component;
    this.DragDropService.startDragOrClick($event, component);
  }

  isDraggingOrClicking() {
    return this.DragDropService.isDraggingOrClicking();
  }

  isDragging() {
    return this.DragDropService.isDragging();
  }

  _parseHstComments() {
    const iframeDom = this._getIframeDOM(this);

    this.PageStructureService.clearParsedElements();
    this.hstCommentsProcessorService.run(
      iframeDom,
      this.PageStructureService.registerParsedElement.bind(this.PageStructureService)
    );
    this.PageStructureService.printParsedElements();
  }

  _updateChannelIfSwitched() {
    const channelId = this.PageMetaDataService.getChannelId();
    if (channelId !== this.ChannelService.getId()) {
      return this.ChannelService.switchToChannel(channelId);
    }

    return this.$q.resolve();
  }

  _getIframeDOM() {
    return this.iframeJQueryElement.contents()[0];
  }

  _parseLinks() {
    const iframeDom = this._getIframeDOM();
    const internalLinkHostURL = `${iframeDom.location.protocol}//${iframeDom.location.host}`;
    const internalLinkPrefixes = this.ChannelService.makeInternalLinkPrefixList(internalLinkHostURL);

    this.linkProcessorService.run(iframeDom, internalLinkPrefixes);
  }

  getContainers() {
    return this.editMode ? this.PageStructureService.containers : [];
  }
}
