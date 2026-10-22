import verifyInputs from "../../fragments/verifyInputService.js";
import initializeSwal from "../../fragments/swalService.js";
import {initStartAndStopDatepicker, formatDateForBackend} from "../../fragments/datePickerService.js";

var token = $("meta[name='_csrf']").attr("content");
let employeeService;

let failedByEmployeeId = {};
const operationLabels = {
    SET_EXPIRE_DATE: 'Opdatering af udløbsdato',
    SET_GROUP_MEMBERSHIP: 'Tilføjelse af gruppemedlemskab',
    DELETE_GROUP_MEMBER_SHIP: 'Fjernelse af gruppemedlemskab',
    SET_PASSWORD: 'Nulstilling af kodeord'
};

$(document).ready(function() {
    employeeService = new EmployeeService();
    const isAdmin = $('#isAdmin').data('admin');
    if (isAdmin) {
        fetch('/rest/admin/adQueue/failed', { headers: { 'X-CSRF-TOKEN': token } })
            .then(r => r.json())
            .then(items => {
                items.forEach(item => {
                    if (item.employeeId) {
                        if (!failedByEmployeeId[item.employeeId]) {
                            failedByEmployeeId[item.employeeId] = [];
                        }
                        failedByEmployeeId[item.employeeId].push(item);
                    }
                });
            })
            .catch(() => {})
            .finally(() => employeeService.init());
    } else {
        employeeService.init();
    }
    initEmployeeButtons();
    setupBusinessChangeHandler();
});

