$(document).ready(function() {

    let date = $("input[type=text]#startDate, input[type=text]#stopDate, input[type=text]#editStartDate, input[type=text]#editStopDate");
    date.datepicker({
        format: 'dd/mm/yyyy',
        weekStart: 1
    });

    // Process and format existing values
    date.each(function() {
        let existingValue = $(this).val();
        if (existingValue && existingValue.match(/^\d{4}-\d{2}-\d{2}$/)) {
            let dateObj = new Date(existingValue + 'T00:00:00Z');
            $(this).datepicker('setUTCDate', dateObj);
        }
    });

    $('.input-group-text').on('click', function() {
        $(this).closest('.input-group').find('input').datepicker('show');
    });
});
