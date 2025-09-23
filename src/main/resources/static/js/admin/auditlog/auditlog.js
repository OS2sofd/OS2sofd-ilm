var token = $("meta[name='_csrf']").attr("content");
var auditLogService;

$(document).ready(function() {
    auditLogService = new AuditLogService();
    auditLogService.init();
    auditLogService.loadDetails();
});

function AuditLogService() {

    this.init = function () {
        var auditLogTable = $('#eventlog').DataTable({
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
                { "data": 'tts'},
                { "data": 'performerName'},
                { "data": 'targetName'},
                { "data": 'logAction'},
                { "data": 'message'},
                { "data": 'detailId', "render": function (data, type, row) {
                    if (!row.detailId) {
                        return '';
                    }
                    return '<a data-id="' + row.id + '" onclick="auditLogService.openDetails(this)"><em class="fa fa-search"></em></a>';
                }}
            ]
        });
        $.each($('.input-filter', auditLogTable.table().footer()), function() {
            var column = auditLogTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.downloadReport = function () {
        let mode = $('#auditInterval').val();
        $.ajax({
            method : "POST",
            url: "/rest/admin/download/log/" + mode ,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            xhrFields: {
                responseType: 'blob'
            },
            success: function(data, textStatus, jqXHR) {
                toastr.success("Download startet");
                var blob = new Blob([data], { type: 'application/zip' });
                var link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download = 'Auditlogs.zip';
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Fejl under download af rapport!");
            }
        });
    }

    this.openDetails = function (obj) {
        let id = $(obj).data("id");
        window.location.href = "/admin/auditlog/details/" + id;
    }

    this.loadDetails = function () {

        var txtArea1 = document.getElementById("detailContent");
        if (txtArea1) {
            var editor_one = CodeMirror.fromTextArea(txtArea1, {
                lineNumbers: true,
                matchBrackets: true,
                styleActiveLine: true,
                readOnly: true,
                mode: "application/json",
                json : true
            });
        }
    }

    this.findAssociatedLogs = function (obj) {
        let id = $(obj).data("id");
        window.location.href = "/admin/auditlog/correlated/" + id;
    }

}