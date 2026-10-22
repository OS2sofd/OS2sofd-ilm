export function validateDayLimits(maxFutureDays, maxEmploymentDays, defaultEmploymentDays) {
    let isValid = true;

    if (maxEmploymentDays > 0 && defaultEmploymentDays > maxEmploymentDays) {
        toastr.warning("Standard ansættelse må ikke være længere end maks ansættelse");
        isValid = false;
    }
    if (defaultEmploymentDays < 0) {
        toastr.warning("Standard ansættelse må ikke være negativ");
        isValid = false;
    }
    if (maxFutureDays < 0) {
        toastr.warning("Maks dage i fremtiden for ansættelse må ikke være negativ");
        isValid = false;
    }
    if (maxEmploymentDays < 0) {
        toastr.warning("Maks dage for ansættelse må ikke være negativ");
        isValid = false;
    }

    return isValid;
}

export function prefixAlert(inputString) {
    const input = inputString.value.trim();
    const displayDiv = document.getElementById('prefixDisplay');
    const textSpan = document.getElementById('prefixText');

    displayDiv.hidden = !input;
    if (input) {
        textSpan.textContent = 'ILM-' + input + '-0001';
    }
}

// Toggles which naming-strategy inputs are visible based on the selected strategy.
// idPrefix lets the same function serve both the create and edit business forms,
// whose element ids differ only by a prefix (e.g. "createAgencyPrefix" vs "agencyPrefix").
export function nameStrategyChanged(selectElement, idPrefixRow, idTemplateRow) {
    const strategy = selectElement.value;
    const prefixRow = document.getElementById(idPrefixRow);
    const templateRow = document.getElementById(idTemplateRow);

    if (prefixRow) {
        prefixRow.hidden = strategy !== 'PREFIXED';
    }
    if (templateRow) {
        templateRow.hidden = strategy !== 'TAILORED';
    }
}

// Illustrative-only preview of a TAILORED template using fixed example values.
// Actual resolution always happens server-side via AccountNameTemplateService.
export function templatePreview(inputString) {
    const template = inputString.value.trim();
    const displayDiv = document.getElementById('templatePreviewDisplay');
    const textSpan = document.getElementById('templatePreviewText');

    displayDiv.hidden = !template;
    if (!template) {
        return;
    }

    const exampleValues = {
        'løbenummer': '0001',
        'person-fornavn': 'Jens',
        'person-efternavn': 'Jensen',
        'person-navn': 'Jens Jensen',
        'person-initialer': 'JJ',
        'virksomhed-navn': 'Digital Identity',
        'virksomhed-initialer': 'DI'
    };

    let resolved = template;
    for (const [token, value] of Object.entries(exampleValues)) {
        resolved = resolved.split('{' + token + '}').join(value);
    }

    textSpan.textContent = resolved;
}
