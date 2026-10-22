var token = $("meta[name='_csrf']").attr("content");
let policyService;

$(document).ready(function () {
    policyService = new PolicyService();
    policyService.init();
    initPolicyButtons();
});

function PolicyService() {

    this.init = function () {
        $('.i-checks').iCheck({
            checkboxClass: 'icheckbox_square-green',
            radioClass: 'iradio_square-green',
        });
    }

    this.acceptTerms = async function (id) {
        if (!document.getElementById('acceptedTOS').checked) {
            toastr.warning("Du skal acceptere de generelle vilkår og privatlivspolitik")
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

            location.href = '/employee/my/account/' + id;
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }
}

function initPolicyButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'acceptTerms': {
                const id = target.closest('#acceptTerms')?.dataset.id;
                policyService.acceptTerms(id);
                break;
            }
        }
    });
}
