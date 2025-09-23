var token = $("meta[name='_csrf']").attr("content");
var profileService;

$(document).ready(function() {
    profileService = new ProfileService();
    profileService.init();
});

function ProfileService() {
    this.init = function () {
        $('#saveEditProfile').hide();
        $('#cancelEditProfile').hide();
    }

    this.enableEditProfile = function (obj) {
        $('#nameInput').attr("readonly", false);
        $('#phoneInput').attr("readonly", false);
        $('#enableEditProfile').hide();
        $('#saveEditProfile').show();
        $('#cancelEditProfile').show();
    }

    this.disableEditProfile = function (obj) {
        $('#nameInput').attr("readonly", true);
        $('#phoneInput').attr("readonly", true);
        $('#enableEditProfile').show();
        $('#saveEditProfile').hide();
        $('#cancelEditProfile').hide();
    }

    this.saveEditProfile = function (obj) {
        let id = $(obj).data("id");
        let name = $('#nameInput').val();
        let phone = $('#phoneInput').val();

        if (!name) {
            toastr.warning("Du mangler at indtaste dit navn");
            return;
        }
        let data = {
            id: id,
            name: name,
            phoneNumber: phone
        }

        $.ajax({
            method : "POST",
            url: "/rest/profile/edit/profile",
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            data: JSON.stringify(data),
            success: function(data, textStatus, jqXHR){
                toastr.success("ændringerne er blevet gemt");
                setTimeout(function() {
                    window.location.reload();
                }, 1000)
            }
        })
    }

    this.resetPassword = function (obj) {
        let id = $(obj).data("id");
        if (!id) {
            toastr.warning("Der opstod en fejl");
            return;
        }
        $.ajax({
            method : "POST",
            url: "/rest/profile/resetPassword/" + id,
            headers: {
                "content-type": "application/json",
                'X-CSRF-TOKEN': token
            },
            success: function(data, textStatus, jqXHR){
                toastr.success("Nulstillet kodeord!");
                setTimeout(function() {
                    window.location.reload();
                }, 1000)
            }
        })
    }

    this.requestExtension = function (obj) {
        let id = $(obj).data("id");
        if (!id) {
            toastr.warning("Der opstod en fejl");
            return;
        }
        swal({
                title: 'Anmod',
                text: "er du sikker?",
                showCancelButton: true,
                confirmButtonColor: "red",
                confirmButtonText: "Ja",
                cancelButtonText: "Nej",
                closeOnConfirm: true,
                closeOnCancel: true
            },
            function(isConfirm) {
                if (isConfirm) {
                    $.ajax({
                        method : "POST",
                        url: "/rest/profile/requestExtension/" + id,
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        success: function(data, textStatus, jqXHR){
                            toastr.success("Anmodning om forlængelse sendt!");
                            setTimeout(function() {
                                window.location.reload();
                            }, 1000)
                        }
                    })
                }
            }
        )
    }
}