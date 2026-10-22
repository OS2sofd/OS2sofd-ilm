import initializeSwal from "../../fragments/swalService.js";

var token = $("meta[name='_csrf']").attr("content");
let cmsService;

$(document).ready(function() {
    cmsService = new CmsService();
    cmsService.init();
    initCmsButtons();

    $('#value').on('change', function () {
        const fileName = this.files[0];
        if (fileName) {
            $('.custom-file-upload span').text(fileName.name);
        }
    });
});

function CmsService() {
    this.init = function () {
        let textTable = $('#cmsTextTable').DataTable({
            "bSort": false,
            "paging": false,
            "responsive": true,
            "dom": "<'row'<'col-sm-12'tr>>",
            "language": {
                "search":      "Søg",
                "lengthMenu":   "_MENU_ rækker per side",
                "info":     "Viser _START_ til _END_ af _TOTAL_ rækker",
                "zeroRecords":  "Ingen data...",
                "infoEmpty":    "Henter data...",
                "infoFiltered": "(ud af _MAX_ rækker)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                }
            },
            "columnDefs": [
                { width: '45%', targets: 0},
                { width: '40%', targets: 1},
                { width: '10%', targets: 2},
                { width: '5%', targets: 3},
            ]
        });

        setupInputFilters(textTable);

        if (globalThis.location.href.includes('edit')) {
            if ($('#key').val() !== 'cms.logo' && !$('#key').val().startsWith("cms.simple")) {
                $('#content').summernote({
                    "height": 320,
                    "toolbar": [
                        [ "font", [ "bold", "italic", "underline" ]],
                        [ "para", [ "ul", "ol", "style" ]],
                        [ "insert", ["link"]]
                    ]
                });
            }
        }

        // Apply the default "AKTIV" filter on both tables
        $('.input-filter select').trigger('change');
    }

    this.delete = function (id) {
        const params = {
            title: 'Slet Hjælpetekst',
            text: 'Er du sikker?',
            id: id,
            token: token,
            success: 'CMS slettet',
            warning: 'Kunne ikke slette CMS',
            url: '/rest/admin/cms/delete/'
        };
        initializeSwal(params);
    }

    this.setActive = async function (element, value) {

        const data = {
            key: $(element).data("key"),
            text: $(element).data("text"),
            value: value
        }

        try {
            const url = '/rest/admin/cms/enable';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                throw new Error("Der opstod en teknisk fejl");
            }

            toastr.success("CMS aktiveret")
            location.reload();
        } catch (error) {
            console.error(error);
            toastr.warning("Kunne ikke aktivere cms indstillingen")
        }
    }

    this.removeLogo = function() {
        document.getElementById("cmsLogoForm").reset();
        document.getElementById("cmsLogoForm").submit();
    }
}

function setupInputFilters(table) {
    $.each($('.input-filter', table.table().footer()), function() {
        const column = table.column($(this).index());

        $('input, select', this).on('keyup change', function () {
            const val = this.value;
            if ($(this).is('select') && val !== '') {
                column.search('^' + val + '$', true, false).draw();
            } else {
                if (column.search() !== val) {
                    column.search(val).draw();
                }
            }
        });
    });
}

function initCmsButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'removeLogoBtn':
                cmsService.removeLogo();
                break;
        }

        if (target.classList.contains('cmsDeleteBtn') || target.closest('.cmsDeleteBtn')) {
            const btn = target.closest('.cmsDeleteBtn');
            const id = btn?.dataset.id;
            cmsService.delete(id);
        } else if (target.classList.contains('cmsActivateBtn') || target.closest('.cmsActivateBtn')) {
            const btn = target.closest('.cmsActivateBtn');
            cmsService.setActive(btn, true);
        } else if (target.classList.contains('cmsDeactivateBtn') || target.closest('.cmsDeactivateBtn')) {
            const btn = target.closest('.cmsDeactivateBtn');
            cmsService.setActive(btn, false);
        }
    })
}
