/**
 * Initializes datepickers with date restrictions that have been set on the business
 * @param startId - The id of the startDate element
 * @param stopId - The id of the stopDate element
 * @param maxFutureDays - The max future days that has been set on the business
 * @param maxEmployment - The max employment days that has been set on the business
 * @param defaultEmploymentDays - The default employment days that has been set on the business
 */
export function initStartAndStopDatepicker(startId, stopId, maxFutureDays, maxEmployment, defaultEmploymentDays) {
    let startDate = $(`#${startId}`);
    let stopDate = $(`#${stopId}`);

    const existingStartVal = startDate.val();
    const existingStopVal  = stopDate.val();

    startDate.datepicker('destroy');
    stopDate.datepicker('destroy');

    const hasMaxFutureDays = maxFutureDays > 0;
    const hasMaxEmployment = maxEmployment > 0;

    // --- Start datepicker ---
    const startDateOptions = {
        format: 'dd/mm/yyyy',
        autoclose: true,
        todayHighlight: true
    };

    if (hasMaxFutureDays) {
        startDateOptions.endDate = `+${maxFutureDays}d`;
    }

    startDate.datepicker(startDateOptions).on('changeDate', function(e) {
        // When setDate is called internally, e.date could be undefined and throw an error
        if (!e.date) {
            return;
        }

        // When start date changes, update the stop date picker limits
        const selectedStartDate = e.date;
        stopDate.datepicker('setStartDate', selectedStartDate);

        if (hasMaxEmployment) {
            const maxStopDate = new Date(selectedStartDate);
            maxStopDate.setDate(selectedStartDate.getDate() + maxEmployment);
            stopDate.datepicker('setEndDate', maxStopDate);
        }
    });

    // --- Stop datepicker ---
    const stopDateOptions = {
        format: 'dd/mm/yyyy',
        autoclose: true,
        todayHighlight: true
    };

    if (hasMaxFutureDays && hasMaxEmployment) {
        stopDateOptions.endDate = `+${maxFutureDays + maxEmployment}d`;
    }

    stopDate.datepicker(stopDateOptions);

    // --- Restore or compute correct values ----

    // Resolve the effective start value (normalize to dd/MM/yyyy for display)
    const effectiveStartDisplay = existingStartVal ? toPickerFormat(existingStartVal) : formatDate(new Date());

    let effectiveStopDisplay = null;
    if (existingStopVal) {
        effectiveStopDisplay = toPickerFormat(existingStopVal);
    } else if (defaultEmploymentDays > 0) {
        // Parse the start date we are going to use, then add defaultEmploymentDays
        const [d, m, y] = effectiveStartDisplay.split('/');
        const startDateObj = new Date(`${y}-${m}-${d}`);
        startDateObj.setDate(startDateObj.getDate() + defaultEmploymentDays);
        effectiveStopDisplay = formatDate(startDateObj);
    }
    // else we leave it empty

    // Apply start value
    startDate.datepicker('update', effectiveStartDisplay);

    if (effectiveStopDisplay) {
        stopDate.datepicker('update', effectiveStopDisplay);
    }

    // --- Apply startDate: 'today' constraints, deferred if value is in the past ---

    const startIsInPast = existingStartVal &&
        new Date(toIso(existingStartVal)) < new Date().setHours(0, 0, 0, 0);

    if (startIsInPast) {
        // Defer the 'today' lower-bound until the picker is shown, otherwise
        // setStartDate('today') runs update() immediately and filters out the
        // existing past start date, leaving getDate() empty.
        startDate.on('show', function () {
            startDate.datepicker('setStartDate', 'today');
        });
    } else {
        startDate.datepicker('setStartDate', 'today');
    }

    const stopIsInPast = existingStopVal &&
        new Date(toIso(existingStopVal)) < new Date().setHours(0, 0, 0, 0);

    if (stopIsInPast) {
        stopDate.on('show', function () {
            stopDate.datepicker('setStartDate', 'today');
        });
    } else {
        stopDate.datepicker('setStartDate', 'today');
    }
}

/**
 * Converts a date string (yyyy-MM-dd or dd/MM/yyyy) to yyyy-MM-dd for Date parsing.
 */
function toIso(val) {
    if (!val) {
        return null;
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(val)) {
        return val;
    }

    const [d, m, y] = val.split('/');
    return `${y}-${m}-${d}`;
}

function toPickerFormat(val) {
    if (!val) {
        return val;
    }

    // yyyy-MM-dd (Thymeleaf raw) → dd/mm/yyyy
    if (/^\d{4}-\d{2}-\d{2}$/.test(val)) {
        const [year, month, day] = val.split('-');
        return `${day}/${month}/${year}`;
    }
    return val;
}

export function formatDate(dateObject) {
    if (!dateObject) {
        return;
    }

    return String(dateObject.getDate()).padStart(2, "0") + "/" +
        String(dateObject.getMonth() + 1).padStart(2, "0") + "/" +
        dateObject.getFullYear();
}

export function formatDateForBackend(dateObject) {
    if (!dateObject) {
        return;
    }

    return dateObject.getFullYear() + "-" +
        String(dateObject.getMonth() + 1).padStart(2, "0") + "-" +
        String(dateObject.getDate()).padStart(2, "0");
}
