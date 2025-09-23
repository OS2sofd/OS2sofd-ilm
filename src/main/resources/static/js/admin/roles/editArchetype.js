var token = $("meta[name='_csrf']").attr("content");
var editArchetypeService;

$(document).ready(function() {
    editArchetypeService = new EditArchetypeService();
    editArchetypeService.initRoleTable();
    editArchetypeService.initGroupTable();
    editArchetypeService.initializeOriginalRoleState();
    editArchetypeService.initializeOriginalGroupState();
});

function EditArchetypeService () {
    let originalRoleIds = [];
    let originalGroupIds = [];

    let selectedRoleIds = [];
    let selectedGroupIds = [];

    let deselectedGroupIds = [];
    let deselectedRoleIds = [];

    this.initializeOriginalRoleState = function() {
        const checkboxes = document.querySelectorAll('#roleTable input[type="checkbox"][data-id]');
        checkboxes.forEach(checkbox => {
            if (checkbox.checked) {
                const roleId = checkbox.getAttribute('data-id');
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
                const groupId = checkbox.getAttribute('data-id');
                if (!originalGroupIds.includes(groupId)) {
                    originalGroupIds.push(groupId);
                }
                if (!selectedGroupIds.includes(groupId)) {
                    selectedGroupIds.push(groupId);
                }
            }
        });
    }

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

    this.saveEditedArchetype = function (obj) {
        let id = $(obj).data("id");
        let name = $('#editArchetypeName').val();

        let rolesToAdd = selectedRoleIds.filter(roleId => !originalRoleIds.includes(roleId));
        let groupsToAdd = selectedGroupIds.filter(groupId => !originalGroupIds.includes(groupId));

        let data = {
            "archetypeId": id,
            "name": name,
            "rolesToAdd": rolesToAdd,
            "groupsToAdd": groupsToAdd,
            "rolesToRemove": deselectedRoleIds,
            "groupsToRemove": deselectedGroupIds
        }
        $.ajax({
            method : "POST",
            url: "/rest/archetype/edit",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR){
                toastr.success("Arketype opdateret");
                window.location.href = window.location.origin + "/archetypes";
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en fejl");
            }
        });
    }

    this.updateRole = function (checkbox) {
        const roleId = checkbox.getAttribute('data-id');
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
        const groupId = checkbox.getAttribute('data-id');
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

    this.editArchetype = function (id) {
        window.location.href = "/archetype/edit/" + id;
    }
}
