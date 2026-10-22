import verifyInputs from "../../fragments/verifyInputService.js";
import {prefixAlert, validateDayLimits, nameStrategyChanged, templatePreview} from "./businessHelper.js";
import initializeDataTable from "../../fragments/dataTableService.js";

var token = $("meta[name='_csrf']").attr("content");
let editBusinessService;
const CVR_PATTERN = /^\d{8}$/;

$(document).ready(function() {
    editBusinessService = new EditBusinessService();
    editBusinessService.businessArchetypeTable();
    editBusinessService.initLeaderTable();
    editBusinessService.initializeOriginalRoleState();
    editBusinessService.initEmployeeTable();
    initEditBusinessButtons();

    $('[data-toggle="tooltip"]').tooltip();

    let agencyPrefix = document.getElementById('agencyPrefix');
    if (agencyPrefix) {
        agencyPrefix.addEventListener('input', function() {
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

function EditBusinessService() {
    let originalArchetypeIds = [];
    let deselectedArchetypeIds = [];
    let editSelectedArchetypeIds = [];

    this.initializeOriginalRoleState = function() {
        const checkboxes = document.querySelectorAll('#archetypeTable input[type="checkbox"][data-id]');
        checkboxes.forEach(checkbox => {
            if (checkbox.checked) {
                const archetypeId = checkbox.dataset.id;
                if (!originalArchetypeIds.includes(archetypeId)) {
                    originalArchetypeIds.push(archetypeId);
                }
                if (!originalArchetypeIds.includes(archetypeId)) {
                    originalArchetypeIds.push(archetypeId);
                }
            }
        });
    }

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

    this.initLeaderTable = function () {
        initializeDataTable('#leaderTable', token, {
            'columnDefs': [
                {
                    'targets': 3,
                    'orderable': false
                }
            ]
        });
    }

    this.initEmployeeTable = function () {
        initializeDataTable('#affiliatedTable', token, {
            'columnDefs': [
                {
                    'targets': 3,
                    'orderable': false
                }
            ]
        })
    }

    this.saveEditedAgency = async function (id) {
        let name = $('#agencyName').val();
        let cvr = $('#agencyCvr').val();
        let prefix = $('#agencyPrefix').val();
        let activationPeriod = $('#activationSelect').find('option:selected').val();
        let ou = $('#entity').data('uuid');
        let activeLeader = $('#editActiveLeader').is(':checked');
        let inherit = $('#editInheritPrivileges').is(':checked');
        let maxFutureDays = $('#editMaxDaysInFuture').val();
        let maxEmploymentDays = $('#editMaxEmploymentDays').val();
        let defaultEmploymentDays = $('#editDefaultEmploymentDays').val();
        let nameStrategy = $('#nameStrategy').val();
        let accountNameTemplate = $('#accountNameTemplate').val();
        let enableOrdering = $('#enableOrdering').is(':checked');

        const validated = validateDayLimits(Number(maxFutureDays), Number(maxEmploymentDays), Number(defaultEmploymentDays));
        if (!validated) {
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
            "archetypesToRemove": deselectedArchetypeIds,
            "archetypesToAdd": editSelectedArchetypeIds,
            "nameStrategy": nameStrategy,
            "accountNameTemplate": accountNameTemplate,
            "enableOrdering": enableOrdering
        }

        try {
            const url = `/rest/business/edit/${id}`;
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            toastr.success("Virksomhed blev opdateret");
            location.href = '/business/overview';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.cancelButton = function () {
        location.href = '/business/overview';
    }

    this.handleNotExistingCvr = function (cvrInput) {
        cvrInput.addClass('is-invalid');
        let businessName = $('#businessName');
        businessName.text("Det Cvr-nummer du har indtastet findes ikke.");
        businessName.css('display', 'block');

        editBusinessService.showMessage(
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
                        "content-type": "application/json",
                        'X-CSRF-TOKEN': token
                    }
                });

                if (!response.ok) {
                    throw new Error("Der opstod en teknisk fejl");
                }

                const data = await response.json();
                if (data?.cvr) {
                    editBusinessService.handleExistingCvr(cvrInput, data.name);
                } else {
                    editBusinessService.handleNotExistingCvr(cvrInput);
                }

            } catch (error) {
                console.error(error.message);
                // There is no error message that makes sense here
                toastr.warning(error.message);
            }
        }
    }

    this.handleExistingCvr = function (cvrInput, name) {
        cvrInput.addClass('is-valid');
        let businessName = $('#cvrSearchResult');
        businessName.text(name);
        businessName.css('display', 'block');

        editBusinessService.showMessage(
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

    this.editUpdateArchetype = function (checkbox) {
        const archetypeId = checkbox?.dataset.id;
        const wasOriginal = originalArchetypeIds.includes(archetypeId);
        const isChecked = checkbox.checked;

        if (isChecked) {
            // Checkbox is checked
            if (!editSelectedArchetypeIds.includes(archetypeId)) {
                editSelectedArchetypeIds.push(archetypeId);
            }

            // If this was originally selected and was deselected, remove from deselected array
            if (wasOriginal) {
                const deselectedIndex = deselectedArchetypeIds.indexOf(archetypeId);
                if (deselectedIndex > -1) {
                    deselectedArchetypeIds.splice(deselectedIndex, 1);
                }
            }
        } else {
            // Checkbox is unchecked
            editSelectedArchetypeIds = editSelectedArchetypeIds.filter(id => id !== archetypeId);

            // If this was originally selected, add to deselected array
            if (wasOriginal && !deselectedArchetypeIds.includes(archetypeId)) {
                deselectedArchetypeIds.push(archetypeId);
            }
        }
    }
}

function initEditBusinessButtons() {
    $(document).on('change', (e) => {
        const target = e.target;

        if (target.classList.contains('editUpdateArchetypeButtons') || target.closest('.editUpdateArchetypeButtons')) {
            const checkbox = target.closest('.editUpdateArchetypeButtons');

            editBusinessService.editUpdateArchetype(checkbox);
        }
    });

    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                editBusinessService.cancelButton();
                break;
            case 'confirm': {
                const id = target.closest('#confirm').dataset.id;
                editBusinessService.saveEditedAgency(id);
            }
        }
    })

    $(document).on('input', (e) => {
        const target = e.target;

        if (target.id === 'agencyCvr' || target.closest('#agencyCvr')) {
            editBusinessService.onCvrSearch();
        }
    })
}
