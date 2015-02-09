/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

(function () {
    "use strict";

    angular.module('hippo.channel.menu')

        .controller('hippo.channel.menu.TreeCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            '$rootScope',
            '$log',
            'hippo.channel.ConfigService',
            'hippo.channel.FeedbackService',
            'hippo.channel.FormStateService',
            'hippo.channel.menu.MenuService',
            function ($scope, $state, $stateParams, $rootScope, $log, ConfigService, FeedbackService, FormStateService, MenuService) {

                function onSuccess() {
                }

                function setErrorFeedback(errorResponse) {
                    $scope.$parent.feedback = FeedbackService.getFeedback(errorResponse);
                }

                function editItem(itemId) {
                    $scope.$parent.feedback = '';

                    $state.go('menu-item.edit', {
                        menuItemId: itemId
                    });
                }

                function selectItem(itemId) {
                    if (FormStateService.isDirty()) {
                        if (FormStateService.isValid()) {
                            // handle the split between sitemap and external links. Grab whichever
                            // one is used and save that as link and then destroy them both
                            var savedMenuItem = angular.copy($scope.$parent.selectedMenuItem);

                            if (savedMenuItem.linkType === 'SITEMAPITEM') {
                                savedMenuItem.link = savedMenuItem.sitemapLink;
                            } else if (savedMenuItem.linkType === 'EXTERNAL') {
                                savedMenuItem.link = savedMenuItem.externalLink;
                            } else if (savedMenuItem.linkType === 'NONE') {
                                delete savedMenuItem.link;
                            }
                            delete savedMenuItem.sitemapLink;
                            delete savedMenuItem.externalLink;

                            MenuService.saveMenuItem(savedMenuItem).then(function() {
                                    editItem(itemId);
                                },
                                function (error) {
                                    setErrorFeedback(error);
                                    FormStateService.setValid(false);
                                }
                            );
                        }
                    } else {
                        editItem(itemId);
                    }
                }

                $scope.callbacks = {
                    accept: function(sourceNodeScope, destNodesScope, destIndex) {
                        // created an issue for the Tree component, to add a disabled state
                        // link: https://github.com/JimLiu/angular-ui-tree/issues/63
                        // for now, simply don't accept any moves when the form is invalid
                        return FormStateService.isValid();
                    },
                    dragStart: function(event) {
                        var draggedItemId = event.source.nodeScope.$modelValue.id;
                        selectItem(draggedItemId);
                    },
                    dropped: function(event) {
                        var source = event.source,
                            sourceNodeScope = source.nodeScope,
                            sourceId = sourceNodeScope.$modelValue.id,
                            dest = event.dest,
                            destNodesScope = dest.nodesScope,
                            destId = destNodesScope.$nodeScope ? destNodesScope.$nodeScope.$modelValue.id : ConfigService.menuId;

                        if (source.nodesScope !== destNodesScope || source.index !== dest.index) {
                            MenuService.moveMenuItem(sourceId, destId, dest.index);
                        }

                        selectItem(sourceId);
                    }
                };
            }
        ]);
}());
