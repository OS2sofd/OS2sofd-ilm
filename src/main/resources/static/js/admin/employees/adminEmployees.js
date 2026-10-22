import {initStartAndStopDatepicker, formatDate, formatDateForBackend} from '../../fragments/datePickerService.js';
import verifyInputs from "../../fragments/verifyInputService.js";
import initializeDataTable from "../../fragments/dataTableService.js";
import {createArchetypeService} from "../../fragments/archetypeService.js";

var token = $("meta[name='_csrf']").attr("content");
let adminEmployeeService;
const { closeModal, saveArchetype, updateArchetype } = createArchetypeService();

$(document).ready(function() {
    adminEmployeeService = new AdminEmployeeService();
    initAdminEmployeeButtons();

    // Init start date to today on load
    adminEmployeeService.initStartDate(new Date());
    adminEmployeeService.onSelectedBusiness();
});

function AdminEmployeeService() {
    this.initStartDate = function (dateObject) {
        let startDate = $('#startDate');
        let today = formatDate(dateObject);
        startDate.val(today);
    }

    this.employeeArchetypeTable = function () {
        initializeDataTable('#archetypeTable', token, {
            'columnDefs': [
                {
                    'targets': 1,
                    'orderable': false
                }
            ]
        });
    }

    this.cancelButton = function () {
        globalThis.location.href = globalThis.location.origin + "/employees/overview";
    }

    this.saveCreatedEmployee = async function () {
        let name = $('#createEmployeeName').val();
        let email = $('#createEmployeeMail').val();
        let phone = $('#createEmployeePhone').val() || null;
        let cpr = $('#createEmployeeCpr').val().replace(/-/g, '');
        let uuid = $('#createEmployeeUuid').val();
        let business = $('#businessSelect').find("option:selected").val();
        let startDate = $('#startDate').datepicker('getDate');
        let stopDate = $('#stopDate').datepicker('getDate');
        let position = $('#createEmployeePos').val();
        let archetype = $('#archetype').data('id');
        let ou = $('#entity').data('uuid');
        let activation = $('#activationSelect').find("option:selected").val();
        let enableOrdering = $('#enableOrdering').is(':checked');

        const isValid = verifyInputs([
            {value: name, message: "Du skal angive et navn"},
            {value: email, message: "Du skal angive en mailadresse"},
            {value: startDate, message: "Du skal angive et start dato"},
            {value: position, message: "Du skal angive en stilling"},
            {value: archetype, message: "Du skal vælge en arketype"},
            {value: ou, message: "Du skal vælge en organisation"},
            {value: [cpr, uuid], message: "Du skal angive et CPR-nummer eller en UUID", type: 'oneOf'},
            {value: cpr, message: "CPR-nummer skal være 10 tegn", type: 'custom', condition: () => !cpr || cpr.length === 10},
            {value: startDate, message: "Start dato må ikke være efter stop dato", compareValue: stopDate, type: 'greaterThan'}
        ]);

        if (!isValid) {
            return;
        }

        const backendStartDate = formatDateForBackend(startDate);
        const backendStopDate = formatDateForBackend(stopDate);

        let data = {
            "name": name,
            "email": email,
            "phone": phone,
            "cpr": cpr,
            "uuid": uuid,
            "businessId": business,
            "startDate": backendStartDate,
            "stopDate": backendStopDate,
            "position": position,
            "archetype": archetype,
            "orgUnit": ou,
            "activationId": activation,
            "enableOrdering": enableOrdering
        }

        try {
            const url = '/rest/employee/admin/create';
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

            toastr.success("Medarbejder er oprettet");
            location.href = '/employees/overview';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.chooseArchetype = function () {
        adminEmployeeService.employeeArchetypeTable();
        $('#modal-archetype').modal("show");
    }

    this.onSelectedBusiness = function () {
        $('#businessSelect').on('change', async function() {
            let businessId = $(this).val();
            $('#archetype').val("");

            if (businessId && businessId !== '0') {
                // Fetch the business's activation timeframe
                try {
                    const url = `/rest/business/admin/employees/${businessId}/activation`;
                    const response = await fetch(url, {
                        method: 'GET',
                        headers: {
                            'content-type': 'application/json',
                            'X-CSRF-TOKEN': token
                        }
                    });

                    if (!response.ok) {
                        throw new Error("Der opstod en teknisk fejl");
                    }

                    const body = await response.json();
                    if (body.activationTimeFrameId) {
                        $('#activationSelect').val(body.activationTimeFrameId);
                    } else {
                        $('#activationSelect').val('0'); // Set to "Ikke tidsbegrænset"
                    }

                } catch {
                    // Default to "Ikke tidsbegrænset" on error
                    $('#activationSelect').val('0');
                }

                try {
                    const url = `/rest/business/${businessId}/archetypes`;
                    const response = await fetch(url, {
                        method: 'GET',
                        headers: {
                            'content-type': 'application/json',
                            'X-CSRF-TOKEN': token
                        }
                    });

                    if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error(errorText);
                    }

                    const archetypes = await response.json();
                    populateArchetypeTable(archetypes, true);
                } catch (error) {
                    populateArchetypeTable([], true);
                    console.error(error.message);
                    toastr.warning(error.message);
                }
            } else {
                // Reset to default when no business is selected
                $('#activationSelect').val('0');
                populateArchetypeTable([], false);
            }

            // Default the "order Exchange account" checkbox from the selected business' settings
            const selectedBusinessOption = this.options[this.selectedIndex];
            const enableOrdering = selectedBusinessOption?.dataset.enableOrdering === 'true';
            if (typeof $.fn.iCheck !== 'undefined') {
                $('#enableOrdering').iCheck(enableOrdering ? 'check' : 'uncheck');
            } else {
                $('#enableOrdering').prop('checked', enableOrdering);
            }

            handleDatesAndOu(this);
        });
    }
}

function handleDatesAndOu(selectElement) {
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    const defaultOrgUnitUuid = selectedOption?.dataset.defaultOrgUnitUuid;
    const defaultOrgUnitName = selectedOption?.dataset.defaultOrgUnitName;
    const maxFutureDays = selectedOption?.dataset.maxDaysFuture;
    const defaultEmployment = selectedOption?.dataset.defaultEmploymentDays;
    const maxEmployment = selectedOption?.dataset.maxEmploymentDays;
    let entity = $('#entity');
    let stopDate = $('#stopDate');

    if (defaultOrgUnitUuid && defaultOrgUnitName) {
        entity.val(defaultOrgUnitName);

        entity.data('uuid', defaultOrgUnitUuid);
        entity.data('name', defaultOrgUnitName);
    } else {
        entity.val('');
        entity.removeData('uuid');
        entity.removeData('name');
    }

    if (defaultEmployment) {
        let today = new Date();
        let newDate = new Date(today);
        let days = Number(defaultEmployment);
        newDate.setDate(today.getDate() + days);

        const newStopDate = formatDate(newDate);
        stopDate.val(newStopDate);
    }

    initStartAndStopDatepicker('startDate', 'stopDate', Number(maxFutureDays), Number(maxEmployment), Number(defaultEmployment));
}

function initAdminEmployeeButtons() {
    $('#cancel').on('click', () => adminEmployeeService.cancelButton());
    $('.close-modal').each(function (index, element) {
        $(element).on('click', () => closeModal());
    });
    $('#save-archetype').on('click', () => saveArchetype());
    $('#chooseArchetype').on('click', () => adminEmployeeService.chooseArchetype());
    $('#confirm').on('click', function () {
        adminEmployeeService.saveCreatedEmployee(this);
    });

    $(document).on('change', '.updateArchetypeButtons', function() {
        updateArchetype(this);
    });
}

function createArchetypeRow(archetype) {
    const row = document.createElement('tr');

    // Create name cell
    const nameCell = document.createElement('td');
    nameCell.dataset.value = archetype.id;
    nameCell.textContent = archetype.name || '';

    // Create action cell with radio input
    const actionCell = document.createElement('td');
    const radioInput = document.createElement('input');

    // Set radio input properties
    Object.assign(radioInput, {
        type: 'radio',
        name: 'archetypeChoice',
        id: 'archetypeChoice' + archetype.id,
        className: 'i-checks updateArchetypeButtons'
    });

    // Set data attributes
    radioInput.dataset.name = archetype.name || '';
    radioInput.dataset.id = archetype.id;

    actionCell.appendChild(radioInput);
    row.appendChild(nameCell);
    row.appendChild(actionCell);

    return row;
}

function createEmptyMessageRow(message) {
    const row = document.createElement('tr');

    // Create TWO cells instead of one with colspan to avoid DataTables issues
    const messageCell = document.createElement('td');
    messageCell.className = 'text-center text-muted';
    messageCell.textContent = message;

    const emptyCell = document.createElement('td');
    emptyCell.className = 'text-center text-muted';
    emptyCell.innerHTML = '&nbsp;'; // Non-breaking space to keep the cell structure

    row.appendChild(messageCell);
    row.appendChild(emptyCell);

    return row;
}

function populateArchetypeTable(archetypes, businessSelected = true) {
    const table = $('#archetypeTable');
    const tableBody = document.querySelector('#archetypeTable tbody');

    // Destroy DataTable if it exists
    if ($.fn.DataTable.isDataTable('#archetypeTable')) {
        table.DataTable().destroy();
    }

    // Clear existing content
    tableBody.innerHTML = '';

    const fragment = document.createDocumentFragment();

    if (!businessSelected) {
        // No business selected state
        const row = createEmptyMessageRow('Vælg venligst en virksomhed for at se tilgængelige arketyper');
        fragment.appendChild(row);

    } else if (archetypes && archetypes.length > 0) {
        // Business selected with archetypes
        archetypes.forEach(function(archetype) {
            const row = createArchetypeRow(archetype);
            fragment.appendChild(row);
        });

    } else {
        // Business selected but no archetypes available
        const row = createEmptyMessageRow('Ingen arketyper tilgængelige for denne virksomhed');
        fragment.appendChild(row);
    }

    tableBody.appendChild(fragment);

    // Only reinitialize DataTable and other plugins if we have actual archetype rows
    if (businessSelected && archetypes && archetypes.length > 0) {
        // Reinitialize DataTable
        table.DataTable({
            "searching": true,
            "paging": false,
            "info": false,
            "autoWidth": false,
            "columnDefs": [
                { "orderable": false, "targets": 1 }
            ]
        });

        // Reinitialize other plugins
        if (typeof $.fn.iCheck !== 'undefined') {
            $('.i-checks').iCheck({
                checkboxClass: 'icheckbox_square-green',
                radioClass: 'iradio_square-green'
            });
        }
    }
}
