import verifyInputs from "../fragments/verifyInputService.js";

var token = $("meta[name='_csrf']").attr("content");
let registrationService;

$(document).ready(function () {
    $('[data-toggle="tooltip"]').tooltip();
    init();
});

async function init() {
    registrationService = new RegistrationService();
    if (!location.href.includes("acceptTos")) {
        registrationService.init();
        registrationService.openTosModal();
    }
    registrationService.toggleCvrFields();
    registrationService.onCvrSearch();

    initRegistrationButtons();
}

function RegistrationService() {

    this.init = function () {
        $('.i-checks').iCheck({
            checkboxClass: 'icheckbox_square-green',
            radioClass: 'iradio_square-green',
        });
    }

    this.onCvrSearch = async function () {
        const cvrInput = $('.searchCvr');

        // Clear previous validation states
        cvrInput.removeClass('is-invalid is-valid');

        if (cvrInput.val().length > 1) {
            try {
                const url = `/rest/search/${cvrInput.val()}`;
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        "content-type": "application/json",
                        'X-CSRF-TOKEN': token
                    }
                });

                if (!response.ok) {
                    throw new Error("Der opstod en teknisk fejl");
                }

                const data = await response.json();
                const leaderFlow = $('.leaderFlow');
                if (data?.cvr) {
                    registrationService.handleExistingCvr(cvrInput, data.name);
                    if (data.leaderEnabled) {
                        leaderFlow.removeAttr('hidden');
                        leaderFlow.data('leader-enabled', true);
                    } else {
                        leaderFlow.attr('hidden', '');
                        leaderFlow.data('leader-enabled', false);
                    }
                } else {
                    registrationService.handleNotExistingCvr(cvrInput);
                }

            } catch (error) {
                console.error(error);
                toastr.warning(error.message);
            }
        }
    }

    this.handleNotExistingCvr = function ($cvrInput) {
        $cvrInput.addClass('is-invalid');
        let businessName = $('#businessName');
        businessName.text("Det Cvr-nummer du har indtastet findes ikke.");
        businessName.removeAttr('hidden');

        registrationService.showMessage(
            'Cvr-nummer er ugyldigt og findes ikke i systemet.',
            'danger'
        );
    }

    this.handleExistingCvr = function ($cvrInput, name) {
        $cvrInput.addClass('is-valid');
        let businessName = $('#businessName');
        businessName.text(name);
        businessName.removeAttr('hidden');

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
            $('#businessName').attr('hidden', '');
        }
    }

    this.sendRegistration = async function () {
        let name = $('#requestName').val();
        let mail = $('#requestMail').val();
        let leaderMail = $('#requestLeaderMail').val();
        let phone = $('#requestPhone').val() || null;
        let position = $('#positionName').val();
        let cvr = $('.searchCvr').val();
        let hasCvr = $('#hasCvr').is(':checked');
        let acceptedTos = $('#acceptedTOS').is(':checked');
        let businessName = $('#businessName').text();
        let reason = $('#requestReason').val();

        let leaderFlowEnabled = $('.leaderFlow').data('leader-enabled');
        let phoneRequired = $('#requestPhone').data('required');
        let reasonRequired = $('#requestReason').data('required');

        let isValid = verifyInputs([
            {value: name, message: "Du skal angive et navn"},
            {value: mail, message: "Du skal angive en mailadresse"},
            {value: position, message: "Du skal angive en stilling"},
            {value: acceptedTos, message: "Du skal godkende vilkår og privatlivspolitik inden du kan registrere"},
        ]);

        if (leaderFlowEnabled) {
            isValid = isValid && verifyInputs([
                {value: leaderMail, message: "Du skal angive din leder's mailadresse"}
            ]);
        } else if (hasCvr) {
            isValid = isValid && verifyInputs([
                {value: cvr, message: "Du skal angive et CVR nummer"}
            ]);
        }

        if (phoneRequired) {
            isValid = isValid && verifyInputs([
                {value: phone, message: "Du skal angive et telefonnummer"}
            ]);
        }

        if (reasonRequired) {
            isValid = isValid && verifyInputs([
                {value: reason, message: "Du skal angive en registreringsårsag"}
            ]);
        }

        if (!isValid) {
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
            "businessName": businessName,
            "reason": reason
        }

        try {
            const url = '/rest/register/employee';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'content-type': 'application/json',
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const id = await response.json();
            location.href = `/registration/awaitingApproval/${id}`;
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.openTosModal = function () {
        $('#tosModal').modal({
            backdrop: 'static',
            keyboard: false
        });
        $('#acceptedTOS').prop("disabled", true);
        $('#acceptedTOS').iCheck('uncheck');

        let content = $('#iboxcontent');

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
            if ($('#acceptedTOS').prop("checked") === true) {
                toastr.success("Vilkår godkendt!");
                $('#tosModal').modal("hide");
            }
        }, 200)
    }

    this.acceptTerms = async function () {
        let acceptedTos = $('#acceptTerms').is(':checked');

        const isValid = verifyInputs([
            {value: acceptedTos, message: "Du skal godkende vilkår og privatlivspolitik inden du kan registrere"}
        ])

        if (!isValid) {
            return;
        }

        try {
            const url = '/rest/register/tos';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'content-type': 'application/json',
                    'X-CSRF-TOKEN': token
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const id = await response.json();
            location.href = `/employee/my/account/${id}`;
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }
}

function initRegistrationButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'butRateTypeCreate': {
                registrationService.sendRegistration();
                break;
            }
            case 'openTosModalBtn': {
                registrationService.openTosModal();
                break;
            }
            case 'acceptTerms': {
                registrationService.acceptTerms();
                break;
            }
        }
    });

    $(document).on('change', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'acceptedTOS': {
                registrationService.closeTosModal();
                break;
            }
            case 'hasCvr': {
                registrationService.toggleCvrFields();
                break;
            }
            case 'searchCvr': {
                registrationService.onCvrSearch();
                break;
            }
        }
    })
}
