var token = $("meta[name='_csrf']").attr("content");
var createArchetypeService;

$(document).ready(function() {
    createArchetypeService = new CreateArchetypeService();
    createArchetypeService.initGroupTable();
    createArchetypeService.initRoleTable();

    document.querySelectorAll('#groupTable input[type="checkbox"]').forEach(checkbox => {

    })

});

function CreateArchetypeService() {
    let selectedRoleIds = [];
    let selectedGroupIds = [];

    this.initRoleTable = function () {
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

    this.cancelButton = function () {
        window.location.href = "/archetypes";
    }

    this.saveCreatedArchetype = function () {
        let name = $('#createEmployeeName').val();

        let data = {
            "name": name,
            "roles": selectedRoleIds,
            "groups": selectedGroupIds
        }

        if (!name) {
            toastr.warning("Der skal angives et navn");
            return;
        }
        if (selectedRoleIds.length === 0 && selectedGroupIds.length === 0) {
            toastr.warning("Der skal enten tilføjes en rolle eller gruppe");
            return
        }

        $.ajax({
            method : "POST",
            url: "/rest/archetype/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR){
                window.location.href = "/archetypes";
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en teknisk fejl");
            }
        });
    }

    this.updateGroup = function (checkbox) {
        const groupId = checkbox.getAttribute('data-id');
        if (checkbox.checked) {
            // Add ID if not already present
            if (!selectedGroupIds.includes(groupId)) {
                selectedGroupIds.push(groupId);
            } else {
                selectedGroupIds = selectedGroupIds.filter(id => id !== groupId);
            }
        }
    }

    this.updateRole = function (checkbox) {
        const roleId = checkbox.getAttribute('data-id');
        if (checkbox.checked) {
            // Add ID if not already present
            if (!selectedRoleIds.includes(roleId)) {
                selectedRoleIds.push(roleId);
            } else {
                selectedRoleIds = selectedRoleIds.filter(id => id !== roleId);
            }
        }
    }
}
