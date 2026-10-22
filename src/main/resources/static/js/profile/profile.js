import verifyInputs from "../fragments/verifyInputService.js";
import initializeSwal from "../fragments/swalService.js";

var token = $("meta[name='_csrf']").attr("content");
let profileService;

$(document).ready(function() {
    profileService = new ProfileService();
    profileService.init();

    initProfileButtons();
});

function ProfileService() {
    this.init = function () {
        document.getElementById('saveEditProfileBtn').style.display = 'none';
        document.getElementById('cancelEditProfileBtn').style.display = 'none';
    }

    this.enableEditProfile = function () {
        document.getElementById('nameInput').readOnly = false;
        document.getElementById('phoneInput').readOnly = false;
        document.getElementById('enableEditProfileBtn').style.display = 'none';
        document.getElementById('saveEditProfileBtn').style.display = '';
        document.getElementById('cancelEditProfileBtn').style.display = '';
    }

    this.disableEditProfile = function () {
        document.getElementById('nameInput').readOnly = true;
        document.getElementById('phoneInput').readOnly = true;
        document.getElementById('enableEditProfileBtn').style.display = '';
        document.getElementById('saveEditProfileBtn').style.display = 'none';
        document.getElementById('cancelEditProfileBtn').style.display = 'none';
    }

    this.saveEditProfile = async function (id) {
        const name = document.getElementById('nameInput').value;
        const phone = document.getElementById('phoneInput').value;

        const isValid = verifyInputs([
            {value: name, message: "Du skal angive dit navn"}
        ]);

        if (!isValid) {
            return;
        }

        try {
            const url = '/rest/profile/edit';
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'content-type': 'application/json',
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify({
                    id: id,
                    name: name,
                    phoneNumber: phone
                })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            toastr.success("Ændringerne er blevet gemt");
            setTimeout(function() {
                location.reload();
            }, 1000);
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.resetPassword = async function (id) {
        try {
            const url = `/rest/profile/resetPassword/${id}`;
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

            toastr.success("Kodeord blev nulstillet");
            setTimeout(function() {
                location.reload();
            }, 1000);
        } catch (error) {
            console.error(error.message);
            toastr.warning(error.message);
        }
    }

    this.requestExtension = function (id) {
        const params = {
            title: 'Anmod',
            text: 'Er du sikker?',
            id: id,
            token: token,
            success: 'Anmodning om forlængelse sendt',
            warning: 'Kunne ikke sende anmodningen',
            url: '/rest/profile/requestExtension/'
        };
        initializeSwal(params);
    }
}

function initProfileButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'resetPasswordBtn': {
                const id = target.closest('#resetPasswordBtn')?.dataset.id;
                profileService.resetPassword(id);
                break;
            }
            case 'requestExtensionBtn': {
                const id = target.closest('#requestExtensionBtn')?.dataset.id;
                profileService.requestExtension(id);
                break;
            }
            case 'enableEditProfileBtn': {
                profileService.enableEditProfile();
                break;
            }
            case 'cancelEditProfileBtn': {
                profileService.disableEditProfile();
                break;
            }
            case 'saveEditProfileBtn': {
                const id = target.closest('#saveEditProfileBtn')?.dataset.id;
                profileService.saveEditProfile(id);
                break;
            }
        }
    });
}
