import {initStartAndStopDatepicker, formatDateForBackend} from '../../fragments/datePickerService.js';
import verifyInputs from "../../fragments/verifyInputService.js";
import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let editEmployeeService;

$(document).ready(function() {
    initEditEmployeeButtons();

    editEmployeeService = new EditEmployeeService();
    editEmployeeService.employeeRoleTable();
    editEmployeeService.employeeGroupTable();
    editEmployeeService.employeeRoleGroupTable();
    editEmployeeService.initializeOriginalRoleState();
    editEmployeeService.initializeOriginalGroupState();
    editEmployeeService.initializeOriginalRoleGroupState();
    editEmployeeService.onSelectedBusiness();

    const restriction = document.getElementById('pageLoadDateRestrictions');
    const initMaxFutureDays = restriction?.dataset.maxFuture;
    const initMaxEmploymentDays = restriction?.dataset.maxEmployment;
    const initDefaultEmploymentDays = restriction?.dataset.defaultEmployement;
    initStartAndStopDatepicker('editStartDate', 'editStopDate', initMaxFutureDays, initMaxEmploymentDays, initDefaultEmploymentDays);
});

function confirmCprChange() {
    return new Promise(resolve => {
        swal({
            title: 'Skift CPR-nummer',
            text: "Medarbejderens nuværende AD-konto og SOFD-tilknytning vil blive slettet, og der oprettes en ny med det nye CPR-nummer. Vil du fortsætte?",
            showCancelButton: true,
            confirmButtonColor: "red",
            confirmButtonText: "Ja",
            cancelButtonText: "Nej",
            closeOnConfirm: true,
            closeOnCancel: true
        }, isConfirm => resolve(!!isConfirm));
    });
}

