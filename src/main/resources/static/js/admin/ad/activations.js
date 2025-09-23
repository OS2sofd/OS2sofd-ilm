var token = $("meta[name='_csrf']").attr("content");
var activationService;

$(document).ready(function() {
    activationService = new ActivationService();
    activationService.init();

    $('.clockpicker').clockpicker({
        autoclose: true,
        twelvehour: false
    });

    $('.time-filter').timepicker({
        timeFormat: 'HH:mm',
        interval: 15,
        minTime: '00:00',
        maxTime: '23:59',
        dynamic: false,
        dropdown: true,
        scrollbar: true
    });
});

function ActivationService() {
    this.init = function () {
        var activationTable = $('#activationTable').DataTable({
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
        $.each($('.input-filter', activationTable.table().footer()), function () {
            const column = activationTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if ($(this).is(':checkbox')) {
                    // Custom search for weekends
                    $.fn.dataTable.ext.search.push(function(settings, data, dataIndex) {
                        const weekendValue = data[3];
                        const isChecked = $('.i-checks').prop('checked');

                        if (!isChecked) {
                            return weekendValue === 'Ikke inkluderet';
                        } else {
                            return weekendValue === 'Inkluderet';
                        }
                    });

                    activationTable.draw();

                    // Remove custom search after drawing
                    $.fn.dataTable.ext.search.pop();
                } else {
                    // Normal input behavior
                    if (column.search() !== this.value) {
                        column.search(this.value).draw();
                    }
                }
                });

            $('.time-filter').on('changeTime', function() {
                const selectedTime = $(this).val();

                // Custom search
                activationTable.column(2).search(selectedTime).draw();
            });
        });
    }

    this.deleteActivation = function (obj) {
        let id = $(obj).data('id');

        swal({
                title: 'Slet Aktiverinsperiode?',
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
                        method : "DELETE",
                        url: "/rest/admin/activation/delete/" + id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR) {
                            toastr.success("Aktiveringsperiode slettet");
                            window.location.href = window.location.origin + "/admin/ad/activation";
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            toastr.warning("Der opstod en fejl");
                        }
                    });
                }
            }
        )
    }

    this.editActivation = function (obj) {
        let id = $(obj).data("id");
        window.location.href = window.location.origin + "/admin/ad/activation/edit/" + id;
    }

    this.createActivation = function () {
        let name = $('#createActivationName').val();
        let startTime = $('#createActivationStart').val();
        let stopTime = $('#createActivationStop').val();
        let exclude = $('#createWeekend').is(':checked');

        let verify = activationService.verifyInput(startTime, stopTime, exclude, name);

        console.log(startTime);

        if (!verify) {
            return;
        }

        let data = {
            startTime: startTime,
            stopTime: stopTime,
            excludeWeekends: exclude,
            name: name
        }

        $.ajax({
            method : "POST",
            url: "/rest/admin/activation/create",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Aktiveringsperiode oprettet");

                window.location.href = window.location.origin + "/admin/ad/activation";
            },
            error: function(jqXHR, textStatus, errorThrown) {
                toastr.warning("Der opstod en fejl");
            }
        });
    }

    this.verifyInput = function (starttime, stoptime, exclude, name) {
        if (name === null) {
            toastr.warning("Du skal angive et navn");
            return false;
        }
        if (starttime === null) {
            taostr.warning("Du skal angive et start tidspunkt");
            return false;
        }
        if (stoptime === null) {
            taostr.warning("Du skal angive et stop tidspunkt");
            return false;
        }
        if (exclude === null) {
            taostr.warning("Du skal angive om du vil eksludere weekender");
            return false;
        }
        return true;
    }

    this.cancelButton = function () {
        window.location.href = "/admin/ad/activation";
    }

}
