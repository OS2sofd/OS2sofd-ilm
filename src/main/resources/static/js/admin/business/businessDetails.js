import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let businessDetailService;

$(document).ready(function() {
    businessDetailService = new BusinessDetailService();
    businessDetailService.affiliationTable();
    businessDetailService.initLeaderTable();
});

function BusinessDetailService() {
    this.affiliationTable = function () {
        initializeDataTable('#affiliatedTable', token, {}, true);
    }

    this.initLeaderTable = function () {
        initializeDataTable('#leaderTable', token, {
            'columnDefs': [
                {
                    'targets': 3,
                    'orderable': false
                }
            ]
        });
    }
}
