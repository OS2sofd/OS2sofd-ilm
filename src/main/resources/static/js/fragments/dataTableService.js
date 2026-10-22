/**
 * Initializes a DataTable and attaches custom search logic.
 * @param {string} tableSelector - The select (id) of the table, e.g., '#activationTable'.
 * @param {string} token - A token we use for ajax calls, we use this when we make a view.
 * @param {Object} options - DataTables options object (extends the default options).
 * @param {boolean} datepickerEnabled - Enables datepicker, i.e., the DataTable has a custom datepicker search function.
 * @param {boolean} timepickerEnabled - Enables timepicker, i.e., the DataTable has a custom timepicker search function.
 * @param {boolean} checkboxEnabled - Enables checkbox filtering. Any `input[type=checkbox]` inside an
 *        `.input-filter` footer cell becomes a toggle filter on that column. Use `data-checked-value` and
 *        `data-unchecked-value` on the checkbox to specify the cell text to match in each state.
 * @returns {*|jQuery} - A jQuery DataTable.
 */

// Export the initializeDataTable function as the default export
export default function initializeDataTable(tableSelector, token, options = {}, datepickerEnabled = false, timepickerEnabled = false, checkboxEnabled = false) {
    const defaultOptions =  {
        destroy: true,
        bSort: true,
        paging: true,
        pageLength: 25,
        responsive: true,
        dom: "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
        language: {
            search: "Søg",
            lengthMenu: "_MENU_ rækker per side",
            info: "Viser _START_ til _END_ af _TOTAL_ rækker",
            zeroRecords: "Ingen data...",
            infoEmpty: "Henter data...",
            infoFiltered: "(ud af _MAX_ rækker)",
            paginate: {
                previous: "Forrige",
                next: "Næste"
            }
        }
    };

    // Combine default options with any provided custom options
    const dataTableOptions = { ...defaultOptions, ...options };

    // Initialize the DataTable
    const dataTable = $(tableSelector).DataTable(dataTableOptions);

    $.each($('.input-filter', dataTable.table().footer()), function () {
        const column = dataTable.column($(this).index());

        // Default input filtering
        $('input, select', this).on('keyup change', function () {
            // Checkbox filtering (handled below) — skip the default value-based search.
            if (checkboxEnabled && $(this).is(':checkbox')) {
                return;
            }

            if (column.search() !== this.value) {
                column.search(this.value).draw();
            }
        });

        // If timepicker is enabled we need special filtering
        if (timepickerEnabled) {
            $('.time-filter').on('changeTime', function() {
                const selectedTime = $(this).val();

                // Custom search
                dataTable.column(2).search(selectedTime).draw();
            });
        }

        // If checkbox filtering is enabled, toggle rows in this column based on the checkbox state.
        // The cell text to match in each state comes from `data-checked-value` / `data-unchecked-value`.
        if (checkboxEnabled) {
            $('input:checkbox', this).on('change', function () {
                const checkedValue = $(this).data('checked-value');
                const uncheckedValue = $(this).data('unchecked-value');
                const target = this.checked ? checkedValue : uncheckedValue;

                if (target === undefined) {
                    column.search('').draw();
                    return;
                }

                // Anchor the regex so we match the cell exactly, not as a substring.
                column.search('^' + target + '$', true, false).draw();
            });
        }

        // If datepicker is enabled we need special filtering
        if (datepickerEnabled) {
            $('input:is(.date-filter), select', this).on('keyup change', function () {
                let value = this.value;

                if (!value) {
                    column.search('').draw();
                    return;
                }

                let parts = value.split('/');
                let date = parts[2] + '-' + parts[1] + '-' + parts[0];

                if (column.search() !== date) {
                    column.search(date).draw();
                }
            });
        }
    });

    if (datepickerEnabled) {
        $('.date-filter').datepicker({
            format: 'dd/mm/yyyy',
            autoclose: true,
            todayHighlight: true,
            weekStart: 1
        });
        $('.input-group-addon').on('click', function () {
            $(this).siblings('input.date-filter').datepicker('show');
        });
    }

    return dataTable;
}
