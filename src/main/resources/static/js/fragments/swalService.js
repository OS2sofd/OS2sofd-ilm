/**
 * @param {string} title - The title of the swal that users see.
 * @param {string} text - The description of the swal which describes the action.
 * @param {number} id - The id of the object that the action is being performed on.
 * @param {string} token - The CSRF token.
 * @param {string} success - The success message for the swal.
 * @param {string} warning - The error message for the swal.
 * @param {string} url - The url of the rest method.
 * @param {string} [route] - The route to redirect to after the action. Reloads if omitted.
 * @param {string} [method=POST] - The HTTP method.
 * @param {Object} [options] - Custom swal option overrides.
 */
export default function initializeSwal({ title, text, id, token, success, warning, url, route, method = 'POST', options = {} }) {
    const defaultOptions = {
        title: title,
        text: text,
        showCancelButton: true,
        confirmButtonColor: "red",
        confirmButtonText: "Ja",
        cancelButtonText: "Nej",
        closeOnConfirm: true,
        closeOnCancel: true
    };

    return swal({...defaultOptions, ...options}, async function (isConfirm) {
        if (!isConfirm) {
            return;
        }

        try {
            const response = await fetch(url + id, {
                method: method,
                headers: {
                    "content-type": "application/json",
                    'X-CSRF-TOKEN': token
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            toastr.success(success);

            if (route) {
                globalThis.location.href = route;
            } else {
                globalThis.location.reload();
            }
        } catch (error) {
            console.error(error);
            toastr.warning(warning);
        }
    });
}
