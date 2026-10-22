import {initStartAndStopDatepicker, formatDate, formatDateForBackend} from '../../fragments/datePickerService.js';
import verifyInputs from "../../fragments/verifyInputService.js";
import initializeDataTable from "../../fragments/dataTableService.js";
import {createArchetypeService} from "../../fragments/archetypeService.js";
import initializeSwal from "../../fragments/swalService.js";

var token = $("meta[name='_csrf']").attr("content");
let approveService;
const { closeModal, saveArchetype, updateArchetype } = createArchetypeService();

$(document).ready(function() {
    approveService = new ApproveService();
    approveService.init();

    $('[data-toggle="tooltip"]').tooltip();

    const approveRadio = document.getElementById('approveEmployee');
    const denyRadio = document.getElementById('denyEmployee');

    // Javascript for toggling html elements based on whether Afivs or Godkend is selected
    if (denyRadio) {
        $('#denyEmployee').change(function () {
            if (this.checked) {
                $('.elementDiv').hide();
            }
        });
    }

    if (approveRadio) {
        $('#approveEmployee').change(function () {
            if (this.checked) {
                $('.elementDiv').show();
            }
        });
    }

    approveService.onLoad();
    initApproveButtons();
});

function ApproveService() {
    // This one is too customized to be outsourced
    this.init = function () {
        let approveTable = $('#approveTable').DataTable({
            "serverSide": true,
            "destroy": true,
            "bSort": true,
            "paging": true,
            "pageLength": 25,
            "responsive": true,
            "dom": "<'row'l<'col-sm-12'tr>><'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            "ajax": function (data, callback) {
                const hideRejected = $('#hideRejected').is(':checked');
                $.ajax({
                    "url": "/rest/approve/view?hideRejected=" + hideRejected,
                    "type": "POST",
                    "headers": {
                        "X-CSRF-TOKEN": token
                    },
                    "data": JSON.stringify(data),
                    "contentType": "application/json",
                    "success": callback
                });
            },
            "language": {
                "search": "Søg",
                "lengthMenu": "_MENU_ rækker per side",
                "info": "Viser _START_ til _END_ af _TOTAL_ rækker",
                "zeroRecords": "Ingen data...",
                "infoEmpty": "Henter data...",
                "infoFiltered": "(ud af _MAX_ rækker)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                }
            },
            "columns": [
                {"data": 'name'},
                {"data": 'cpr'},
                {"data": 'cvr'},
                {"data": 'businessName'},
                {"data": "approval", "render": function (data, type, row) {
                        if (data === 'AWAITING_APPROVAL') {
                            return 'Afventer godkendelse';
                        }
                        else if (data === 'LEADER_APPROVED') {
                            return 'Leder har godkendt';
                        }
                        else if (data === 'AWAITING_EXTENSION_APPROVAL') {
                            return "Godkendt, men vil anmode om mere tid";
                        }
                        else if (data === 'DENIED') {
                            return 'Afvist';
                        }
                    }
                },
                {"data": 'id', "render": function (data, type, row) {
                        let isAdmin = $('#isAdmin').data('admin');
                        let isLeader = $('#isLeader').data('leader');

                        if ((row.approval === 'LEADER_APPROVED' || row.approval === 'AWAITING_APPROVAL') && isAdmin) {
                            return '<a data-id="'+ row.id +'" class="sendToAdminButtons"><em class="fa fa-fw fa-check"></em></a>'
                        }
                        else if (row.approval === 'AWAITING_APPROVAL' && isLeader){
                            return '<a data-id="'+ row.id +'" class="approveButtons"><em class="fa fa-fw fa-check"></em></a>'
                                + '<a data-id="'+ row.id +'" class="denyButtons"><em class="fa fa-fw fa-times"></em></a>';
                        }
                        else if (row.approval === 'AWAITING_EXTENSION_APPROVAL' && (isLeader || isAdmin)) {
                            return '<span><a data-id="'+ row.id +'" class="extensionButtons"><em class="fa fa-fw fa-check"></em></a></span><a data-id="'+ row.id +'" class="denyExtensionButtons"><em class="fa fa-fw fa-times"></em></a>';
                        }
                        return '';
                    }
                }
            ]
        });
        $.each($('.input-filter', approveTable.table().footer()), function () {
            let column = approveTable.column($(this).index());

            $('input, select', this).on('keyup change', function () {
                if (column.search() !== this.value) {
                    column.search(this.value).draw();
                }
            });
        });

        $('#hideRejected').on('ifChanged change', function () {
            approveTable.ajax.reload();
        });
    }

    this.approveArchetypeTable = function () {
        initializeDataTable('#archetypeTable', token);
    }

    this.sendToAdminApprove = function (id) {
        globalThis.location.href = "/approval/" + id;
    }

    this.chooseEntity = function () {
        $('#modal-ou').modal("show");
    }

    this.approve = function (id) {
        const params = {
            title: 'Godkend medarbejder',
            text: 'Er du sikker?',
            id: id,
            token: token,
            success: 'Medarbejder godkendt',
            warning: 'Kunne ikke godkende medarbejderen',
            url: '/rest/approve/',
            options: { confirmButtonColor: 'green' }
        };
        initializeSwal(params);
    }

    this.deny = function (id) {
        const params = {
            title: 'Afvis medarbejder',
            text: 'Er du sikker?',
            id: id,
            token: token,
            success: 'Medarbejder afvist',
            warning: 'Kunne ikke afvise medarbejderen',
            url: '/rest/deny/'
        };
        initializeSwal(params);
    }

    this.cancelButton = function () {
        globalThis.location.href = "/approve";
    }

    this.saveApprovedEmployee = async function (id ) {
        let isApproved = $('#approveEmployee').is(':checked');
        let isDenied = $('#denyEmployee').is(':checked');
        let archetype = $('#archetype').data('id');
        let ou = $('#entity').data('uuid');
        let startDate = formatDateForBackend($('#startDate').datepicker('getDate'));
        let stopDate = formatDateForBackend($('#stopDate').datepicker('getDate'));
        let activationPeriod = $('#activationSelect').find('option:selected').val();
        let businessName = $('#businessName').val();
        let enableOrdering = $('#enableOrdering').is(':checked');
        let manualAccountName = $('#manualAccountName').val();

        if (!isDenied) {
            const isValid = verifyInputs([
                {value: startDate, message: "Du skal angive et start dato"},
                {value: archetype, message: "Du skal vælge en arketype"},
                {value: ou, message: "Du skal vælge en organisation"},
                {value: [isApproved, isDenied], message: "Du skal godkende eller afvise medarbejderen", type: 'oneOfNotFalse'},
                {value: startDate, message: "Start dato må ikke være efter stop dato", compareValue: stopDate, type: 'greaterThan'}
            ]);

            if (!isValid) {
                return;
            }

            if ($('#manualAccountName').length && !manualAccountName) {
                toastr.warning("Du skal angive et brugernavn til AD-kontoen");
                return;
            }
        }

        let data = {
            "approved": isApproved,
            "denied": isDenied,
            "archetype": archetype,
            "ou": ou,
            "startDate": startDate,
            "stopDate": stopDate,
            "activationPeriod": activationPeriod,
            "businessName": businessName,
            "enableOrdering": enableOrdering,
            "manualAccountName": manualAccountName
        }

        try {
            const url = `/rest/approval/${id}`;
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

            toastr.success("Medarbejder er godkendt");
            location.href = '/approve';
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message || "Kunne ikke godkende medarbejderen");
        }
    }

    this.chooseArchetype = function () {
        approveService.approveArchetypeTable();
        $('#modal-archetype').modal("show");
    }

    this.denyExtension = function (id) {
        const params = {
            title: 'Afvise forlængelses anmodning',
            text: 'Er du sikker?',
            id: id,
            token: token,
            success: 'Forlængelse afvist',
            warning: 'Kunne ikke afvise anmodning om forlængelsen',
            url: '/rest/employee/denyExtension/'
        };
        initializeSwal(params);
    }

    this.approveExtension = function (id) {
        swal({
            title: 'Forlæng Tildeling',
            text: "Vælg ny slutdato:",
            type: "input",
            inputType: "date",
            showCancelButton: true,
            confirmButtonColor: "#18a689",
            confirmButtonText: "Gem",
            cancelButtonText: "Annuller",
            closeOnConfirm: false
        }, async function(newDate) {
            if (newDate) {
                try {
                    const url = '/rest/employee/extend';
                    const response = await fetch(url, {
                        method: 'POST',
                        headers: {
                            "content-type": "application/json",
                            'X-CSRF-TOKEN': token
                        },
                        body: JSON.stringify({
                            "id": id,
                            "stopDate": newDate
                        }),
                    });

                    if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error(errorText);
                    }

                    toastr.success("Medarbejder blev forlænget");
                    location.reload();
                } catch (error) {
                    console.error(error.message);
                    toastr.warning(error.message);
                }
            }
        });
    }

    this.chooseArchetype = function () {
        approveService.approveArchetypeTable();
        $('#modal-archetype').modal("show");
    }

    this.onLoad = function () {
        const business = $('#business');

        // Default the "order Exchange account" checkbox from the business' settings
        const enableOrdering = business.data('enable-ordering') === true;
        $('#enableOrdering').prop('checked', enableOrdering);
        if (typeof $.fn.iCheck !== 'undefined') {
            $('#enableOrdering').iCheck('update');
        }

        // Load default org unit into the input
        const defaultOrgUnitUuid = business.data('default-ou-uuid');
        const defaultOrgUnitName = business.data('default-ou-name');
        let entity = $('#entity');

        if (defaultOrgUnitUuid && defaultOrgUnitName) {
            entity.val(defaultOrgUnitName);

            entity.data('uuid', defaultOrgUnitUuid);
            entity.data('name', defaultOrgUnitName);
        } else {
            entity.val('');
            entity.removeData('uuid');
            entity.removeData('name');
        }

        // Load the start and stop dates
        const maxFutureDays = business.data('max-days-future');
        const maxEmploymentDays = business.data('max-employment-days');
        const defaultEmploymentDays = business.data('default-employment-days');
        const stopDate = $('#stopDate');

        if (defaultEmploymentDays && !stopDate.val()) {
            let startDate = $('#startDate').val();
            if (startDate === "") {
                let today = new Date();
                let newDate = new Date(today);
                let days = Number(defaultEmploymentDays);
                newDate.setDate(today.getDate() + days);
                const newStopDate = formatDate(newDate);

                stopDate.val(newStopDate);
            } else {
                let startDateObj = convertStringToDate(startDate);
                let newDate = new Date(startDateObj);
                let days = Number(defaultEmploymentDays);
                newDate.setDate(startDateObj.getDate() + days);
                const newStopDate = formatDate(newDate);

                stopDate.val(newStopDate);
            }
        }

        initStartAndStopDatepicker('startDate', 'stopDate', Number(maxFutureDays), Number(maxEmploymentDays), Number(defaultEmploymentDays));
    }
}

