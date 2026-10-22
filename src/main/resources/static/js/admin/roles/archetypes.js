import initializeSwal from "../../fragments/swalService.js";

var token = $("meta[name='_csrf']").attr("content");
let archetypeService;

$(document).ready(function() {
    archetypeService = new ArchetypeService();
    archetypeService.init();
    initArchetypeButtons()
});

function ArchetypeService () {

    this.init = function () {
        let archetypeTable = $('#archetypeTable').DataTable({
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
                        return '<a class="editArchetypeBtn" data-id="'+ data +'"><em class="fa fa-fw fa-pencil"></em></a>' +
                            '<a class="deleteArchetypeBtn" data-id="'+ data +'"><em class="fa fa-fw fa-times"></em></a>';
                    }
                    return '';
                    }}
            ],
            'columnDefs': [
                {
                    'targets': 1,
                    'orderable': false
                }
            ]
        });
        $.each($('.input-filter', archetypeTable.table().footer()), function () {
            const column = archetypeTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.editArchetype = function (id) {
        globalThis.location.href = "/archetype/edit/" + id;
    }

    this.deleteArchetype = function (id) {
        const params = {
            title: 'Slet arketype?',
            text: 'Er du sikker?',
            id: id,
            token: token,
            success: 'Arketype slettet',
            warning: 'Kunne ikke slette arketypen',
            url: '/rest/archetype/delete/'
        };
        initializeSwal(params);
    }
}

function initArchetypeButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        if (target.classList.contains('editArchetypeBtn') || target.closest('.editArchetypeBtn')) {
            const id = target.closest('.editArchetypeBtn')?.dataset.id;
            archetypeService.editArchetype(id);
        }
        else if (target.classList.contains('deleteArchetypeBtn') || target.closest('.deleteArchetypeBtn')) {
            const id = target.closest('.deleteArchetypeBtn')?.dataset.id;
            archetypeService.deleteArchetype(id);
        }
    })
}
