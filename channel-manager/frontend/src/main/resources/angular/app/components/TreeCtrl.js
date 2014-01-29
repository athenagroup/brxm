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

    angular.module('hippo.channelManager.menuManagement')

        .controller('hippo.channelManager.menuManagement.TreeCtrl', ['$scope',
        '$location',
        '$routeParams',
        'hippo.channelManager.menuManagement.ConfigService',
        'hippo.channelManager.menuManagement.MenuService',
        function ($scope, $location, $routeParams, ConfigService, MenuService) {
            // fetch initial data
            $scope.menuTree = [{}];
            MenuService.getMenu(ConfigService.menuId).then(function (response) {
                $scope.menuTree = reformatData(response.children);
            });

            $scope.addItem = function() {
                 $scope.menuTree.push({
                    name: 'hooi'
                 });
            };

            // methods
            $scope.setSelectedItem = function (branch) {
                // set selected menu item so child-controllers can access it
                $scope.$parent.selectedMenuItem = branch;

                if (branch && branch.id) {
                    // redirect if the selected item is different from the current
                    if ($scope.$parent.selectedMenuItemId !== branch.id) {
                        $location.path('/' + branch.id + '/edit');
                    }
                }
            };

            function reformatData(src) {
                var result = [];
                _.each(src, function (item) {
                    var newItem = {
                        id: item.id,
                        text: item.name
                    };

                    if (item.children && item.children.length > 0) {
                        newItem.children = reformatData(item.children);
                    }

                    result.push(newItem);
                });

                return result;
            }
        }]);
})();