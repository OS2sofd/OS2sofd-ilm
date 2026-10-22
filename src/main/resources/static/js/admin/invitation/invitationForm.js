import verifyInputs from "../../fragments/verifyInputService.js";

const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

$(document).ready(function () {
    init();
});

function init() {
    initializePageButtons();
}

function initializePageButtons() {
    $(document).on('click', '#sendButton', function (e) {
        const name = $('#nameInput').val();
        const mail = $('#mailInput').val();
        sendInvitation(name, mail);
    });
}

async function sendInvitation(name, mail) {
    const isValid = verifyInputs([
        {value: name, message: "Du skal angive navnet af modtageren"},
        {value: mail, message: "Du skal angive mailadressen af modtageren"}
    ]);

    if (!isValid) {
        return;
    }

    if (!EMAIL_PATTERN.test(mail)) {
        toastr.warning("Ugyldig mailadresse");
        return;
    }

    const sendButton = document.getElementById('sendButton');
    sendButton.disabled = true;

    try {
        const url = '/rest/admin/send/invitation';
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'content-type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector("meta[name='_csrf']").getAttribute("content")
            },
            body: JSON.stringify({
                recipient: name,
                mail: mail
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        toastr.success("Invitationen er sendt");
        document.getElementById('nameInput').value = '';
        document.getElementById('mailInput').value = '';
    } catch (error) {
        console.error(error.message);
        toastr.warning(error.message);
    } finally {
        sendButton.disabled = false;
    }
}
