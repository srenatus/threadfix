var myAppModule = angular.module('threadfix')

myAppModule.controller('ReportsController', function ($scope, $window, threadfixAPIService, filterService, trendingUtilities) {

    // Using this controller is easy; just set up a parent controller with empty and reportQuery fields.
    $scope.empty = $scope.$parent.empty;
    $scope.noData = $scope.$parent.empty;
    $scope.leftMargin = [20, 70, 40, 60];
    $scope.rightMargin = {top: 20, right: 20, bottom: 30, left: 60};

    if (!$scope.empty) {
        $scope.loadingLeft = true;
        $scope.loadingRight = true;
    }

    $scope.$on('seeMoreExtension', function(event, extension) {
        $scope.seeMoreExtension = extension;
    });

    var loadReports = function() {
        threadfixAPIService.loadReport("/dashboard/leftReport", $scope.reportQuery).
            success(function(data, status, headers, config) {

                $scope.allScans = data.object.scanList;
                $scope.savedFilters = data.object.savedFilters;

                if ($scope.allScans) {
                    filterService.findDefaultFilter($scope);
                    trendingUtilities.resetFilters($scope);

                    $scope.allScans.sort(function (a, b) {
                        return a.importTime - b.importTime;
                    });

                    $scope.filterScans = $scope.allScans;
                    trendingUtilities.refreshScans($scope);
                } else {
                    $scope.noData = true;
                }

                $scope.loadingLeft = false;
            }).
            error(function(data, status, headers, config) {

                // TODO improve error handling and pass something back to the users
                $scope.leftReportFailed = true;
                $scope.loadingLeft = false;
            });

        threadfixAPIService.loadReport("/dashboard/rightReport", $scope.reportQuery).
            success(function(data, status, headers, config) {

                $scope.topAppsData = data.object;

                if (!$scope.topAppsData) {
                    $scope.empty = true;
                }

                $scope.loadingRight = false;

            }).
            error(function(data, status, headers, config) {

                // TODO improve error handling and pass something back to the users
                $scope.rightReportFailed = true;
                $scope.loadingRight = false;
            });
    };

    $scope.$on('rootScopeInitialized', function() {
        $scope.reportQuery = $scope.$parent.reportQuery;
        $scope.label = {
            teamId: $scope.$parent.teamId,
            appId: $scope.$parent.appId

        };
        $scope.rightReportTitle = $scope.$parent.rightReportTitle;
        if (!$scope.empty) {
            loadReports();
        }
    });

    var reload = function() {
        $scope.loadingLeft = true;
        $scope.leftReport = null;
        $scope.loadingRight = true;
        $scope.rightReport = null;
        $scope.rightReportFailed = false;
        $scope.leftReportFailed = false;
        $scope.empty = false;
        loadReports();
    };

    $scope.$on('scanUploaded', reload);
    $scope.$on('scanDeleted', function(event, shouldReload) {
        if (shouldReload) {
            reload();
        } else {
            $scope.leftReport = null;
            $scope.rightReport = null;

            $scope.topAppsData = null;
            $scope.trendingScansData = null;
            $scope.noData = true;
            $scope.allScans = null;

            $scope.empty = true;
        }
    });

});