import verifyInputs from "../../fragments/verifyInputService.js";
import {prefixAlert, validateDayLimits, nameStrategyChanged, templatePreview} from "./businessHelper.js";
import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let createBusinessService;
const CVR_PATTERN = /^\d{8}$/;

$(document).ready(function() {
    createBusinessService = new CreateBusinessService();
    createBusinessService.businessArchetypeTable();
    initCreateBusinessButtons();

    $('[data-toggle="tooltip"]').tooltip();

    let createAgencyPrefix = document.getElementById('createAgencyPrefix');
    if (createAgencyPrefix) {
        createAgencyPrefix.addEventListener('input', function() {
            prefixAlert(this);
        });
    }

    let accountNameTemplate = document.getElementById('accountNameTemplate');
    if (accountNameTemplate) {
        accountNameTemplate.addEventListener('input', function() {
            templatePreview(this);
        });
    }

    let nameStrategySelect = document.getElementById('nameStrategy');
    if (nameStrategySelect) {
        nameStrategySelect.addEventListener('change', function() {
            nameStrategyChanged(this, 'prefixRow', 'templateRow');
        });
        nameStrategyChanged(nameStrategySelect, 'prefixRow', 'templateRow');
    }
});

function CreateBusinessService() {
    let selectedArchetypeIds = [];

    this.businessArchetypeTable = function () {
        initializeDataTable('#archetypeTable', token, {
            'columnDefs': [
                {
                    'targets': 1,
                    'orderable': false
                }
            ]
        });
    }

    this.cancelButton = function () {
        location.href = '/business/overview';
    }

    this.handleNotExistingCvr = function (cvrInput) {
        cvrInput.addClass('is-invalid');
        let businessName = $('#businessName');
        businessName.text("Det Cvr-nummer du har indtastet findes ikke.");
        businessName.css('display', 'block');

        createBusinessService.showMessage(
            'Cvr-nummer er ugyldigt og findes ikke i systemet.',
            'danger'
        );
    }

    this.onCvrSearch = async function () {
        const cvrInput = $('.searchCvr');
        if (!cvrInput.val() || cvrInput.val().length < 1) {
            $('#cvrSearchResult').text("");
            $('#cvrSearchResult').hide();
        }
        // Clear previous validation states
        cvrInput.removeClass('is-invalid is-valid');

        if (cvrInput.val().length > 1) {
            try {
                const url = `/rest/search/${cvrInput.val()}`;
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        'content-type': 'application/json',
                        'X-CSRF-TOKEN': token
                    }
                });

                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(errorText);
                }

                const data = await response.json();
                if (data?.cvr) {
                    createBusinessService.handleExistingCvr(cvrInput, data.name);
                }
                else {
                    createBusinessService.handleNotExistingCvr(cvrInput);
                }

            } catch (error) {
                console.error(error.message);
                toastr.warning(error.message);
            }
        }
    }

    this.handleExistingCvr = function ($cvrInput, name) {
        $cvrInput.addClass('is-valid');
        let businessName = $('#cvrSearchResult');
        businessName.text(name);
        businessName.css('display', 'block');

        createBusinessService.showMessage(
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

    this.createAgency = async function () {
        const name = $('#createAgencyName').val();
        const cvr = $('#createAgencyCvr').val();
        const prefix = $('#createAgencyPrefix').val();
        let activationPeriod = $('#activationSelect').find('option:selected').val();
        let ou = $('#entity').data('uuid');
        let activeLeader = $('#activeLeader').is(':checked');
        let inherit = $('#inheritPrivileges').is(':checked');
        let maxFutureDays = $('#createMaxDaysInFuture').val();
        let maxEmploymentDays = $('#createMaxEmploymentDays').val();
        let defaultEmploymentDays = $('#createDefaultEmploymentDays').val();
        let nameStrategy = $('#nameStrategy').val();
        let accountNameTemplate = $('#accountNameTemplate').val();
        let enableOrdering = $('#enableOrdering').is(':checked');

        const isValidLimits = validateDayLimits(Number(maxFutureDays), Number(maxEmploymentDays), Number(defaultEmploymentDays));

        if (!isValidLimits) {
            return;
        }

        const validations = [
            { value: name, message: "Du skal angive et navn"},
            { value: cvr, message: "Du skal angive et CVR-nummer"},
        ];
        if (nameStrategy === 'PREFIXED') {
            validations.push({ value: prefix, message: "Du skal angive et virksomhedsprefix"});
        }
        if (nameStrategy === 'TAILORED') {
            validations.push({ value: accountNameTemplate, message: "Du skal angive et brugernavnsformat"});
        }

        const isValid = verifyInputs(validations);

        if (!isValid) {
            return;
        }

        if (!CVR_PATTERN.test(cvr)) {
            toastr.warning("Dit CVR-nummer er ugyldigt");
            return;
        }

        let data = {
            "name": name,
            "cvr": cvr,
            "prefix": prefix,
            "activationPeriod": activationPeriod,
            "ou": ou,
            "activeLeader": activeLeader,
            "inherit": inherit,
            "maxFutureDays": maxFutureDays,
            "maxEmploymentDays": maxEmploymentDays,
            "defaultEmploymentDays": defaultEmploymentDays,
            "archetypeIds": selectedArchetypeIds,
            "nameStrategy": nameStrategy,
            "accountNameTemplate": accountNameTemplate,
            "enableOrdering": enableOrdering
        }

        try {
            const url = '/rest/business/create';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorText = await response.text()
                throw new Error(errorText);
            }

            toastr.success("Virksomhed blev oprettet");
            location.href = '/business/overview';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.updateArchetype = function (checkbox) {
        const archetypeId = checkbox.dataset.id;
        if (checkbox.checked) {
            if (!selectedArchetypeIds.includes(archetypeId)) {
                selectedArchetypeIds.push(archetypeId);
            }
        } else {
            selectedArchetypeIds = selectedArchetypeIds.filter(id => id !== archetypeId);
        }
    }
}

function initCreateBusinessButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                createBusinessService.cancelButton();
                break;
            case 'confirm':
                createBusinessService.createAgency();
                break;
        }
    });

    $(document).on('change', (e) => {
        const target = e.target;

        if (target.classList.contains('updateArchetypeButtons') || target.closest('.updateArchetypeButtons')) {
            const checkbox = target.closest('.updateArchetypeButtons');

            createBusinessService.updateArchetype(checkbox);
        }
    });

    $(document).on('input', (e) => {
        const target = e.target;

        if (target.id === 'createAgencyCvr' || target.closest('#createAgencyCvr')) {
            createBusinessService.onCvrSearch();
        }
    })
}
