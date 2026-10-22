import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let auditLogService;

$(document).ready(function() {
    auditLogService = new AuditLogService();
    const evenLog = $('#eventLog');
    if (evenLog) {
        auditLogService.initEventLog();
    }

    const correlatedTable = $('#correlatedTable');
    if (correlatedTable) {
        auditLogService.initCorrelatedTable();
    }

    auditLogService.loadDetails();
    initAuditLogButtons();
});

function AuditLogService() {
    this.initEventLog = function () {
        let auditLogTable = $('#eventLog').DataTable({
            "serverSide": true,
            "destroy": true,
            "order": [[0, 'desc']],
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "ajax": {
                "url": "/rest/admin/auditlog/view",
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
                { "data": 'tts', 'render': function (data, type, row) {
                    const [datePart, timePart] = data.split('T');
                    const [year, month, day] = datePart.split('-');

                    return `${day}/${month}/${year} ${timePart}`;
                }},
                { "data": 'performerName'},
                { "data": 'targetName'},
                { "data": 'logAction'},
                { "data": 'message'},
                { "data": 'detailId', "render": function (data, type, row) {
                    if (!row.detailId) {
                        return '';
                    }
                    return '<a data-id="' + row.id + '" class="openDetailsBtn"><em class="fa fa-search"></em></a>';
                }}
            ],
            'columnDefs': [
                {
                    'targets': 5,
                    'orderable': false,
                }
            ]
        });
        $.each($('.input-filter', auditLogTable.table().footer()), function() {
            const column = auditLogTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.initCorrelatedTable = function () {
        initializeDataTable('#correlatedTable', token, {
            'columnDefs': [{
                'targets': 4,
                'orderable': false
            }]
        });
    }

    this.downloadReport = async function () {
        let mode = $('#auditInterval').val();
        try {
            const url = `/rest/admin/download/log/${mode}`;
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const blob = await response.blob();
            toastr.success("Download startet");

            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = 'Auditlogs.zip';
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(link.href);
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.openDetails = function (id) {
        location.href = `/admin/auditlog/details/${id}`;
    }

    this.loadDetails = function () {
        const txtArea1 = document.getElementById("detailContent");
        if (txtArea1) {
            let editor_one = CodeMirror.fromTextArea(txtArea1, {
                lineNumbers: true,
                matchBrackets: true,
                styleActiveLine: true,
                readOnly: true,
                mode: "application/json",
                json : true
            });
        }
    }

    this.findAssociatedLogs = function (id) {
        globalThis.location.href = "/admin/auditlog/correlated/" + id;
    }
}

function initAuditLogButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'downloadReport':
                auditLogService.downloadReport()
                break;
            case 'findAssociatedLogsBtn': {
                const id = target.closest('#findAssociatedLogsBtn').dataset.id;
                auditLogService.findAssociatedLogs(id);
            }
        }

        if (target.classList.contains('openDetailsBtn') || target.closest('.openDetailsBtn')) {
            const id = target.closest('.openDetailsBtn').dataset.id;
            auditLogService.openDetails(id);
        }
    })
}
