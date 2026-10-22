const DATATABLE_LANGUAGE = {
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
};

document.addEventListener('DOMContentLoaded', () => {
    const token = document.querySelector("meta[name='_csrf']")?.getAttribute('content');
    const tableElement = document.getElementById('emailTemplateTable');
    if (!tableElement) {
        console.warn('emailTemplateTable not found - aborting init');
        return;
    }

    const table = $(tableElement).DataTable({
        "bSort": false,
        "paging": false,
        "responsive": true,
        "dom": "<'row'<'col-sm-12'tr>>",
        "language": DATATABLE_LANGUAGE,
        "columnDefs": [
            { width: '45%', targets: 0 },
            { width: '35%', targets: 1 },
            { width: '10%', targets: 2 },
            { width: '10%', targets: 3 }
        ]
    });
    setupInputFilters(table);

    tableElement.addEventListener('click', (e) => {
        const enableBtn = e.target?.closest('.js-template-enable-btn');
        if (enableBtn) {
            setEnabled(enableBtn.dataset.id, true, token);
            return;
        }

        const disableBtn = e.target?.closest('.js-template-disable-btn');
        if (disableBtn) {
            setEnabled(disableBtn.dataset.id, false, token);
        }
    });
});

async function setEnabled(id, enabled, token) {
    try {
        const response = await fetch(`/rest/admin/emailtemplate/${id}/enabled`, {
            method: 'POST',
            headers: {
                'content-type': 'application/json',
                'X-CSRF-TOKEN': token
            },
            body: JSON.stringify({ enabled: enabled })
        });

        if (!response.ok) {
            throw new Error(`Unexpected status ${response.status}`);
        }

        toastr.success(enabled ? 'Mailskabelon aktiveret' : 'Mailskabelon deaktiveret');
        location.reload();
    }
    catch (error) {
        console.error(error);
        toastr.warning('Kunne ikke ændre status på mailskabelonen');
    }
}

function setupInputFilters(table) {
    $('.input-filter', table.table().footer()).each(function () {
        const column = table.column($(this).index());

        $('input, select', this).on('keyup change', function () {
            const val = this.value;
            if ($(this).is('select') && val !== '') {
                column.search('^' + val + '$', true, false).draw();
            }
            else if (column.search() !== val) {
                column.search(val).draw();
            }
        });
    });
}
