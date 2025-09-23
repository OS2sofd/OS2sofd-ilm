var token = $("meta[name='_csrf']").attr("content");
var businessService;

$(document).ready(function() {
    businessService = new BusinessService();
    businessService.init();
    businessService.affiliatedTable();

    $('[data-toggle="tooltip"]').tooltip();
});

function BusinessService() {

    this.init = function () {
        var businessTable = $('#businessTable').DataTable({
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
        $.each($('.input-filter', businessTable.table().footer()), function () {
            var column = businessTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });
    }

    this.affiliatedTable = function () {
        var affiliatedTable = $('#affiliatedTable').DataTable({
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
        $.each($('.input-filter', affiliatedTable.table().footer()), function () {
            var column = affiliatedTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
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

    this.editAgency = function (obj) {
        let id = $(obj).data("id");
        window.location.href = "/business/edit/" + id;
    }

    this.saveEditedAgency = function (obj) {
        let id = $(obj).data("id");

        var name = $('#agencyName').val();
        var prefix = $('#agencyPrefix').val();

        let data = {
            name: name,
            prefix: prefix
        }

        $.ajax({
            method : "POST",
            url: "/rest/business/edit/" + id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Virksomhed opdateret");

                window.location.href = window.location.origin + "/business/overview";
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en fejl");
            }
        });
    }

    this.deleteAgency = function (obj) {
        let id = $(obj).data('id');

        swal({
                title: 'Slet virksomhed?',
                text: "Er du sikkert? Det vil slette alle tilhørende medarbejder.",
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
                        url: "/rest/business/delete/" + id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR) {
                            toastr.success("Virksomheden er slettet");

                            window.location.href = window.location.origin + "/business/overview";
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            toastr.warning("Der opstod en fejl");
                        }
                    });
                }
            }
        )
    }

    this.cancelButton = function () {
        window.location.href = window.location.origin + "/business/overview";
    }

    this.handleNotExistingCvr = function ($cvrInput) {
        $cvrInput.addClass('is-invalid');
        let businessName = $('#businessName');
        businessName.text("Det Cvr-nummer du har indtastet findes ikke.");
        businessName.css('display', 'block');

        businessService.showMessage(
            'Cvr-nummer er ugyldigt og findes ikke i systemet.',
            'danger'
        );
    }

    this.onCvrSearch = function () {
        const $cvrInput = $('.searchCvr');
        if (!$cvrInput.val() || $cvrInput.val().length < 1) {
            $('#cvrSearchResult').text("");
            $('#cvrSearchResult').hide();
        }
        // Clear previous validation states
        $cvrInput.removeClass('is-invalid is-valid');

        if ($cvrInput.val().length > 1) {
            $.ajax({
                url: "/rest/search/" + $cvrInput.val(),
                headers: {
                    'X-CSRF-TOKEN': token
                },
                type: 'GET',
                contentType: 'application/json',
                success: function (data) {
                    if (data && data.cvr) {
                        businessService.handleExistingCvr($cvrInput, data.name);
                    }
                    else {
                        businessService.handleNotExistingCvr($cvrInput);
                    }
                }
            });
        }
    }

    this.handleExistingCvr = function ($cvrInput, name) {
        $cvrInput.addClass('is-valid');
        let businessName = $('#cvrSearchResult');
        businessName.text(name);
        businessName.css('display', 'block');

        businessService.showMessage(
            'CVR nummer er gyldigt og findes i systemet.',
            'success'
        );
    }

    this.showMessage = function(message, type) {
        const $messageContainer = $('#messageContainer');

        $messageContainer
            .removeClass()
            .addClass(`alert alert-${type}`)
            .html(message)
            .fadeIn(200);

        setTimeout(() => {
            $messageContainer.fadeOut(200);
        }, 5000);
    };

    this.createAgency = function () {
        var name = $('#createAgencyName').val();
        var cvr = $('#createAgencyCvr').val();
        var prefix = $('#createAgencyPrefix').val();

        let data = {
            name: name,
            cvr: cvr,
            prefix: prefix
        }

        $.ajax({
            method : "POST",
            url: "/rest/business/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Virksomhed oprettet");

                window.location.href = window.location.origin + "/business/overview";
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning(jqXHR.responseText);
            }
        });
    }

    this.openDetails = function (obj) {
        let id = $(obj).data("id");
        window.location.href = "/business/details/" + id;
    }

}
