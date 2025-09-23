var token = $("meta[name='_csrf']").attr("content");
var registrationService;

$(document).ready(function () {
    registrationService = new RegistrationService();
    if (!window.location.href.includes("acceptTos")) {
        registrationService.init();
        registrationService.openTosModal();
    }
    registrationService.toggleCvrFields();
    registrationService.onCvrSearch();
});

function RegistrationService() {

    this.init = function () {
        $('.i-checks').iCheck({
            checkboxClass: 'icheckbox_square-green',
            radioClass: 'iradio_square-green',
        });
    }

    this.onCvrSearch = function () {
        const $cvrInput = $('.searchCvr');

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
                        registrationService.handleExistingCvr($cvrInput, data.name);
                    } else {
                        registrationService.handleNotExistingCvr($cvrInput);
                    }
                },
            });
        }
    }

    this.handleNotExistingCvr = function ($cvrInput) {
        $cvrInput.addClass('is-invalid');
        let businessName = $('#businessName');
        businessName.text("Det Cvr-nummer du har indtastet findes ikke.");
        businessName.css('display', 'block');

        registrationService.showMessage(
            'Cvr-nummer er ugyldigt og findes ikke i systemet.',
            'danger'
        );
    }

    this.handleExistingCvr = function ($cvrInput, name) {
        $cvrInput.addClass('is-valid');
        let businessName = $('#businessName');
        businessName.text(name);
        businessName.css('display', 'block');

        registrationService.showMessage(
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

    this.toggleCvrFields = function () {
        const cvrFields = $('#cvrInputFields');
        if ($('#hasCvr').is(':checked')) {
            cvrFields.slideDown(200);
            cvrFields.css('display', 'block');
        } else {
            cvrFields.slideUp(200);
            cvrFields.css('display', 'none');
            $('.searchCvr').val('');
            $('#businessName').val('');
            $('#businessNameContainer').hide();
        }
    }

    this.sendRegistration = function () {
        let name = $('#requestName').val();
        let mail = $('#requestMail').val();
        let leaderMail = $('#requestLeaderMail').val();
        let phone = $('#requestPhone').val();
        let position = $('#positionName').val();
        let cvr = $('.searchCvr').val();
        let hasCvr = $('#hasCvr').is(':checked');
        let acceptedTos = $('#acceptedTOS').is(':checked');
        let businessName = $('#businessName').text();

        if (!name) {
            toastr.warning("Du skal angive navn");
            return;
        }
        if (!mail) {
            toastr.warning("Du skal angive en mailadresse");
            return;
        }
        if (!leaderMail) {
            toastr.warning("Du skal angive din leder's mailadresse");
            return;
        }
        if (!position) {
            toastr.warning("Du skal angive en stilling");
            return;
        }
        if (!cvr && hasCvr) {
            toastr.warning("Du skal angive et CVR nummer");
            return;
        }
        if (!acceptedTos) {
            toastr.warning("Du skal godkende vilkår og privatlivspolitik inden du kan registrere");
            return;
        }

        let data = {
            "name": name,
            "mail": mail,
            "leaderMail": leaderMail,
            "phone": phone,
            "cvr": cvr,
            "hasCvr": hasCvr,
            "position": position,
            "acceptedTos": acceptedTos,
            "businessName": businessName
        }

        $.ajax({
            url: "/rest/register/employee",
            headers: {
                'X-CSRF-TOKEN': token
            },
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function (data, textStatus, jQxhr) {
                window.location.href = "/registration/awaitingApproval/" + data;
            },
        })
    }

    this.openTosModal = function () {
        $('#tosModal').modal({
            backdrop: 'static',
            keyboard: false
        });
        $('#acceptedTOS').prop("disabled", true);
        $('#acceptedTOS').iCheck('uncheck');

        var content = $('#iboxcontent');

        // Reset scrollbar
        content.scrollTop(0);

        // Immediately enable checkbox if no scrolling is needed
        if (content[0].scrollHeight <= content.outerHeight()) {
            $('#acceptedTOS').prop("disabled", false);
        }

        content.on("scroll", function () {
            const scrollElement = content[0];
            const scrollBottom = scrollElement.scrollHeight - scrollElement.scrollTop;
            const isAtBottom = scrollBottom <= scrollElement.clientHeight + 5; // 5px buffer

            if (isAtBottom) {
                $('#acceptedTOS').prop("disabled", false);
            }
        });

        $('#tosModal').modal("show");
    }

    this.closeTosModal = function () {
        setTimeout(() => {
            if ($('#acceptedTOS').prop("checked") == true) {
                toastr.success("Vilkår godkendt!");
                $('#tosModal').modal("hide");
            }
        }, 200)
    }

    this.acceptTerms = function () {
        let acceptedTos = $('#acceptTerms').is(':checked');

        if (!acceptedTos) {
            toastr.warning("Du skal godkende vilkår og privatlivspolitik inden du kan registrere");
            return;
        }

        $.ajax({
            url: "/rest/register/tos",
            headers: {
                'X-CSRF-TOKEN': token
            },
            type: 'POST',
            contentType: 'application/json',
            success: function (data, textStatus, jQxhr) {
                window.location.href = window.location.origin + "/employee/my/account/" + data;
            },
        })
    }
}
