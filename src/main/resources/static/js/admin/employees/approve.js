var token = $("meta[name='_csrf']").attr("content");
var approveService;

$(document).ready(function() {
    approveService = new ApproveService();
    approveService.init();

    $('[data-toggle="tooltip"]').tooltip();

    const isAdmin = $('#isAdmin').data('admin');

    if (isAdmin) {
        // Use a rest call instead of inline javascript for security reasons (pre-processing)
        $.ajax({
            method : "POST",
            url: "/rest/admin/orgUnits/tree",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function(allOUs){
                $('#ou-tree').jstree({
                    "core": {
                        "data": allOUs,
                        "themes": {
                            "icons": false
                        },
                        "multiple": false
                    },
                    "search" : {
                        "show_only_matches": true,
                        "search_callback": function(str, node) {
                            return (node.text.toUpperCase().includes(str.toUpperCase()));
                        }
                    },
                    "plugins" : ["search", "wholerow"]
                });

                // searching in the JSTree
                var to = false;
                $('#ou-tree-search').keyup(function() {
                    if (to) {
                        clearTimeout(to);
                    }

                    to = setTimeout(function() {
                        var v = $('#ou-tree-search').val();

                        $('#ou-tree').jstree(true).search(v);
                    }, 400);
                });
            }
        });
    }

    const approveRadio = document.getElementById('approveEmployee');
    const denyRadio = document.getElementById('denyEmployee');

    // Javascript for toggling html elements based on whether Afivs or Godkend is selected
    if (denyRadio) {
        $('#denyEmployee').change(function () {
            if (this.checked) {
                $('.elementDiv').hide();
            }
        });
    }

    if (approveRadio) {
        $('#approveEmployee').change(function () {
            if (this.checked) {
                $('.elementDiv').show();
            }
        });
    }
});