function EmployeeService() {
    let rolesToAdd = [];
    let groupsToAdd = [];

    this.init = function () {
        let tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        let minDate = tomorrow.toISOString().split('T')[0];

        $('#editStopDate').attr("min", minDate);
        $('#stopDate').attr("min", minDate);
        let employeeTable = $('#employeeTable').DataTable({
            "serverSide": true,
            "destroy": true,
            "order": [[0, 'desc']],
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "autoWidth": false,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "ajax": {
                "url": "/rest/employee/view",
                "type": "POST",
                "headers": {
                    "X-CSRF-TOKEN": token
                },
                "data": function (d) {
                    return JSON.stringify(d);
                },
                "contentType": "application/json"
            },
            "language": {
                "search": "Søg",
                "lengthMenu": "_MENU_ rækker per side",
                "info": "Viser _START_ til _END_ af _TOTAL_ rækker",
                "zeroRecords": "Ingen data...",
                "infoEmpty": "Henter data...",
                "infoFiltered": "(ud af _MAX_ rækker)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                }
            },
            "columns": [
                {"data": 'name'},
                {
                    "data": 'businessName', render: function (data, type, row) {
                        const isLeader = $('#isLeader').data('leader');
                        if (!isLeader && row.businessId) {
                            return '<span>' + (data || '') + '</span> <a href="/business/details/' + row.businessId + '"><em class="fa fa-fw fa-external-link" style="color: #000000"></em></a>'
                        }
                        return '<span>' + (data || '') + '</span>';
                    }
                },
                {"data": 'cvr'},
                {"data": 'orgUnitName'},
                {"data": 'samAccountName'},
                {"data": 'startDate'},
                {"data": 'stopDate'},
                {
                    "data": 'approved', render: function (data, type, row) {
                        let html = '';
                        if (row.approved === 'AWAITING_APPROVAL') {
                            html += '<span>Afventer godkendelse</span>';
                        } else if (row.approved === 'ADMIN_APPROVED') {
                            html += '<span>Administrator har godkendt</span>';
                        } else if (row.approved === 'DENIED') {
                            html += '<span>Afvist</span>';
                        } else if (row.approved === 'LEADER_APPROVED') {
                            html += '<span>Leder har godkendt</span>';
                        } else if (row.approved === 'AWAITING_EXTENSION_APPROVAL') {
                            html += '<span>Afventer godkendelse om forlængelse</span>';
                        }
                        return html;
                    }
                },
                {
                    "data": 'integrated', render: function (data, type, row) {
                        const isAdmin = $('#isAdmin').data('admin');
                        if (isAdmin && failedByEmployeeId[row.id]) {
                            const createItem = failedByEmployeeId[row.id].find(i => i.operation === 'CREATE');
                            if (createItem) {
                                return '<span style="color:#e74c3c">AD oprettelse fejlet</span>';
                            }
                        }
                        let html = '';
                        if (row.integrated === 'AWAITING_FULL_INTEGRATION' || row.integrated === 'AWAITING_RC_INTEGRATION') {
                            if (!row.acceptedTos) {
                                html += '<span>Medarbejder skal godkende vilkår</span>';
                            } else {
                                html += '<span>Afventer synkronisering</span>';
                            }
                        } else if (row.integrated === 'FULLY_INTEGRATED') {
                            html += '<span>Fuldt synkroniseret</span>';
                        } else if (row.integrated === null) {
                            html += '<span>Ikke synkroniseret</span>';
                        }
                        return html;
                    }
                },
                {"data": 'exchangeAccountStatus', render: function (data, type, row) {
                    const span = document.createElement('span');
                    const label = document.createElement('label');
                    const icon = document.createElement('em');
                    switch (data) {
                        case 'NEEDS_ORDER': {
                            label.textContent = 'Afventer bestilling';
                            icon.className = 'fa fa-spinner';
                            break;
                        }
                        case 'NONE': {
                            label.textContent = 'Ikke bestilt';
                            icon.className = 'fa fa-shopping-cart text-muted';
                            break;
                        }
                        case 'HAS_ACCOUNT': {
                            label.textContent = 'Bestilt hos SOFD';
                            icon.className = 'fa fa-check-circle';
                            break;
                        }
                        case 'ORDER_FAILED': {
                            label.textContent = 'Bestilling fejlede';
                            icon.className = 'fa fa-times-circle';
                            break;
                        }
                    }

                    icon.classList.add('m-r-xs')
                    span.appendChild(icon);
                    span.appendChild(label);
                    return span.outerHTML;
                    }
                },
                {
                    "data": 'id', render: function (data, type, row) {
                        let html = '';

                        const isAdmin = $('#isAdmin').data('admin');
                        const isLeader = $('#isLeader').data('leader');
                        const resetUrl = $('#passwordResetLink').data('reset-url');

                        if (isAdmin && failedByEmployeeId[row.id]) {
                            const failedItems = failedByEmployeeId[row.id];
                            const createItem = failedItems.find(i => i.operation === 'CREATE');
                            const representative = createItem ?? failedItems[0];
                            const icon = document.createElement('em');
                            icon.className = 'fa fa-fw fa-exclamation-triangle';
                            const link = document.createElement('a');
                            link.style.margin = '5px';
                            link.style.color = '#e74c3c';
                            link.dataset.queueId = representative.queueItemId;
                            link.dataset.operation = representative.operation;
                            link.dataset.employeeName = row.name;
                            link.className = 'retryQueueBtn';
                            link.appendChild(icon);
                            // If CREATE failed the account doesn't exist — normal buttons are meaningless
                            if (createItem) {
                                return link.outerHTML;
                            }
                            html += link.outerHTML;
                        }

                        if (isAdmin || isLeader) {

                            if (row.approved === 'DENIED') {
                                html += '<a style="margin: 5px" data-id="' + row.id + '" class="changeEmployeeApprovedStatusBtn"><em class="fa fa-fw fa-check"></em></a>';
                            }

                            // Should not be able to edit an employees roles, groups, startdate etc. when the employee is not approved and thus doesn't have an adUser
                            if (row.samAccountName != null) {
                                html += '<a style="margin: 5px" data-id="' + row.id + '" class="editEmployeeBtn"><em class="fa fa-fw fa-pencil"></em></a>';
                            }
                            // These operations should only be available if the employee has an AD user
                            if (row.stopDate && row.samAccountName != null) {
                                const stopDate = new Date(row.stopDate);
                                const today = new Date();
                                // Might be dumb, but LocalDate has time set to 2 am for some reason
                                today.setHours(2, 0, 0, 0);

                                if (stopDate > today) {
                                    html += '<a style="margin: 5px" data-id="' + row.id + '" class="extendAccountBtn"><em class="fa fa-fw fa-plus"></em></a>';
                                    html += '<a style="margin: 5px" data-id="' + row.id + '" class="lockAccountBtn"><em class="fa fa-fw fa-lock"></em></a>';

                                    if (resetUrl) {
                                        html += '<a style="margin: 5px" data-id="' + row.id + '" data-name="' + row.name + '" href="' + resetUrl + '" target="_blank" rel="noopener noreferrer" title="Nulstil AD-Kodeord"><em style="color: #000000" class="fa fa-fw fa-external-link"></em></a>';
                                    } else {
                                        html += '<a style="margin: 5px" data-id="' + row.id + '" data-name="' + row.name + '" class="resetPasswordBtn" title="Nulstil AD-Kodeord"><em class="fa fa-fw fa-undo"></em></a>';
                                    }
                                }
                                if (today >= stopDate) {
                                    html += '<a style="margin: 5px" data-id="' + row.id + '" class="unlockAccountBtn"><em class="fa fa-fw fa-unlock"></em></a>';
                                }
                            }
                        }

                        if (isAdmin) {
                            html += '<a style="margin: 5px" data-id="' + row.id + '" data-name="' + row.name + '" class="deleteEmployeeBtn" title="Slet medarbejder"><em class="fa fa-fw fa-trash"></em></a>';
                        }
                        return html;
                    }
                }
            ],
            'columnDefs': [
                {
                    'targets': 10,
                    'orderable': false
                }
            ]
        });
        $.each($('.input-filter', employeeTable.table().footer()), function () {
            const column = employeeTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });

            $('input:is(.date-filter), select', this).on('keyup change', function () {
                let value = this.value;

                if (!value) {
                    column.search('').draw();
                    return;
                }

                let parts = value.split('/');
                let date = parts[2] + '-' + parts[1] + '-' + parts[0];

                if (column.search() !== date) {
                    column.search(date).draw();
                }
            });
        });
        $('.date-filter').datepicker({
            format: 'dd/mm/yyyy',
            autoclose: true,
            todayHighlight: true,
            weekStart: 1
        });
        $('.input-group-addon').on('click', function () {
            $(this).siblings('input.date-filter').datepicker('show');
        });

        // Restore previously saved column visibility (defaults to all visible)
        const storageKey = 'employeeTableHiddenColumns';
        let hiddenColumns = [];
        try {
            hiddenColumns = JSON.parse(localStorage.getItem(storageKey)) || [];
        } catch {
            hiddenColumns = [];
        }
        $('.column-toggle').each(function () {
            const columnIndex = $(this).data('column');
            const isVisible = !hiddenColumns.includes(columnIndex);
            employeeTable.column(columnIndex).visible(isVisible, false);
            if (typeof $.fn.iCheck === 'undefined') {
                this.checked = isVisible;
            } else {
                $(this).iCheck(isVisible ? 'check' : 'uncheck');
            }
        });
        employeeTable.columns.adjust();

        // Keep the column-options dropdown open while toggling checkboxes
        $('#columnOptionsMenu').on('click', function (e) {
            e.stopPropagation();
        });

        // Apply and persist the selected column visibility when "Opdater" is clicked
        $('#updateColumnsButton').on('click', function () {
            const hidden = [];
            $('.column-toggle').each(function () {
                const columnIndex = $(this).data('column');
                employeeTable.column(columnIndex).visible(this.checked, false);
                if (!this.checked) {
                    hidden.push(columnIndex);
                }
            });
            employeeTable.columns.adjust();
            localStorage.setItem(storageKey, JSON.stringify(hidden));
            $('#toggleColumnOptionsButton').dropdown('hide');
        });
    }

    this.cancelButton = function () {
        globalThis.location.href = globalThis.location.origin + "/employees/overview";
    }

    this.resetPassword = function (id, name) {
        const params = {
            title: 'Nulstil kodeord for ' + name + '?',
            text: 'En besked med et nyt kodeord vil blive sendt',
            id: id,
            token: token,
            success: 'Medarbejder har nulstillet sit kodeord',
            warning: 'Kunne ikke nulstille kodeord',
            url: '/rest/employee/resetPassword/'
        };
        initializeSwal(params);
    }

    this.lockAccount = function (id) {
        const params = {
            title: 'Lås Medarbejder',
            text: 'Medarbejderens AD konto vil blive låst og de vil modtage en besked',
            id: id,
            token: token,
            success: 'Medarbejder spærret',
            warning: 'Kunne ikke spærre medarbejder',
            url: '/rest/employee/lock/'
        };
        initializeSwal(params);
    }

    this.deleteEmployee = function (id, name) {
        const params = {
            title: 'Slet ' + name + '?',
            text: 'Medarbejderen vil blive slettet fra ILM. Deres tilhørsforhold i OS2sofd samt deres AD konto vil også slettes. Dette kan ikke fortrydes.',
            id: id,
            token: token,
            success: 'Medarbejder blev slettet',
            warning: 'Kunne ikke slette medarbejder',
            url: '/rest/employee/delete/',
            method: 'DELETE'
        };
        initializeSwal(params);
    }

    this.unlockAccount = function (id) {
        swal({
            title: 'Genåben medarbejder',
            text: "Du skal vælge en ny slutdato for at genåbne medarbejderen. De vil modtage en besked om dette.",
            type: "input",
            inputType: "date",
            showCancelButton: true,
            confirmButtonColor: "#18a689",
            confirmButtonText: "Gem",
            cancelButtonText: "Annuller",
            closeOnConfirm: false
        }, async function (newDate) {
            if (newDate) {
                try {
                    const url = '/rest/employee/unlock';
                    const response = await fetch(url, {
                        method: 'POST',
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        body: JSON.stringify({
                            "id": id,
                            "stopDate": newDate
                        })
                    });

                    if (!response.ok) {
                        const errorText = await response.text()
                        throw new Error(errorText);
                    }

                    toastr.success("Medarbejder blev genåbnet");
                    location.reload();
                } catch (error) {
                    console.error(error.message);
                    toastr.warning(error.message);
                }
            }
        });
    }

    this.extendAccount = function (id) {
        swal({
            title: 'Forlæng eller forkort Tildeling',
            text: "Vælg ny slutdato:",
            type: "input",
            inputType: "date",
            showCancelButton: true,
            confirmButtonColor: "#18a689",
            confirmButtonText: "Gem",
            cancelButtonText: "Annuller",
            closeOnConfirm: false
        }, async function (newDate) {
            if (newDate) {
                try {
                    const url = '/rest/employee/extend';
                    const response = await fetch(url, {
                        method: 'POST',
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        body: JSON.stringify({
                            "id": id,
                            "stopDate": newDate
                        })
                    });

                    if (!response.ok) {
                        const errorText = await response.text()
                        throw new Error(errorText);
                    }

                    toastr.success("Medarbejder blev forlænget");
                    location.reload();
                } catch (error) {
                    console.error(error.message);
                    toastr.warning(error.message);
                }
            }
        });
    }

    this.editEmployee = function (id) {
        location.href = `/employee/edit/${id}`;
    }

    this.changeEmployeeApprovedStatus = function(id) {
        location.href = `/approval/${id}`;
    }

    this.saveCreatedEmployee = async function () {
        const name = $('#createEmployeeName').val();
        const email = $('#createEmployeeMail').val();
        const phone = $('#createEmployeePhone').val();
        const cpr = $('#createEmployeeCpr').val();
        const uuid = $('#createEmployeeUuid').val();
        const business = $('#businessSelect').find("option:selected").val();
        const startDate = $('#startDate').val();
        const stopDate = $('#stopDate').val();
        const position = $('#createEmployeePos').val();

        let startDateValue = startDate;
        let stopDateValue = stopDate;

        if (startDate && stopDate) {
            let startDateObj = $('#startDate').datepicker('getDate');
            let stopDateObj = $('#stopDate').datepicker('getDate');

            if (startDateObj && stopDateObj) {
                startDateValue = formatDateForBackend(startDateObj);
                stopDateValue = formatDateForBackend(stopDateObj);
            }
        }

        const isValid = verifyInputs([
            { value: name, message: "Du skal angive et navn"},
            { value: email, message: "Du skal angive en mailadresse"},
            { value: startDate, message: "Du skal angive et start dato"},
            { value: stopDate, message: "Du skal angive et stop dato"},
            { value: position, message: "Du skal angive en stilling"},
            { value: business, message: "Du skal vælge en virksomhed", type: 'notZero'},
            { value: [cpr, uuid], message: "Du skal angive et CPR-nummer eller en UUID", type: 'oneOf' },
            { value: startDateValue, message: "Start dato må ikke være efter stop dato", compareValue: stopDateValue, type: 'greaterThan' }
        ]);

        if (!isValid) {
            return;
        }

        let data = {
            "name": name,
            "email": email,
            "phone": phone,
            "cpr": cpr,
            "uuid": uuid,
            "businessId": business,
            "startDate": startDateValue,
            "stopDate": stopDateValue,
            "position": position
        }

        try {
            const url = '/rest/employee/create';
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

            toastr.success("Medarbejder blev oprettet");
            location.href = '/employees/overview';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.openDetails = function (obj) {
        let id = $(obj).data("id");
        globalThis.location.href = "/employee/details/" + id;
    }

    this.closeModal = function () {
        for (const element of rolesToAdd) {
            $('#roleChoice' + element).iCheck('uncheck');
        }
        rolesToAdd = [];
        $('#addRoleModal').modal('hide');
    }

    this.updateRole = function (obj) {
        let id = $(obj).data("id");
        if ($('#roleChoice' + id).prop("checked")) {
            if (!rolesToAdd.includes(id)) {
                rolesToAdd.push(id);
            }
        }
    }

    this.updateGroup = function (obj) {
        let id = $(obj).data("id");
        if ($('#groupChoice' + id).prop("checked")) {
            if (!groupsToAdd.includes(id)) {
                groupsToAdd.push(id);
            }
        }
    }
}

function setupBusinessChangeHandler() {
    $('#businessSelect').on('change', function() {
        const selectedOption = this.options[this.selectedIndex];
        const defaultEmployment = Number(selectedOption?.dataset.defaultEmploymentDays) || 0;
        const maxFutureDays = Number(selectedOption?.dataset.maxDaysFuture) || 0;
        const maxEmployment = Number(selectedOption?.dataset.maxEmploymentDays) || 0;

        initStartAndStopDatepicker('startDate', 'stopDate', maxFutureDays, maxEmployment, defaultEmployment);
    });
}

function initEmployeeButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                employeeService.cancelButton();
                break;
            case 'confirm':
                employeeService.saveCreatedEmployee();
                break;
        }

        if (target.classList.contains('editEmployeeBtn') || target.closest('.editEmployeeBtn')) {
            const id = target.closest('.editEmployeeBtn')?.dataset.id;
            employeeService.editEmployee(id);
        }
        else if (target.classList.contains('changeEmployeeApprovedStatusBtn') || target.closest('.changeEmployeeApprovedStatusBtn')) {
            const id = target.closest('.changeEmployeeApprovedStatusBtn')?.dataset.id;
            employeeService.changeEmployeeApprovedStatus(id);
        }
        else if (target.classList.contains('extendAccountBtn') || target.closest('.extendAccountBtn')) {
            const id = target.closest('.extendAccountBtn')?.dataset.id;
            employeeService.extendAccount(id);
        }
        else if (target.classList.contains('lockAccountBtn') || target.closest('.lockAccountBtn')) {
            const id = target.closest('.lockAccountBtn')?.dataset.id;
            employeeService.lockAccount(id);
        }
        else if (target.classList.contains('resetPasswordBtn') || target.closest('.resetPasswordBtn')) {
            const id = target.closest('.resetPasswordBtn')?.dataset.id;
            const name = target.closest('.resetPasswordBtn')?.dataset.name;
            employeeService.resetPassword(id, name);
        }
        else if (target.classList.contains('unlockAccountBtn') || target.closest('.unlockAccountBtn')) {
            const id = target.closest('.unlockAccountBtn')?.dataset.id;
            employeeService.unlockAccount(id);
        }
        else if (target.classList.contains('deleteEmployeeBtn') || target.closest('.deleteEmployeeBtn')) {
            const btn = target.closest('.deleteEmployeeBtn');
            const id = btn?.dataset.id;
            const name = btn?.dataset.name;
            employeeService.deleteEmployee(id, name);
        }
        else if (target.classList.contains('retryQueueBtn') || target.closest('.retryQueueBtn')) {
            const btn = target.closest('.retryQueueBtn');
            const queueId = btn?.dataset.queueId;
            const operation = btn?.dataset.operation;
            const employeeName = btn?.dataset.employeeName;

            if (operation === 'CREATE') {
                retryWithNewUsername(queueId, employeeName);
                return;
            }

            retryAdOperation(operation, employeeName, queueId);
        }
    });
}

