/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
    angular.module('hippo.essentials')
        .directive("essentialsNotifier", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    messages: '='
                },
                templateUrl: 'directives/essentials-notifier.html',
                controller: function ($scope, $filter, $log, $rootScope, $http, $timeout, $document) {
                    var promisesQueue = [];
                    var lastLength = 0;
                    var ERROR_SHOW_TIME = 1000;
                    $scope.visible = true;
                    $scope.messages = [];

                    $scope.activeMessages = [];
                    $scope.archiveMessages = [$scope.messages[0]];
                    $scope.archiveOpen = false;

                    $scope.$watch('messages', function () {
                        // don't execute if message count is not changed, e.g. when changing visibility only
                        if (lastLength == $scope.messages.length) {
                            return;
                        }
                        var date = new Date();
                        var now = date.toLocaleTimeString();
                        // cancel all hide promises
                        angular.forEach(promisesQueue, function (promise) {
                            $timeout.cancel(promise);
                        });
                        promisesQueue = [];
                        // keep messages which are not older than time showed + ERROR_SHOW_TIME:
                        /*  var elapsedTime = new Date();
                         elapsedTime.setSeconds(elapsedTime.getSeconds() + ERROR_SHOW_TIME);
                         var keepValuesCounter =0;
                         angular.forEach($scope.activeMessages, function (value) {
                         if(value.fullDate && value.fullDate.getDate() < elapsedTime){
                         keepValuesCounter++;
                         }
                         });*/
                        var currentLength = $scope.messages.length;
                        var startIdx = lastLength;
                        lastLength = currentLength;
                        $scope.activeMessages = [];
                        $scope.activeMessages = $scope.messages.slice(startIdx, currentLength);
                        $scope.archiveMessages = $scope.messages.slice(0, startIdx);
                        angular.forEach($scope.messages, function (value) {
                            value.visible = true;
                            if (!value.date) {
                                value.date = now;
                                value.fullDate = date;
                            }
                        });
                        if ($scope.archiveMessages.length === 0) {
                            $scope.archiveMessages.push({type: "info", message: 'No archived messages', visible: true, date: now, fullDate: date});
                        }
                        // newer messages first:
                        $scope.archiveMessages.reverse();
                        if ($scope.activeMessages.length > 1) {
                            // animate close:
                            var counter = 1;
                            var copy = $scope.activeMessages.slice(0);
                            angular.forEach(copy, function (value) {
                                if (counter > 1) {
                                    var promise = $timeout(function () {
                                        value.visible = false;
                                        $scope.archiveMessages.unshift(value);
                                    }, ERROR_SHOW_TIME * counter);
                                    promisesQueue.push(promise);
                                }
                                counter = counter + 0.5;
                            });
                        }
                    }, true);

                    $scope.toggleArchive = function ($event) {
                        $scope.archiveOpen = !$scope.archiveOpen;
                        $event.stopPropagation();
                    };
                    $scope.hide = function() {
                        $scope.visible = false;
                        $rootScope.$broadcast('hide-messages');
                    };
                    $rootScope.$on('show-messages', function() {
                        $scope.visible = true;
                    });
                    $scope.toggleErrors = function ($event) {
                        $event.stopPropagation();
                        $scope.showErrors = !$scope.showErrors;
                    };

                    $document.bind('click', function () {
                        $scope.archiveOpen = false;
                        $scope.$apply();
                    });
                }
            };
        }).directive("essentialsPlugin", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-plugin.html',
                controller: function ($scope, $filter, $log, $rootScope, $http, pluginService) {
                    $scope.slides = [];
                    angular.forEach($scope.plugin.imageUrls, function(url) {
                        $scope.slides.push({
                            image: url
                        });
                    });
                    $scope.interval = 5000; // carousel change interval
                    $scope.showDescription = false;
                    $scope.toggleDescription = function ($event) {
                        $event.preventDefault();
                        $scope.showDescription = !$scope.showDescription;
                    };
                    $scope.isDiscovered = function() {
                        return $scope.plugin.type === 'feature' && $scope.plugin.installState === 'discovered';
                    };
                    $scope.isBoarding = function() {
                        return $scope.plugin.installState === 'boarding' || $scope.plugin.installState === 'installing';
                    };
                    $scope.isPending = function() {
                        return $scope.plugin.installState === 'boardingPending' || $scope.plugin.installState === 'installationPending';
                    };
                    $scope.isOnBoard = function() {
                        return $scope.plugin.type === 'tool' ||
                               $scope.plugin.installState === 'onBoard' ||
                               $scope.plugin.installState === 'installed';
                    };
                    $scope.installPlugin = function () {
                        $scope.installButtonDisabled = true; // avoid double-clicking

                        // Due to inter-plugin dependencies, successful installation reloads the list of plugins
                        // to have all states updated. This causes angular to re-instantiate this controller, so
                        // we don't need to do anything with the promise returned by #install().
                        pluginService.install($scope.plugin.id);
                    };
                }
            };
        }).directive("essentialsInstalledFeature", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-installed-feature.html',
                controller: function ($scope) {
                    $scope.needsRebuild = function() {
                        var state = $scope.plugin.installState;
                        return state === 'boarding' || state === 'installing';
                    };
                    $scope.needsConfiguration = function() {
                        return $scope.plugin.installState === 'onBoard';
                    };
                    $scope.hasConfiguration = function() {
                        return $scope.plugin.installState === 'installed' && $scope.plugin.hasConfiguration;
                    };
                    $scope.isPending = function() {
                        return $scope.plugin.installState === 'boardingPending' || $scope.plugin.installState === 'installationPending';
                    };
                }
            };
        }).directive("essentialsInstalledTool", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-installed-tool.html',
                controller: function ($scope, $location) {
                    $scope.useTool = function () {
                        $location.path('/tools/' + $scope.plugin.id);
                    };
                }
            };
        }).directive("essentialsFeatureFooter", function () {
            return {
                replace: true,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-feature-footer.html',
                controller: function ($scope, $rootScope, pluginService) {
                    $scope.toggleChanges = function($event) {
                        $event.preventDefault();
                        $scope.showChanges = !$scope.showChanges;

                        // Lazy-loading messages
                        if (!$scope.messagesLoaded) {
                            pluginService.getChangeMessages($scope.plugin.id).then(function(changeMessages) {
                                $scope.changeMessages = changeMessages;
                                $scope.messagesLoaded = true;
                            });
                        }
                    };

                    $scope.messagesLoaded = false; // Flag for lazy loading
                    $scope.showChanges = false;
                    $scope.hasMessages = !!$scope.plugin.packageFile;
                }
            };
        });
})();
