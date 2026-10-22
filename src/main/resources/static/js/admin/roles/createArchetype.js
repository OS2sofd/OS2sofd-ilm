import verifyInputs from "../../fragments/verifyInputService.js";
import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let createArchetypeService;

$(document).ready(function() {
    createArchetypeService = new CreateArchetypeService();
    createArchetypeService.initGroupTable();
    createArchetypeService.initRoleTable();
    createArchetypeService.initRoleGroupTable();

    initCreateArchetypeButtons();
});

function CreateArchetypeService() {
    let selectedRoleIds = [];
    let selectedGroupIds = [];
    let selectedRoleGroupIds = [];

    this.initRoleTable = function () {
        initializeDataTable('#roleTable', token, {
            'columnDefs': [
                {
                    'targets': 3,
                    'orderable': false
                }
            ]
        });
    }

    this.initGroupTable = function () {
        initializeDataTable('#groupTable', token, {
            'columnDefs': [
                {
                    'targets': 1,
                    'orderable': false
                }
            ]
        });
    }

    this.initRoleGroupTable = function () {
        initializeDataTable('#roleGroupTable', token, {
            'columnDefs': [
                {
                    'targets': 2,
                    'orderable': false
                }
            ]
        });
    }

    this.cancelButton = function () {
        globalThis.location.href = "/archetypes";
    }

    this.saveCreatedArchetype = async function () {
        let name = $('#createEmployeeName').val();

        const isValid = verifyInputs([
            {value: name, message: "Du skal angive et navn"},
            {value: [selectedRoleGroupIds.length, selectedGroupIds.length, selectedRoleIds.length], message: "Du skal enten tilføje en gruppe, rolle eller et rollebuket", type: "oneOfNotZero"},
        ]);

        if (!isValid) {
            return;
        }

        let data = {
            "name": name,
            "roles": selectedRoleIds,
            "groups": selectedGroupIds,
            "roleGroups": selectedRoleGroupIds,
        }

        try {
            const url = '/rest/archetype/create';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            toastr.success("Arketypen blev oprettet");
            location.href = '/archetypes';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.updateGroup = function (checkbox) {
        const groupId = checkbox.dataset.id;
        if (checkbox.checked) {
            if (!selectedGroupIds.includes(groupId)) {
                selectedGroupIds.push(groupId);
            }
        } else {
            selectedGroupIds = selectedGroupIds.filter(id => id !== groupId);
        }
    }

    this.updateRole = function (checkbox) {
        const roleId = checkbox.dataset.id;
        if (checkbox.checked) {
            if (!selectedRoleIds.includes(roleId)) {
                selectedRoleIds.push(roleId);
            }
        } else {
            selectedRoleIds = selectedRoleIds.filter(id => id !== roleId);
        }
    }

    this.updateRoleGroup = function (checkbox) {
        const roleGroupId = checkbox.dataset.id;
        if (checkbox.checked) {
            if (!selectedRoleGroupIds.includes(roleGroupId)) {
                selectedRoleGroupIds.push(roleGroupId);
            }
        } else {
            selectedRoleGroupIds = selectedRoleGroupIds.filter(id => id !== roleGroupId);
        }
    }
}

function initCreateArchetypeButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                createArchetypeService.cancelButton();
                break;
            case 'confirm':
                createArchetypeService.saveCreatedArchetype();
                break;
        }
    });

    $(document).on('change', (e) => {
        const target = e.target;

        if (target.classList.contains('updateRoleGroupBtn') || target.closest('.updateRoleGroupBtn')) {
            const checkbox = target.closest('.updateRoleGroupBtn');
            createArchetypeService.updateRoleGroup(checkbox);
        } else if (target.classList.contains('updateRoleBtn') || target.closest('.updateRoleBtn')) {
            const checkbox = target.closest('.updateRoleBtn');
            createArchetypeService.updateRole(checkbox)
        } else if (target.classList.contains('updateGroupBtn') || target.closest('.updateGroupBtn')) {
            const checkbox = target.closest('.updateGroupBtn');
            createArchetypeService.updateGroup(checkbox);
        }
    });
}
