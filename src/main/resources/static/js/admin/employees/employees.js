var token = $("meta[name='_csrf']").attr("content");
var employeeService;

$(document).ready(function() {
    employeeService = new EmployeeService();
    employeeService.init();
    employeeService.groupTable();
    employeeService.roleTable();
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
        var employeeTable = $('#employeeTable').DataTable({
            "serverSide": true,
            "destroy": true,
            "order": [[0, 'desc']],
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
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
                { "data": 'name' },
                { "data": 'cvr', render: function (data, type, row) {
                    const isLeader = $('#isLeader').data('leader');
                    if (!isLeader) {
                        return '<span>'+ data +'</span><a href="/business/details/'+ row.businessId +'"><em class="fa fa-fw fa-external-link" style="color: #000000"></em></a>'
                    }
                    return '<span>' + data + '</span>';
                    }},
                { "data": 'orgUnitName' },
                { "data": 'samAccountName' },
                { "data": 'startDate'},
                { "data": 'stopDate' },
                { "data": 'approved', render: function (data, type, row) {
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
                    }},
                { "data": 'integrated', render: function (data, type, row) {
                    let html = '';
                    if (row.integrated === 'AWAITING_FULL_INTEGRATION' || row.integrated === 'AWAITING_RC_INTEGRATION') {
                        html += '<span>Afventer synkronisering</span>';
                    } else if (row.integrated === 'FULLY_INTEGRATED') {
                        html += '<span>Fuldt synkroniseret</span>';
                    } else if (row.integrated === null) {
                        html += '<span>Ikke synkroniseret</span>';
                    }
                    return html;
                    }},
                { "data": 'id', render: function (data, type, row) {
                        let html = '';

                        const isAdmin = $('#isAdmin').data('admin');
                        const isLeader = $('#isLeader').data('leader');

                        if (isAdmin || isLeader) {
                            html += '<a style="margin: 5px" data-id="' + row.id + '" onclick="employeeService.editEmployee(this)"><em class="fa fa-fw fa-pencil"></em></a>';
                            // Allow to lock an account only if its not already expired
                            if (row.stopDate) {
                                const stopDate = new Date(row.stopDate);
                                const today = new Date();
                                // Might be dumb, but LocalDate has time set to 2 am for some reason
                                today.setHours(2, 0, 0, 0);

                                if (stopDate > today) {
                                    html += '<a style="margin: 5px" data-id="' + row.id + '" onclick="employeeService.extendAccount(this)"><em class="fa fa-fw fa-plus"></em></a>';
                                    html += '<a style="margin: 5px" data-id="' + row.id + '" onclick="employeeService.lockAccount(this)"><em class="fa fa-fw fa-lock"></em></a>';
                                    html += '<a style="margin: 5px" data-id="' + row.id + '" data-name="' + row.name + '" onclick="employeeService.resetPassword(this)"><em class="fa fa-fw fa-refresh"></em></a>';
                                }
                                if (today >= stopDate) {
                                    html += '<a style="margin: 5px" data-id="' + row.id + '" onclick="employeeService.unlockAccount(this)"><em class="fa fa-fw fa-unlock"></em></a>';
                                }
                            }
                        }
                        return html;
                    }}
            ]
        });
        $.each($('.input-filter', employeeTable.table().footer()), function() {
            var column = employeeTable.column($(this).index());

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
    }

    this.roleTable = function () {
        var roleTable = $('#roleTable').DataTable({
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
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
            }
        });
        $.each($('.input-filter', roleTable.table().footer()), function () {
            var column = roleTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.groupTable = function () {
        var groupTable = $('#groupTable').DataTable({
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
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
            }
        });
        $.each($('.input-filter', groupTable.table().footer()), function () {
            var column = groupTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.initRoleTable = function () {
        var roleTable = $('#allRolesTable').DataTable({
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
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
            }
        });
        $.each($('.input-filter', roleTable.table().footer()), function () {
            var column = roleTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.initGroupTable = function () {
        var groupTable = $('#allGroupsTable').DataTable({
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
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
            }
        });
        $.each($('.input-filter', groupTable.table().footer()), function () {
            var column = groupTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.cancelButton = function () {
        window.location.href = window.location.origin + "/employees/overview";
    }

    this.resetPassword = function (obj) {
        let id = $(obj).data("id");
        let name = $(obj).data("name");
        swal({
                title: 'Nulstil kodeord for ' + name + "?",
                text: "En besked med et nyt kodeord vil blive sendt.",
                showCancelButton: true,
                confirmButtonColor: "red",
                confirmButtonText: "Ja",
                cancelButtonText: "Nej",
                closeOnConfirm: true,
                closeOnCancel: true
            },
            function(isConfirm) {
                if (isConfirm) {
                    $.ajax({
                        method: "POST",
                        url: "/rest/employee/resetPassword/" + id,
                        headers: {
                            "Content-Type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function (data, textStatus, jqXHR) {
                            toastr.success("Medarbejder har nulstillet sit kodeord!");
                            window.location.reload();
                        }
                    });
                }
            }
        )
    }
    this.lockAccount = function (obj) {
        let id = $(obj).data("id");
        swal({
                title: 'Lås Medarbejder',
                text: "Medarbejderens AD konto vil blive låst og de vil modtage en besked",
                showCancelButton: true,
                confirmButtonColor: "red",
                confirmButtonText: "Ja",
                cancelButtonText: "Nej",
                closeOnConfirm: true,
                closeOnCancel: true
            },
            function(isConfirm) {
                if (isConfirm) {
                    $.ajax({
                        method: "POST",
                        url: "/rest/employee/lock/" + id,
                        headers: {
                            "Content-Type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function (data, textStatus, jqXHR) {
                            toastr.success("Medarbejder spærret!");
                            window.location.reload();
                        }
                    });
                }
            }
        )
    }

    this.unlockAccount = function (obj) {
        let id = $(obj).data("id");
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
        }, function(newDate) {
            if (newDate) {
                $.ajax({
                    method: "POST",
                    url: "/rest/employee/unlock/",
                    headers: {
                        "content-type": "application/json",
                        'X-CSRF-TOKEN': token
                    },
                    data: JSON.stringify({
                        "id": id,
                        "stopDate": newDate
                    }),
                    success: function() {
                        toastr.success("Medarbejder genåbnet");
                        window.location.reload();
                    }
                });
            }
        });
    }

    this.extendAccount = function (obj) {
        let id = $(obj).data("id");
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
        }, function(newDate) {
            if (newDate) {
                $.ajax({
                    method: "POST",
                    url: "/rest/employee/extend/",
                    headers: {
                        "content-type": "application/json",
                        'X-CSRF-TOKEN': token
                    },
                    data: JSON.stringify({
                        "id": id,
                        "stopDate": newDate
                    }),
                    success: function() {
                        console.log(newDate);
                        toastr.success("Medarbejder forlænget");
                        window.location.reload();
                    }
                });
            }
        });
    }

    this.editEmployee = function (obj) {
        let id = $(obj).data("id");
        window.location.href = "/employee/edit/" + id;
    }

    this.saveEditedEmployee = function (obj) {
        let id = $(obj).data("id");

        let name = $('#editEmployeeName').val();
        let email = $('#editEmployeeMail').val();
        let phone = $('#editEmployeePhone').val();
        let business = $('#editBusinessSelect').find("option:selected").val();
        let startDate = formatDateForBackend($('#editStartDate').datepicker('getDate'));
        let stopDate = formatDateForBackend($('#editStopDate').datepicker('getDate'));
        let position = $('#editEmployeePosition').val();

        if (startDate && stopDate) {

            if (startDate > stopDate) {
                toastr.warning("Start dato må ikke være efter stop dato!");
                return;
            }
        }

        // Hack, instead of flagging something we just give some random input ourselves
        let readyToPost = employeeService.verifyInput(name, email, startDate, stopDate, position, business, "placeholder", "placeholder");

        if (!readyToPost) {
            return;
        }
        let data = {
            "name": name,
            "email": email,
            "phone": phone,
            "businessId": business,
            "startDate": startDate,
            "stopDate": stopDate,
            "position": position,
            "roles": rolesToAdd,
            "groups": groupsToAdd
        }

        $.ajax({
            method : "PATCH",
            url: "/rest/employee/edit/" + id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Medarbejder er opdateret");

                window.location.href = window.location.origin + "/employees/overview";
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en fejl");
            }
        });
    }

    this.verifyInput = function (name, email, startDate, stopDate, position, business, cpr, uuid) {
        if (business === "0") {
            toastr.warning("Du mangler at vælge en virksomhed");
            return false;
        }
        if (!name) {
            toastr.warning("Du mangler at indtaste et navn");
            return false;
        }
        if (!email) {
            toastr.warning("Du mangler at indtaste en mailadresse");
            return false;
        }
        if (!startDate) {
            toastr.warning("Du mangler at indtaste en start dato");
            return false;
        }
        if (!stopDate) {
            toastr.warning("Du mangler at indtaste en stop dato");
            return false;
        }
        if (!position) {
            toastr.warning("Du mangler at indtaste en stilling!");
            return false;
        }
        if (startDate > stopDate) {
            toastr.warning("Stop dato må ikke være før start dato");
            return false;
        }
        if (!cpr && !uuid) {
            toastr.warning("Du mangler at indtaste en CPR eller en UUID");
            return false
        }
        return true;
    }

    this.saveCreatedEmployee = function () {
        let name = $('#createEmployeeName').val();
        let email = $('#createEmployeeMail').val();
        let phone = $('#createEmployeePhone').val();
        let cpr = $('#createEmployeeCpr').val();
        let uuid = $('#createEmployeeUuid').val();
        let business = $('#businessSelect').find("option:selected").val();
        let startDate = $('#startDate').val();
        let stopDate = $('#stopDate').val();
        let position = $('#createEmployeePos').val();

        if (startDate.val() && stopDate.val()) {
            let startDateValue = formatDateForBackend(startDate.datepicker('getDate'));
            let stopDateValue = formatDateForBackend(stopDate.datepicker('getDate'));

            if (startDateValue > stopDateValue) {
                toastr.warning("Start dato må ikke være efter stop dato!");
                return;
            }
        }

        let readyToPost = employeeService.verifyInput(name, email, startDate, stopDate, position, business, cpr);

        if (!readyToPost) {
            return;
        }

        let data = {
            "name": name,
            "email": email,
            "phone": phone,
            "cpr": cpr,
            "uuid": uuid,
            "businessId": business,
            "startDate": startDate,
            "stopDate": stopDate,
            "position": position
        }

        $.ajax({
            method : "POST",
            url: "/rest/employee/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Medarbejder er oprettet");

                window.location.href = window.location.origin + "/employees/overview";
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en fejl");
            }
        });
    }

    this.openDetails = function (obj) {
        let id = $(obj).data("id");
        window.location.href = "/employee/details/" + id;
    }

    this.openRoleModal = function () {
        employeeService.initRoleTable();
        $('#addRoleModal').modal("show");
        for (let i = 0; i < rolesToAdd.length; i++) {
            $('#roleChoice' + rolesToAdd[i]).prop("checked", true);
        }
    }

    this.closeModal = function () {
        for (let i = 0; i < rolesToAdd.length; i++) {
            $('#roleChoice' + rolesToAdd[i]).iCheck('uncheck');
        }
        rolesToAdd = [];
        $('#addRoleModal').modal('hide');
    }

    this.closeModalAccept = function () {
        toastr.success("Du har tilføjet roller, husk at gemme før du forlader siden");
        $('#addRoleModal').modal("hide");
    }

    this.updateRole = function (obj) {
        let id = $(obj).data("id");
        if ($('#roleChoice' + id).prop("checked")) {
            if (!rolesToAdd.includes(id)) {
                rolesToAdd.push(id);
            }
        }
    }

    this.openGroupModal = function () {
        employeeService.initGroupTable();
        $('#addGroupModal').modal("show");
        for (let i = 0; i < groupsToAdd.length; i++) {
            $('#groupChoice' + groupsToAdd[i]).prop("checked", true);
        }
    }

    this.closeGroupModal = function () {
        for (let i = 0; i < groupsToAdd.length; i++) {
            $('#groupChoice' + groupsToAdd[i]).iCheck('uncheck');
        }
        groupsToAdd = [];
        $('#addGroupModal').modal('hide');
    }

    this.closeGroupModalAccept = function () {
        toastr.success("Du har tilføjet grupper, husk at gemme før du forlader siden");
        $('#addGroupModal').modal("hide");
    }

    this.updateGroup = function (obj) {
        let id = $(obj).data("id");
        if ($('#groupChoice' + id).prop("checked")) {
            if (!groupsToAdd.includes(id)) {
                groupsToAdd.push(id);
            }
        }
    }

    this.deleteGroup = function (obj) {
        let id = $(obj).data("id");
        let employeeId = $(obj).data("con");

        let data = {
            "employeeId": employeeId
        }

        $.ajax({
            method : "POST",
            url: "/rest/employee/group/delete/" + id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Du har slettet medarbejderen fra gruppen");
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en fejl");
            }
        });
    }

    this.deleteRole = function (obj) {
        let id = $(obj).data("id");
        let employeeId = $(obj).data("con");

        let data = {
            "employeeId": employeeId
        }

        $.ajax({
            method : "POST",
            url: "/rest/employee/role/delete/" + id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Du har slettet en rolle fra medarbejderen");
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en fejl");
            }
        });
    }
}

function formatDateForBackend(dateObject) {
    return dateObject.getFullYear() + "-" +
        String(dateObject.getMonth() + 1).padStart(2, "0") + '-' +
        String(dateObject.getDate()).padStart(2, "0");
}
