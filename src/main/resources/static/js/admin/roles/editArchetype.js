import verifyInputs from "../../fragments/verifyInputService.js";
import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let editArchetypeService;

$(document).ready(function() {
    editArchetypeService = new EditArchetypeService();
    editArchetypeService.initRoleTable();
    editArchetypeService.initGroupTable();
    editArchetypeService.initRoleGroupTable();
    editArchetypeService.initializeOriginalRoleState();
    editArchetypeService.initializeOriginalGroupState();
    editArchetypeService.initializeOriginalRoleGroupState();

    initEditArchetypeButtons();
});

function EditArchetypeService () {
    let originalRoleIds = [];
    let originalGroupIds = [];
    let originalRoleGroupIds = [];

    let selectedRoleIds = [];
    let selectedGroupIds = [];
    let selectedRoleGroupIds = [];

    let deselectedGroupIds = [];
    let deselectedRoleIds = [];
    let deselectedRoleGroupIds = [];

    this.initializeOriginalRoleState = function() {
        const checkboxes = document.querySelectorAll('#roleTable input[type="checkbox"][data-id]');
        checkboxes.forEach(checkbox => {
            if (checkbox.checked) {
                const roleId = checkbox.dataset.id;
                if (!originalRoleIds.includes(roleId)) {
                    originalRoleIds.push(roleId);
                }
                if (!selectedRoleIds.includes(roleId)) {
                    selectedRoleIds.push(roleId);
                }
            }
        });
    }

    this.initializeOriginalGroupState = function() {
        const checkboxes = document.querySelectorAll('#groupTable input[type="checkbox"][data-id]');
        checkboxes.forEach(checkbox => {
            if (checkbox.checked) {
                const groupId = checkbox.dataset.id;
                if (!originalGroupIds.includes(groupId)) {
                    originalGroupIds.push(groupId);
                }
                if (!selectedGroupIds.includes(groupId)) {
                    selectedGroupIds.push(groupId);
                }
            }
        });
    }

    this.initializeOriginalRoleGroupState = function() {
        const checkboxes = document.querySelectorAll('#roleGroupTable input[type="checkbox"][data-id]');
        checkboxes.forEach(checkbox => {
            if (checkbox.checked) {
                const roleGroupId = checkbox.dataset.id;
                if (!originalRoleGroupIds.includes(roleGroupId)) {
                    originalRoleGroupIds.push(roleGroupId);
                }
                if (!selectedRoleGroupIds.includes(roleGroupId)) {
                    selectedRoleGroupIds.push(roleGroupId);
                }
            }
        });
    }

    this.initRoleTable = function () {
        initializeDataTable('#roleTable', token);
    }

    this.initGroupTable = function () {
        initializeDataTable('#groupTable', token);
    }

    this.initRoleGroupTable = function () {
        initializeDataTable('#roleGroupTable', token);
    }

    this.cancelButton = function () {
        globalThis.location.href = "/archetypes";
    }

    this.saveEditedArchetype = async function (id) {
        let name = $('#editArchetypeName').val();

        const isValid = verifyInputs([
            {value: name, message: "Du skal angive et navn"},
            {value: [selectedGroupIds.length, selectedRoleGroupIds.length, selectedRoleIds.length], message: "Du skal enten tilføje en gruppe, rolle eller et rollebuket", type: "oneOfNotZero"}
        ]);

        if (!isValid) {
            return;
        }

        let rolesToAdd = selectedRoleIds.filter(roleId => !originalRoleIds.includes(roleId));
        let groupsToAdd = selectedGroupIds.filter(groupId => !originalGroupIds.includes(groupId));
        let roleGroupsToAdd = selectedRoleGroupIds.filter(groupId => !originalRoleGroupIds.includes(groupId));

        let data = {
            "archetypeId": id,
            "name": name,
            "rolesToAdd": rolesToAdd,
            "groupsToAdd": groupsToAdd,
            "roleGroupsToAdd": roleGroupsToAdd,
            "rolesToRemove": deselectedRoleIds,
            "groupsToRemove": deselectedGroupIds,
            "roleGroupsToRemove": deselectedRoleGroupIds
        }

        try {
            const url = '/rest/archetype/edit';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'content-type': 'application/json',
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            toastr.success("Arketype blev opdateret")
            location.href = '/archetypes';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.updateRole = function (checkbox) {
        const roleId = checkbox.dataset.id;
        const wasOriginal = originalRoleIds.includes(roleId);
        const isChecked = checkbox.checked;

        if (isChecked) {
            // Checkbox is checked
            if (!selectedRoleIds.includes(roleId)) {
                selectedRoleIds.push(roleId);
            }

            // If this was originally selected and was deselected, remove from deselected array
            if (wasOriginal) {
                const deselectedIndex = deselectedRoleIds.indexOf(roleId);
                if (deselectedIndex > -1) {
                    deselectedRoleIds.splice(deselectedIndex, 1);
                }
            }
        } else {
            // Checkbox is unchecked
            selectedRoleIds = selectedRoleIds.filter(id => id !== roleId);

            // If this was originally selected, add to deselected array
            if (wasOriginal && !deselectedRoleIds.includes(roleId)) {
                deselectedRoleIds.push(roleId);
            }
        }
    }

    this.updateGroup = function (checkbox) {
        const groupId = checkbox.dataset.id;
        const wasOriginal = originalGroupIds.includes(groupId);
        const isChecked = checkbox.checked;

        if (isChecked) {
            // Checkbox is checked
            if (!selectedGroupIds.includes(groupId)) {
                selectedGroupIds.push(groupId);
            }

            // If this was originally selected and was deselected, remove from deselected array
            if (wasOriginal) {
                const deselectedIndex = deselectedGroupIds.indexOf(groupId);
                if (deselectedIndex > -1) {
                    deselectedGroupIds.splice(deselectedIndex, 1);
                }
            }
        } else {
            // Checkbox is unchecked
            selectedGroupIds = selectedGroupIds.filter(id => id !== groupId);

            // If this was originally selected, add to deselected array
            if (wasOriginal && !deselectedGroupIds.includes(groupId)) {
                deselectedGroupIds.push(groupId);
            }
        }
    }

    this.updateRoleGroup = function (checkbox) {
        const roleGroupId = checkbox.dataset.id;
        const wasOriginal = originalRoleGroupIds.includes(roleGroupId);
        const isChecked = checkbox.checked;

        if (isChecked) {
            // Checkbox is checked
            if (!selectedRoleGroupIds.includes(roleGroupId)) {
                selectedRoleGroupIds.push(roleGroupId);
            }

            // If this was originally selected and was deselected, remove from deselected array
            if (wasOriginal) {
                const deselectedIndex = deselectedRoleGroupIds.indexOf(roleGroupId);
                if (deselectedIndex > -1) {
                    deselectedRoleGroupIds.splice(deselectedIndex, 1);
                }
            }
        } else {
            // Checkbox is unchecked
            selectedRoleGroupIds = selectedRoleGroupIds.filter(id => id !== roleGroupId);

            // If this was originally selected, add to deselected array
            if (wasOriginal && !deselectedRoleGroupIds.includes(roleGroupId)) {
                deselectedRoleGroupIds.push(roleGroupId);
            }
        }
    }
}

function initEditArchetypeButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                editArchetypeService.cancelButton();
                break;
            case 'confirm': {
                const id = target.closest('#confirm')?.dataset.id;
                editArchetypeService.saveEditedArchetype(id);
                break;
            }
        }
    });

    $(document).on('change', (e) => {
        const target = e.target;

        if (target.classList.contains('updateRoleGroupBtn') || target.closest('.updateRoleGroupBtn')) {
            const checkbox = target.closest('.updateRoleGroupBtn');
            editArchetypeService.updateRoleGroup(checkbox);
        } else if (target.classList.contains('updateRoleBtn') || target.closest('.updateRoleBtn')) {
            const checkbox = target.closest('.updateRoleBtn');
            editArchetypeService.updateRole(checkbox)
        } else if (target.classList.contains('updateGroupBtn') || target.closest('.updateGroupBtn')) {
            const checkbox = target.closest('.updateGroupBtn');
            editArchetypeService.updateGroup(checkbox);
        }
    });
}
