var token = $("meta[name='_csrf']").attr("content");
var cmsService;

$(document).ready(function() {
    cmsService = new CmsService();
    cmsService.init();

    $('#value').on('change', function () {
        const fileName = this.files[0];
        if (fileName) {
            $('.custom-file-upload span').text(fileName.name);
        }
    })
});

function CmsService() {
    this.init = function () {
        var table = $('#cmsTable').DataTable({
            "bSort": false,
            "paging": false,
            "responsive": true,
            "dom": "<'row'<'col-sm-12'tr>>",
            "language": {
                "search":	   "Søg",
                "lengthMenu":   "_MENU_ rækker per side",
                "info":		 "Viser _START_ til _END_ af _TOTAL_ rækker",
                "zeroRecords":  "Ingen data...",
                "infoEmpty":	"Henter data...",
                "infoFiltered": "(ud af _MAX_ rækker)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                }
            }
        });
        $.each($('.input-filter', table.table().footer()), function() {
            var column = table.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
        if (window.location.href.includes('edit')) {
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
    }

    this.delete = function (element) {
        swal({
                title: 'Slet Hjælpetekst',
                text: "er du sikker?",
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
                        url: "/rest/admin/cms/delete/" + $(element).data("id"),
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            toastr.success("CMS slettet");
                            window.location.reload();
                        }
                    });
                }
            }
        )
    }

    this.removeLogo = function() {
        document.getElementById("cmsLogoForm").reset();
        document.getElementById("cmsLogoForm").submit();
    }
}