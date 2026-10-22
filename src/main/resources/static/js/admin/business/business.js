import initializeSwal from "../../fragments/swalService.js";
import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let businessService;

$(document).ready(function() {
    businessService = new BusinessService();
    businessService.businessTable();
    initBusinessOverviewButtons();
});

function BusinessService() {
    this.businessTable = function () {
        initializeDataTable('#businessTable', token, {
            'columnDefs': [
                {
                    'targets': 3,
                    'orderable': false
                }
            ]
        });
    }

    this.editAgency = function (id) {
        globalThis.location.href = "/business/edit/" + id;
    }

    this.deleteAgency = function (id) {
        const params = {
            title: 'Slet virksomhed?',
            text: 'Er du sikker? Det vil slette alle tilhørende medarbejder.',
            id: id,
            token: token,
            success: 'Virksomheden er slettet',
            warning: 'Kunne ikke slette virksomheden',
            url: '/rest/business/delete/',
            route: '/business/overview'
        };
        initializeSwal(params);
    }

    this.cancelButton = function () {
        globalThis.location.href = globalThis.location.origin + "/business/overview";
    }

    this.openDetails = function (id) {
        globalThis.location.href = "/business/details/" + id;
    }
}

function initBusinessOverviewButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        if (target.classList.contains('openDetailsBtn') || target.closest('.openDetailsBtn')) {
            const btn = target.closest('.openDetailsBtn');
            const id = btn.dataset.id;
            businessService.openDetails(id);
        } else if (target.classList.contains('editAgencyBtn') || target.closest('.editAgencyBtn')) {
            const btn = target.closest('.editAgencyBtn');
            const id = btn.dataset.id;
            businessService.editAgency(id);
        } else if (target.classList.contains('deleteAgencyBtn') || target.closest('.deleteAgencyBtn')) {
            const btn = target.closest('.deleteAgencyBtn');
            const id = btn.dataset.id;
            businessService.deleteAgency(id);
        }
    })
}
