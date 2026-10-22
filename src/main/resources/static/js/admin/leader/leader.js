import verifyInputs from "../../fragments/verifyInputService.js";
import initializeSwal from "../../fragments/swalService.js";
import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let leaderService;

$(document).ready(function() {
    leaderService = new LeaderService();
    leaderService.initLeaderTable();
    leaderService.initBusinessTable();
    initLeaderButtons();
});

function LeaderService() {
    this.initLeaderTable = function () {
        initializeDataTable('#leaderTable', token, {
            'columnDefs': [
                {
                    'targets': 4,
                    'orderable': false
                }
            ]
        });
    }

    this.initBusinessTable = function () {
        initializeDataTable('#businessTable', token);
    }

    this.editLeader = function (id) {
        globalThis.location.href = "/leader/edit/" + id;
    }

    this.deleteLeader = function (id) {
        const params = {
            title: 'Slet leder?',
            text: 'Er du sikker på, at du vil slette denne leder?',
            id: id,
            token: token,
            success: 'Lederen er slettet',
            warning: 'Kunne ikke slette lederen',
            url: '/rest/leader/delete/',
            route: '/leader/overview'
        };
        initializeSwal(params);
    }

    this.cancelButton = function () {
        globalThis.location.href = globalThis.location.origin + "/leader/overview";
    }

    this.getSelectedBusinessIds = function () {
        let businessIds = [];
        $('.businessCheckbox:checked').each(function () {
            businessIds.push($(this).data('id'));
        });
        return businessIds;
    }

    this.createLeader = async function () {
        const name = $('#leaderName').val();
        const mail = $('#leaderMail').val();
        const mitIdUuid = $('#leaderMitIdUuid').val();
        const internalUuid = $('#leaderInternalUuid').val();
        const cpr = $('#leaderCprNumber').val();
        const businessIds = this.getSelectedBusinessIds();

        const isValid = verifyInputs([
            {value: name, message: "Du skal angive et navn"},
            {value: [mitIdUuid, internalUuid, cpr], message: "Du skal angive en mitId uuid, intern uuid eller et cpr-nummer", type: "oneOf"},
            {value: businessIds, message: "Du skal vælge mindst én virksomhed", type: "custom", condition: () => businessIds.length > 0},
        ]);

        if (!isValid) {
            return;
        }

        let data = {
            "name": name,
            "mail": mail,
            "mitIdUuid": mitIdUuid,
            "cpr": cpr,
            "internalUuid": internalUuid,
            "businessIds": businessIds
        };

        try {
            const url = '/rest/leader/create';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorText = await response.text()
                throw new Error(errorText);
            }

            toastr.success("Leder blev oprettet");
            location.href = '/leader/overview';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.updateLeader = async function () {
        const id = $('#leaderId').val();
        const name = $('#leaderName').val();
        const mail = $('#leaderMail').val();
        const mitIdUuid = $('#leaderMitIdUuid').val();
        const internalUuid = $('#leaderInternalUuid').val();
        const cpr = $('#leaderCprNumber').val();
        const businessIds = this.getSelectedBusinessIds();

        const isValid = verifyInputs([
            {value: name, message: "Du skal angive et navn"},
            {value: [mitIdUuid, internalUuid, cpr], message: "Du skal angive en mitId uuid, intern uuid eller et cpr-nummer", type: "oneOf"},
            {value: businessIds, message: "Du skal vælge mindst én virksomhed", type: "custom", condition: () => businessIds.length > 0},
        ]);

        if (!isValid) {
            return;
        }

        let data = {
            "name": name,
            "mail": mail,
            "mitIdUuid": mitIdUuid,
            "cpr": cpr,
            "internalUuid": internalUuid,
            "businessIds": businessIds
        };

        try {
            const url = `/rest/leader/edit/${id}`;
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorText = await response.text()
                throw new Error(errorText);
            }

            toastr.success("Leder blev opdateret");
            location.href = '/leader/overview';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }
}

function initLeaderButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                leaderService.cancelButton();
                break;
            case 'confirmEdit':
                leaderService.updateLeader();
                break;
            case 'confirmCreate':
                leaderService.createLeader();
                break;
        }

        if (target.classList.contains('editLeaderBtn') || target.closest('.editLeaderBtn')) {
            const id = target.closest('.editLeaderBtn')?.dataset.id;
            leaderService.editLeader(id);
        }
        else if (target.classList.contains('deleteLeaderBtn') || target.closest('.deleteLeaderBtn')) {
            const id = target.closest('.deleteLeaderBtn')?.dataset.id;
            leaderService.deleteLeader(id);
        }
    });
}
