import verifyInputs from "../../fragments/verifyInputService.js";

var token = $("meta[name='_csrf']").attr("content");
let editActivationService;

$(document).ready(function() {
    editActivationService = new EditActivationService();

    initClockPicker();
    initEditActivationButtons();
});

function EditActivationService() {
    this.updateActivation = async function (id) {
        const name = document.getElementById('editActivationName').value;
        const startTime = document.getElementById('editActivationStart').value;
        const stopTime = document.getElementById('editActivationStop').value;
        const exclude = document.getElementById('editWeekend').checked;

        const isValid = verifyInputs([
            { value: name, message: "Du skal angive et navn"},
            { value: startTime, message: "Du skal angive et start tidspunkt"},
            { value: stopTime, message: "Du skal angive et stop tidspunkt"},
            { value: exclude, message: "Du skal angive om du vil eksludere weekender"}
        ]);

        if (!isValid) {
            return;
        }

        const data = {
            "startTime": startTime,
            "stopTime": stopTime,
            "excludeWeekends": exclude,
            "name": name
        };

        try {
            const url = `/rest/admin/activation/edit/${id}`;
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'content-type': 'application/json',
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                throw new Error("Der opstod en teknisk fejl");
            }

            toastr.success("Aktiveringsperiode opdateret");
            location.href = '/admin/ad/activation';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.cancelButton = function () {
        location.href = '/admin/ad/activation';
    }
}

function initClockPicker() {
    $('.clockpicker').clockpicker({
        autoclose: true
    });
}

function initEditActivationButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                editActivationService.cancelButton();
                break;
            case 'confirm': {
                const id = target.closest('#confirm').dataset.id;
                editActivationService.updateActivation(id);
                break;
            }
        }
    });
}
