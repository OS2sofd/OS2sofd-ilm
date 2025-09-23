var token = $("meta[name='_csrf']").attr("content");
var adminEmployeeService;

$(document).ready(function() {
    adminEmployeeService = new AdminEmployeeService();

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
});

function AdminEmployeeService() {
    let archetypeName = [];
    let archetypeToAdd = [];

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
        window.location.href = window.location.origin + "/employees/overview";
    }

    this.saveCreatedEmployee = function () {
        let name = $('#createEmployeeName').val();
        let email = $('#createEmployeeMail').val();
        let phone = $('#createEmployeePhone').val();
        let cpr = $('#createEmployeeCpr').val();
        let uuid = $('#createEmployeeUuid').val();
        let business = $('#businessSelect').find("option:selected").val();
        let startDate = $('#startDate').datepicker('getDate');
        let stopDate = $('#stopDate').datepicker('getDate');
        let position = $('#createEmployeePos').val();
        let archetype = $('#archetype').data('id');
        let ou = $('#entity').data('uuid');
        let activation = $('#activationSelect').find("option:selected").val();

        if (startDate && stopDate) {
            if (startDate > stopDate) {
                toastr.warning("Start dato må ikke være efter stop dato!");
                return;
            }
        }

        let readyToPost = adminEmployeeService.verifyInput(name, email, startDate, stopDate, position, business, cpr, uuid, archetype, ou, activation);

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
            "position": position,
            "archetype": archetype,
            "orgUnit": ou,
            "activationId": activation
        }

        $.ajax({
            method : "POST",
            url: "/rest/employee/admin/create",
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

    this.verifyInput = function (name, email, startDate, stopDate, position, business, cpr, uuid, archetype, ou, activation) {
        if (business === "0") {
            toastr.warning("Du mangler at vælge en virksomhed");
            return false;
        }
        if (activation === "0") {
            toastr.warning("Du mangler at vælge en aktiveringsperiode");
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
        if (archetype === null) {
            toastr.warning("Du mangler at vælge en arketype")
        }
        if (ou === null) {
            toastr.warning("Du mangler at vælge en organisation")
        }
        return true;
    }

    this.chooseArchetype = function () {
        adminEmployeeService.initArchetypeTable();
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

    this.saveArchetype = function () {
        for (let i = 0; i < archetypeName.length; i++) {
            $('#archetype').val(archetypeName[i]);
        }
        for (let i = 0; i < archetypeToAdd.length; i++) {
            $('#archetype').attr('data-id', archetypeToAdd[i]);
        }
        $('#modal-archetype').modal("hide");
    }

    this.chooseEntity = function () {
        $('#modal-ou').modal("show");
    }
}