function clearFailedItem(queueId) {
    const employeeId = Object.keys(failedByEmployeeId).find(k => failedByEmployeeId[k].some(i => i.queueItemId == queueId));
    if (employeeId) {
        failedByEmployeeId[employeeId] = failedByEmployeeId[employeeId].filter(i => i.queueItemId != queueId);
        if (failedByEmployeeId[employeeId].length === 0) {
            delete failedByEmployeeId[employeeId];
        }
    }
    $('#employeeTable').DataTable().draw(false);
}

function retryWithNewUsername(queueId, employeeName) {
    swal({
        title: 'Forsøg igen med nyt brugernavn',
        text: `Oprettelse af AD-konto for ${employeeName} fejlede. Muligvis fordi brugernavnet allerede er i brug. Angiv et nyt brugernavn:`,
        type: 'input',
        showCancelButton: true,
        confirmButtonColor: '#18a689',
        confirmButtonText: 'Genforsøg',
        cancelButtonText: 'Annuller',
        closeOnConfirm: false
    }, async function (newUsername) {
        if (!newUsername) {
            return;
        }

        try {
            const url = `/rest/admin/adQueue/${queueId}/retryWithNewUsername`;
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'content-type': 'application/json',
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify({newUsername: newUsername})
            });

            if (response.status === 409) {
                toastr.info("Køelementet bliver allerede behandlet");
                return;
            } else if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            toastr.success("Køelementet blev lagt i køen med nyt brugernavn");
            swal.close();
            clearFailedItem(queueId);
        } catch (error) {
            toastr.warning(error?.message ? escapeHtml(error.message) : "Køelementet kunne ikke lægges tilbage i køen");
        }
    });
}

function retryAdOperation(operation, employeeName, queueId) {
    const operationLabel = operationLabels[operation] ?? operation;
    swal({
        title: 'Genforsøg AD operation?',
        text: `${operationLabel} for ${employeeName} fejlede. Vil du lægge opgaven i kø igen?`,
        showCancelButton: true,
        confirmButtonColor: '#18a689',
        confirmButtonText: 'Ja',
        cancelButtonText: 'Nej',
        closeOnConfirm: true,
        closeOnCancel: true
    }, async function(isConfirm) {
        if (!isConfirm) {
            return;
        }

        try {
            const url = `/rest/admin/adQueue/${queueId}/retry`;
            const response = await fetch(url, {
                method: 'POST',
                headers: {'X-CSRF-TOKEN': token}
            });

            if (response.status === 409) {
                toastr.info("Køelementet bliver allerede behandlet");
                return;
            } else if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            toastr.success('Køelementet blev lagt tilbage i køen');
            clearFailedItem(queueId);
        } catch (error) {
            toastr.warning(error?.message ? escapeHtml(error.message) : "Køelementet kunne ikke lægges tilbage i køen");
        }
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
