var token = $("meta[name='_csrf']").attr("content");
var editActivationService;

$(document).ready(function() {
    editActivationService = new EditActivationService();

    $('.clockpicker').clockpicker({
        autoclose: true
    });
});

function EditActivationService() {
    this.updateActivation = function (obj) {
        let id = $(obj).data('id');

        let name = $('#editActivationName').val();
        let starttime = $('#editActivationStart').val();
        let stoptime = $('#editActivationStop').val();
        let exclude = $('#editWeekend').is(':checked');

        console.log(exclude);

        let verify = editActivationService.verifyInput(starttime, stoptime, exclude, name);

        if (!verify) {
            return;
        }

        let data = {
            startTime: starttime,
            stopTime: stoptime,
            excludeWeekends: exclude,
            name: name
        }

        $.ajax({
            method : "POST",
            url: "/rest/admin/activation/edit/" + id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR) {
                toastr.success("Aktiveringsperiode opdateret");

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