function ApproveService() {
    let archetypeName = [];
    let archetypeToAdd = [];

    this.init = function () {
        var approveTable = $('#approveTable').DataTable({
            "serverSide": true,
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "ajax": {
                "url": "/rest/approve/view",
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
                {"data": 'cpr'},
                {"data": 'cvr'},
                {"data": "approval", "render": function (data, type, row) {
                        if (data === 'AWAITING_APPROVAL') {
                            return 'Afventer godkendelse';
                        }
                        else if (data === 'LEADER_APPROVED') {
                            return 'Leder har godkendt';
                        }
                        else if (data === 'AWAITING_EXTENSION_APPROVAL') {
                            return "Godkendt, men vil anmode om mere tid";
                        }
                    }
                },
                {"data": 'id', "render": function (data, type, row) {
                        let isAdmin = $('#isAdmin').data('admin');
                        let isLeader = $('#isLeader').data('leader');

                        if ((row.approval === 'LEADER_APPROVED' || row.approval === 'AWAITING_APPROVAL') && isAdmin) {
                            return '<a onclick="approveService.sendToAdminApprove('+row.id+')"><em class="fa fa-fw fa-check"></em></a>'
                        }
                        else if (row.approval === 'AWAITING_APPROVAL' && isLeader){
                            return '<a onclick="approveService.approve('+row.id+')"><em class="fa fa-fw fa-check"></em></a>'
                                + '<a onclick="approveService.deny('+row.id+')"><em class="fa fa-fw fa-times"></em></a>';
                        }
                        else if (row.approval === 'AWAITING_EXTENSION_APPROVAL' && (isLeader || isAdmin)) {
                            return '<span><a onclick="approveService.approveExtension('+row.id+')"><em class="fa fa-fw fa-check"></em></a></span><a onclick="approveService.denyExtension('+row.id+')"><em class="fa fa-fw fa-times"></em></a>';
                        }
                        return '';
                    }
                }
            ]
        });
        $.each($('.input-filter', approveTable.table().footer()), function () {
            var column = approveTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.initArchetypeTable = function () {
        var archetypeTable = $('#archetypeTable').DataTable({
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
        $.each($('.input-filter', archetypeTable.table().footer()), function () {
            var column = archetypeTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.initGroupTable = function () {
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

    this.sendToAdminApprove = function (id) {
        window.location.href = "/approval/" + id;
    }

    this.chooseEntity = function () {
        $('#modal-ou').modal("show");
    }

    this.approve = function (id) {
        swal({
                title: 'Godkend medarbejder',
                text: "Er du sikker?",
                showCancelButton: true,
                confirmButtonColor: "green",
                confirmButtonText: "Ja",
                cancelButtonText: "Nej",
                closeOnConfirm: true,
                closeOnCancel: true
            },
            function(isConfirm) {
                if(isConfirm) {
                    $.ajax({
                        method : "POST",
                        url: "/rest/approve/" + id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            toastr.success("Medarbejder godkendt");
                            window.location.reload();
                        }
                    });
                }
            }
        )
    }

    this.deny = function (id) {
        swal({
                title: 'Afvis medarbejder',
                text: "Er du sikker?",
                showCancelButton: true,
                confirmButtonColor: "red",
                confirmButtonText: "Ja",
                cancelButtonText: "Nej",
                closeOnConfirm: true,
                closeOnCancel: true
            },
            function(isConfirm) {
                if(isConfirm) {
                    $.ajax({
                        method : "POST",
                        url: "/rest/deny/" + id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            toastr.success("Medarbejder afvist");
                            window.location.reload();
                        }
                    });
                }
            }
        )
    }

    this.saveOUs = function (){
        var selectedNodes = $('#ou-tree').jstree('get_selected', true);

        $("#orgUnits").empty();

        if (selectedNodes.length === 1) {
            var node = selectedNodes[0];
            $("#orgUnits").append('<option value=\"' + node.id + '\" selected="selected"> ' + node.text + "</option>");
            $('#entity').val(node.text);
            $('#entity').attr('data-uuid', node.id);
        }

        $('#modal-ou').modal('hide');
    }

    this.cancelButton = function () {
        window.location.href = "/approve"
    }

    this.saveApprovedEmployee = function (obj) {
        let id = $(obj).data("id");

        let isApproved = $('#approveEmployee').is(':checked');
        let isDenied = $('#denyEmployee').is(':checked');
        let archetype = $('#archetype').data('id');
        let ou = $('#entity').data('uuid');
        let startDate = $('#startDate').datepicker('getDate');
        let stopDate = $('#stopDate').datepicker('getDate');
        let activationPeriod = $('#activationSelect').find('option:selected').val();
        let businessName = $('#businessName').val();

        if (startDate && stopDate) {
            if (startDate > stopDate) {
                toastr.warning("Start dato må ikke være efter stop dato!");
                return;
            }
        }

        if (!isDenied) {
            let inputVerified = approveService.verifyInput(isApproved, isDenied, archetype, ou, startDate, stopDate, activationPeriod);

            if (!inputVerified) {
                return;
            }
        }

        let data = {
            "approved": isApproved,
            "denied": isDenied,
            "archetype": archetype,
            "ou": ou,
            "startDate": startDate,
            "stopDate": stopDate,
            "activationPeriod": activationPeriod,
            "businessName": businessName
        }
        $.ajax({
            method : "POST",
            url: "/rest/approval/" + id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Medarbejder er godkendt");

                window.location.href = window.location.origin + "/approve";
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en fejl");
            }
        });
    }

    this.chooseArchetype = function () {
        approveService.initArchetypeTable();
        $('#modal-archetype').modal("show");
    }

    this.closeModal = function () {
        $('input[name="archetypeChoice"]').prop('checked', false);
        archetypeToAdd = [];
        archetypeName = [];
        $('#modal-archetype').modal('hide');
    }

    this.updateArchetype = function (obj) {
        let selectedName = $(obj).data("name");
        let id = $(obj).data("id");

        archetypeToAdd = [];
        archetypeName = [];

        if ($(obj).prop('checked')) {
            archetypeName.push(selectedName);
            archetypeToAdd.push(id);
        }
    }

    this.denyExtension = function (id) {
        swal({
                title: 'Afvise forlængelses anmodning',
                text: "er du sikker?",
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
                        url: "/rest/employee/denyExtension/" + id,
                        headers: {
                            "Content-Type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function (data, textStatus, jqXHR) {
                            toastr.success("Forlængelse afvist");
                            window.location.reload();
                        }
                    });
                }
            }
        )
    }

    this.approveExtension = function (id) {
        swal({
            title: 'Forlæng Tildeling',
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
                        toastr.success("Medarbejder forlænget");
                        window.location.reload();
                    }
                });
            }
        });
    }

    this.saveArchetype = function () {
        for (let i = 0; i < archetypeName.length; i++) {
            $('#archetype').val(archetypeName[i]);
        }
        for (let i = 0; i < archetypeToAdd.length; i++) {
            $('#archetype').attr('data-id', archetypeToAdd[i]);
        }
        $('#modal-archetype').modal("hide");
    }

    this.chooseArchetype = function () {
        approveService.initArchetypeTable();
        $('#modal-archetype').modal("show");
    }

    this.verifyInput = function (isApproved, isDenied, archetype, ou, startdate, stopdate, activation) {
        if (isApproved === false && isDenied === false) {
            toastr.warning("Du skal enten godkende ellers afvise medarbejderen");
            return false;
        }
        if (!archetype || archetype === "0") {
            toastr.warning("Du skal angive en arketype for at tildele roller");
            return false;
        }
        if (!ou) {
            toastr.warning("Du skal angive en organisation, som medarbejderen skal tilføjes til");
            return false;
        }
        if (!startDate) {
            toastr.warning("Du skal angive et start dato");
            return false;
        }
        if (!stopdate) {
            toastr.warning("Du skal angive et stop dato");
            return false;
        }
        if (!activation || activation === "0") {
            toastr.warning("Du skal angive en aktiveringsperiode");
            return false;
        }
        return true;
    }
}

