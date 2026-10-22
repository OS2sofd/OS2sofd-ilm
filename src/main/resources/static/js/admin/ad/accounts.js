var token = $("meta[name='_csrf']").attr("content");
let accountService;

$(document).ready(function() {
    accountService = new AccountService();
    accountService.init();
});

function AccountService() {
    this.init = function () {
        let accTable = $('#accountTable').DataTable({
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "serverSide": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "ajax": {
                "url": "/rest/admin/adAccounts/view",
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
                {"data": 'name', render: function (data, type, row) {
                    return data;
                }},
                {"data": 'status'},
                {"data": 'startDate'},
                {"data": 'stopDate'},
                {"data": 'adGroups'}
            ]
        });
        $.each($('.input-filter', accTable.table().footer()), function () {
            const column = accTable.column($(this).index());

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

}