function EditEmployeeService () {
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

    this.employeeRoleTable = function () {
        initializeDataTable('#roleTable', token, {
            'columnDefs': [
                {
                    'targets': 3,
                    'orderable': false
                }
            ]
        });
    }

    this.employeeGroupTable = function () {
        initializeDataTable('#groupTable', token, {
            'columnDefs': [
                {
                    'targets': 1,
                    'orderable': false
                }
            ]
        });
    }

    this.employeeRoleGroupTable = function () {
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
        location.href = '/employees/overview';
    }

    this.saveEditedEmployee = async function (id) {
        const name = document.getElementById('editEmployeeName').value;
        const email = document.getElementById('editEmployeeMail').value;
        const phone = document.getElementById('editEmployeePhone').value;
        const business = document.getElementById('editBusinessSelect').value;
        const startDate = formatDateForBackend($('#editStartDate').datepicker('getDate'));
        const stopDate = formatDateForBackend($('#editStopDate').datepicker('getDate'));
        const position = document.getElementById('editEmployeePosition').value;

        // Only present for admins - see editEmployee.html
        const cprInput = document.getElementById('editEmployeeCpr');
        const cpr = cprInput ? cprInput.value.replace(/-/g, '') : null;
        const cprChanged = cprInput && cpr !== cprInput.defaultValue.replace(/-/g, '');

        const isValid = verifyInputs([
            { value: name, message: "Du skal angive et navn"},
            { value: email, message: "Du skal angive en mailadresse"},
            { value: startDate, message: "Du skal angive et start dato"},
            { value: stopDate, message: "Du skal angive et stop dato"},
            { value: position, message: "Du skal angive en stilling"},
            { value: business, message: "Du skal vælge en virksomhed", type: 'notZero'},
            { value: startDate, message: "Stop dato må ikke være før start dato", compareValue: stopDate, type: 'greaterThan' },
            { value: cpr, message: "CPR-nummer skal være 10 tegn", type: 'custom', condition: () => !cprChanged || cpr.length === 10 }
        ]);

        if (!isValid) {
            return;
        }

        if (cprChanged && !await confirmCprChange()) {
            return;
        }

        await editEmployeeService.submitEditedEmployee(id, name, email, phone, business, startDate, stopDate, position, cpr);
    }

    this.submitEditedEmployee = async function (id, name, email, phone, business, startDate, stopDate, position, cpr) {
        const rolesToAdd = selectedRoleIds.filter(roleId => !originalRoleIds.includes(roleId));
        const groupsToAdd = selectedGroupIds.filter(groupId => !originalGroupIds.includes(groupId));
        const roleGroupsToAdd = selectedRoleGroupIds.filter(roleGroupId => !originalRoleGroupIds.includes(roleGroupId));

        const data = {
            "name": name,
            "email": email,
            "phone": phone,
            "businessId": business,
            "startDate": startDate,
            "stopDate": stopDate,
            "position": position,
            "cpr": cpr,
            "rolesToAdd": rolesToAdd,
            "groupsToAdd": groupsToAdd,
            "roleGroupsToAdd": roleGroupsToAdd,
            "rolesToRemove": deselectedRoleIds,
            "groupsToRemove": deselectedGroupIds,
            "roleGroupsToRemove": deselectedRoleGroupIds
        };

        try {
            const response = await fetch('/rest/employee/edit/' + id, {
                method: 'PATCH',
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

            toastr.success("Medarbejder er opdateret");
            location.href = '/employees/overview';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.orderExchangeAccount = async function (id) {
        try {
            const url = `/rest/employee/orderExchange/${id}`;
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'content-type': 'application/json',
                    'X-CSRF-TOKEN': token
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            toastr.success("Der er bestilt en mailkonto til medarbejderen");
            // Order is now pending - prevent re-submitting from this page
            const button = document.getElementById('orderExchange');
            if (button) {
                button.disabled = true;
            }
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
            if (!selectedRoleIds.includes(roleId)) {
                selectedRoleIds.push(roleId);
            }
            if (wasOriginal) {
                const deselectedIndex = deselectedRoleIds.indexOf(roleId);
                if (deselectedIndex > -1) {
                    deselectedRoleIds.splice(deselectedIndex, 1);
                }
            }
        } else {
            selectedRoleIds = selectedRoleIds.filter(id => id !== roleId);
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
            if (!selectedGroupIds.includes(groupId)) {
                selectedGroupIds.push(groupId);
            }
            if (wasOriginal) {
                const deselectedIndex = deselectedGroupIds.indexOf(groupId);
                if (deselectedIndex > -1) {
                    deselectedGroupIds.splice(deselectedIndex, 1);
                }
            }
        } else {
            selectedGroupIds = selectedGroupIds.filter(id => id !== groupId);
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
            if (!selectedRoleGroupIds.includes(roleGroupId)) {
                selectedRoleGroupIds.push(roleGroupId);
            }
            if (wasOriginal) {
                const deselectedIndex = deselectedRoleGroupIds.indexOf(roleGroupId);
                if (deselectedIndex > -1) {
                    deselectedRoleGroupIds.splice(deselectedIndex, 1);
                }
            }
        } else {
            selectedRoleGroupIds = selectedRoleGroupIds.filter(id => id !== roleGroupId);
            if (wasOriginal && !deselectedRoleGroupIds.includes(roleGroupId)) {
                deselectedRoleGroupIds.push(roleGroupId);
            }
        }
    }

    this.onSelectedBusiness = function () {
        document.getElementById('editBusinessSelect')?.addEventListener('change', function () {
            const selectedOption = this.options[this.selectedIndex];
            const maxFutureDays = selectedOption?.dataset.maxDaysFuture;
            const maxEmployment = selectedOption?.dataset.maxEmploymentDays;
            const defaultEmployment = selectedOption?.dataset.defaultEmploymentDays;

            initStartAndStopDatepicker('editStartDate', 'editStopDate', Number(maxFutureDays), Number(maxEmployment), Number(defaultEmployment));
        })
    }
}

function initEditEmployeeButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                editEmployeeService.cancelButton();
                break;
            case 'confirm': {
                const id = target.closest('#confirm').dataset.id;
                editEmployeeService.saveEditedEmployee(id);
                break;
            }
            case 'orderExchange': {
                const id = target.closest('#orderExchange').dataset.id;
                editEmployeeService.orderExchangeAccount(id);
                break;
            }
        }
    });

    $(document).on('change', (e) => {
        const target = e.target;

        if (target.classList.contains('updateRolesButton') || target.closest('.updateRolesButton')) {
            const checkbox = target.closest('.updateRolesButton');
            editEmployeeService.updateRole(checkbox);
        }
        else if (target.classList.contains('updateGroupsButton') || target.closest('.updateGroupsButton')) {
            const checkbox = target.closest('.updateGroupsButton');
            editEmployeeService.updateGroup(checkbox);
        }
        else if (target.classList.contains('updateRoleGroupsButton') || target.closest('.updateRoleGroupsButton')) {
            const checkbox = target.closest('.updateRoleGroupsButton');
            editEmployeeService.updateRoleGroup(checkbox);
        }
    });
}