function convertStringToDate(dateString) {
    // Split the date string and rearrange to yyyy-mm-dd format
    const parts = dateString.split('/');
    const day = parts[0];
    const month = parts[1];
    const year = parts[2];

    // Create date object
    return new Date(year, month - 1, day);
}

function initApproveButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'cancel':
                approveService.cancelButton();
                break;
            case 'chooseArchetypeButton':
                approveService.chooseArchetype();
                break;
            case 'save-archetype':
                saveArchetype();
                break;
            case 'confirm': {
                const id = target.closest('#confirm').dataset.id;
                approveService.saveApprovedEmployee(id);
                break;
            }
        }

        if (target.classList.contains('close-modal') || target.closest('.close-modal')) {
            closeModal();
        }
        else if (target.classList.contains('sendToAdminButtons') || target.closest('.sendToAdminButtons')) {
            const id = target.closest('.sendToAdminButtons')?.dataset.id;
            approveService.sendToAdminApprove(id);
        }
        else if (target.classList.contains('approveButtons') || target.closest('.approveButtons')) {
            const id = target.closest('.approveButtons')?.dataset.id;
            approveService.approve(id);
        }
        else if (target.classList.contains('denyButtons') || target.closest('.denyButtons')) {
            const id = target.closest('.denyButtons')?.dataset.id;
            approveService.deny(id);
        }
        else if (target.classList.contains('extensionButtons') || target.closest('.extensionButtons')) {
            const id = target.closest('.extensionButtons')?.dataset.id;
            approveService.approveExtension(id);
        }
        else if (target.classList.contains('denyExtensionButtons') || target.closest('.denyExtensionButtons')) {
            const id = target.closest('.denyExtensionButtons')?.dataset.id;
            approveService.denyExtension(id);
        }
    });

    $(document).on('change', (e) => {
        const target = e.target;

        if (target.classList.contains('updateArchetypeButtons') || target.closest('.updateArchetypeButtons')) {
            const btn = target.closest('.updateArchetypeButtons');
            updateArchetype(btn);
        }
    })
}

