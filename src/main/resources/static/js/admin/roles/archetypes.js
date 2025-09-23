var token = $("meta[name='_csrf']").attr("content");
var archetypeService;

$(document).ready(function() {
    archetypeService = new ArchetypeService();
    archetypeService.init();
});

function ArchetypeService () {

    this.init = function () {
        var archetypeTable = $('#archetypeTable').DataTable({
            "serverSide": true,
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "ajax": {
                "url": "/rest/archetype/view",
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
                {"data": 'id', "render": function (data, row, type) {
                    let isAdmin = $('#isAdmin').data('admin');
                    let isLeader = $('#isLeader').data('leader');

                    if (isAdmin || isLeader) {
                        return '<a onclick="archetypeService.editArchetype('+data+')"><em class="fa fa-fw fa-pencil"></em></a>' +
                            '<a onclick="archetypeService.deleteArchetype('+data+')"><em class="fa fa-fw fa-times"></em></a>';
                    }
                    return '';
                    }}
            ]
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

    this.editArchetype = function (id) {
        window.location.href = "/archetype/edit/" + id;
    }

    this.deleteArchetype = function (id) {
        swal({
                title: 'Slet arketype?',
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
                        url: "/rest/archetype/delete/" + id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            toastr.success("Arketype slettet");
                            window.location.reload();
                        }
                    });
                }
            }
        )
    }

}
