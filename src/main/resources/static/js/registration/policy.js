var token = $("meta[name='_csrf']").attr("content");
var policyService;

$(document).ready(function () {
    policyService = new PolicyService();
    policyService.init();
});

function PolicyService() {

    this.init = function () {
        $('.i-checks').iCheck({
            checkboxClass: 'icheckbox_square-green',
            radioClass: 'iradio_square-green',
        });
    }

    this.approved = function (obj) {
        let id = $(obj).attr('data-id');

        if (!$('#acceptedTOS').prop("checked")) {
            toastr.warning("Du skal acceptere de generelle vilkår og privatlivspolitik")
            return;
        }
        var data = {
            "id": id,
            "acceptedTos": true
        }

        $.ajax({
            method : "POST",
            url: "/rest/register/policy",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR){
                window.location.href = window.location.origin + "/employee/my/account/" + id;
            }
        })
    }
}