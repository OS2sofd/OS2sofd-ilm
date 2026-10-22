import verifyInputs from "../../fragments/verifyInputService.js";
import initializeSwal from "../../fragments/swalService.js";
import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let activationService;

$(document).ready(function() {
    activationService = new ActivationService();
    activationService.init();

    initActivationButtons();
    initClockPicker();
    initTimePicker();
});

function ActivationService() {
    this.init = function () {
        initializeDataTable('#activationTable', token, {
            'columnDefs': [
                {
                    'targets': 4,
                    'orderable': false
                }
            ]
        }, false, true, true);
    }

    this.deleteActivation = function (id) {
        const params = {
            title: 'Slet Aktiverinsperiode?',
            text: 'Er du sikker?',
            id: id,
            token: token,
            success: 'Aktiveringsperiode slettet',
            warning: 'Kunne ikke slette aktiveringsperiode',
            url: '/rest/admin/activation/delete/',
            route: '/admin/ad/activation',
            method: 'DELETE'
        };
        initializeSwal(params);
    }

    this.editActivation = function (id) {
        location.href = `/admin/ad/activation/edit/${id}`;
    }

    this.createActivation = async function () {
        const name = document.getElementById('createActivationName').value;
        const startTime = document.getElementById('createActivationStart').value;
        const stopTime = document.getElementById('createActivationStop').value;
        const exclude = document.getElementById('createWeekend').checked;

        const isValid = verifyInputs([
            { value: name, message: "Du skal angive et navn"},
            { value: startTime, message: "Du skal angive et start tidspunkt"},
            { value: stopTime, message: "Du skal angive et stop tidspunkt"}
        ]);

        if (!isValid) {
            return;
        }

        try {
            const url = '/rest/admin/activation/create';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify({
                    "startTime": startTime,
                    "stopTime": stopTime,
                    "excludeWeekends": exclude,
                    "name": name
                }),
            });

            if (!response.ok) {
                throw new Error("Kunne ikke oprette aktiveringsperioden");
            }

            toastr.success("Aktiveringsperiode blev oprettet");
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
        autoclose: true,
        twelvehour: false
    });
}

function initTimePicker() {
    $('.time-filter').timepicker({
        timeFormat: 'HH:mm',
        interval: 15,
        minTime: '00:00',
        maxTime: '23:59',
        dynamic: false,
        dropdown: true,
        scrollbar: true
    });
}

function initActivationButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                activationService.cancelButton();
                break;
            case 'confirm':
                activationService.createActivation();
                break;
        }

        if (target.classList.contains('editActivationBtn') || target.closest('.editActivationBtn')) {
            const btn = target.closest('.editActivationBtn');
            const id = btn.dataset.id;
            activationService.editActivation(id);
        } else if (target.classList.contains('deleteActivationBtn') || target.closest('.deleteActivationBtn')) {
            const btn = target.closest('.deleteActivationBtn');
            const id = btn.dataset.id;
            activationService.deleteActivation(id);
        }
    });
}
