const EDITOR_HEIGHT = 320;

// same formatting options as the mail template editor in OS2sofd
const EDITOR_TOOLBAR = [
    ['style', ['style', 'bold', 'italic', 'underline', 'clear']],
    ['font', ['fontname', 'fontsize', 'color', 'height']],
    ['para', ['ul', 'ol', 'paragraph']],
    ['insert', ['table', 'link']],
    ['view', ['codeview']]
];
const EDITOR_STYLE_TAGS = ['p', 'blockquote', 'pre', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'];
const EDITOR_FONT_SIZES = ['8', '9', '10', '11', '12', '14', '16', '18', '24', '36', '48'];
const EDITOR_LINE_HEIGHTS = ['1.0', '1.2', '1.4', '1.5', '1.6', '1.8', '2.0', '3.0'];

// summernote 0.8.10 puts the code view html straight into the live editor, where the browser runs it (meta refresh, object/embed loads, onerror etc.).
// these are removed before that happens - the server sanitizer still decides what is saved, this only protects the admin's browser
const DANGEROUS_ELEMENTS = 'script, style, iframe, frame, frameset, object, embed, applet, meta, link, base, form, input, button, textarea, select, svg, math, img, video, audio, source, track, template, noscript';
const URL_ATTRIBUTES = ['href', 'src', 'action', 'formaction', 'xlink:href', 'data', 'background', 'poster'];
const SAFE_URL_PATTERN = /^(https:|mailto:|[^:]*$)/i;
const UNSAFE_STYLE_PATTERN = /url\s*\(|expression\s*\(/i;

// the removed-html warning can be long, so keep it on screen until the user closes it
const WARNING_TOAST_OPTIONS = { timeOut: 0, extendedTimeOut: 0, closeButton: true };

const BYTES_PER_MB = 1024 * 1024;
const MAX_ATTACHMENT_SIZE_MB = 10;
const ALLOWED_ATTACHMENT_EXTENSIONS = ['pdf', 'doc', 'docx', 'odt', 'xls', 'xlsx', 'ods', 'txt', 'png', 'jpg', 'jpeg'];

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('emailTemplateForm');
    const content = document.getElementById('content');
    if (!form || !content) {
        console.warn('emailTemplateForm or content not found - aborting init');
        return;
    }

    // the stored content is sanitized on save, but clean it anyway before the editor renders it
    content.value = sanitizeEditorHtml(content.value);

    const editor = $(content);
    editor.summernote({
        modules: { ...$.summernote.options.modules, codeview: createSafeCodeView() },
        height: EDITOR_HEIGHT,
        toolbar: EDITOR_TOOLBAR,
        styleTags: EDITOR_STYLE_TAGS,
        fontSizes: EDITOR_FONT_SIZES,
        lineHeights: EDITOR_LINE_HEIGHTS,
        dialogsInBody: true
    });

    form.addEventListener('submit', () => {
        // summernote does not update the textarea while in codeview, so switch back before submitting
        if (editor.summernote('codeview.isActivated')) {
            editor.summernote('codeview.deactivate');
        }
    });

    const token = document.querySelector("meta[name='_csrf']")?.getAttribute('content');
    const id = document.getElementById('id')?.value;

    initTestEmail(editor, id, token);
    initAttachmentModal(id, token);
    initRemoveAttachment(token);
});

// a code view that cleans the html before summernote puts it into the live editor
function createSafeCodeView() {
    const BaseCodeView = $.summernote.options.modules.codeview;

    return class SafeCodeView extends BaseCodeView {
        deactivate() {
            const codeMirror = this.$codable.data('cmEditor');
            if (codeMirror) {
                codeMirror.setValue(sanitizeEditorHtml(codeMirror.getValue()));
            }
            else {
                this.$codable.val(sanitizeEditorHtml(this.$codable.val()));
            }

            super.deactivate();
        }
    };
}

// DOMParser builds an inert document, so nothing in the html is run or loaded while it is cleaned
function sanitizeEditorHtml(html) {
    const doc = new DOMParser().parseFromString(html ?? '', 'text/html');

    doc.body.querySelectorAll(DANGEROUS_ELEMENTS).forEach(element => {
        element.remove();
    });

    doc.body.querySelectorAll('*').forEach(element => {
        for (const attribute of [...element.attributes]) {
            const name = attribute.name.toLowerCase();
            const value = attribute.value.replace(/[\s\u0000-\u001F]/g, '');

            const isEventHandler = name.startsWith('on');
            const isUnsafeUrl = URL_ATTRIBUTES.includes(name) && !SAFE_URL_PATTERN.test(value);
            const isUnsafeStyle = name === 'style' && UNSAFE_STYLE_PATTERN.test(attribute.value);
            if (isEventHandler || isUnsafeUrl || isUnsafeStyle) {
                element.removeAttribute(attribute.name);
            }
        }
    });

    // comments are not visible anyway, and could hide conditional comment tricks
    const comments = doc.createTreeWalker(doc.body, NodeFilter.SHOW_COMMENT);
    const commentNodes = [];
    while (comments.nextNode()) {
        commentNodes.push(comments.currentNode);
    }
    commentNodes.forEach(comment => {
        comment.remove();
    });

    return doc.body.innerHTML;
}

function initTestEmail(editor, id, token) {
    const testEmailBtn = document.getElementById('testEmailBtn');
    if (!testEmailBtn) {
        console.warn('testEmailBtn not found - test email disabled');
        return;
    }

    const subject = document.getElementById('subject');
    testEmailBtn.addEventListener('click', () => {
        sendTestEmail(testEmailBtn, id, subject?.value, editor.summernote('code'), token);
    });
}

// the modal is only rendered when S3 is configured
function initAttachmentModal(id, token) {
    const modal = document.getElementById('attachmentModal');
    if (!modal) {
        return;
    }

    const fileInput = document.getElementById('attachmentFile');
    const errorText = document.getElementById('attachmentError');
    const uploadBtn = document.getElementById('uploadAttachmentBtn');
    if (!fileInput || !errorText || !uploadBtn) {
        console.warn('attachment modal is missing elements - attachments disabled');
        return;
    }

    const showError = (message) => {
        errorText.textContent = message;
        errorText.hidden = !message;
    };

    fileInput.addEventListener('change', () => {
        const file = fileInput.files?.[0];
        const error = validateAttachment(file);
        showError(error);
        uploadBtn.disabled = !file || error !== '';
    });

    uploadBtn.addEventListener('click', () => {
        const file = fileInput.files?.[0];
        if (file) {
            uploadAttachment(uploadBtn, modal, id, file, token, showError);
        }
    });

    // start from a clean dialog every time it is opened
    $(modal).on('hidden.bs.modal', () => {
        fileInput.value = '';
        uploadBtn.disabled = true;
        showError('');
    });
}

function validateAttachment(file) {
    if (!file) {
        return '';
    }

    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!ALLOWED_ATTACHMENT_EXTENSIONS.includes(extension)) {
        return 'Filtypen er ikke tilladt';
    }

    if (file.size > MAX_ATTACHMENT_SIZE_MB * BYTES_PER_MB) {
        return `Filen er for stor. Der tillades max ${MAX_ATTACHMENT_SIZE_MB} MB`;
    }

    return '';
}

async function uploadAttachment(button, modal, id, file, token, showError) {
    button.disabled = true;

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch(`/rest/admin/emailtemplate/${id}/attachment`, {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            body: formData
        });

        if (!response.ok) {
            showError(await response.text() || 'Kunne ikke uploade filen');
            button.disabled = false;
            return;
        }

        toastr.success('Filen er vedhæftet');
        $(modal).modal('hide');

        // the attachment name/remove button come from the server-rendered page, so reload to show the new one
        window.location.reload();
    }
    catch (error) {
        console.error(error);
        showError('Kunne ikke uploade filen');
        button.disabled = false;
    }
}

function initRemoveAttachment(token) {
    const removeBtn = document.getElementById('removeAttachmentBtn');
    if (!removeBtn) {
        return;
    }

    removeBtn.addEventListener('click', async () => {
        if (!window.confirm('Er du sikker på, at den vedhæftede fil skal fjernes?')) {
            return;
        }

        removeBtn.disabled = true;

        try {
            const response = await fetch(`/rest/admin/emailtemplate/${removeBtn.dataset.id}/attachment`, {
                method: 'DELETE',
                headers: {
                    'X-CSRF-TOKEN': token
                }
            });

            if (!response.ok) {
                toastr.warning(await response.text() || 'Kunne ikke fjerne filen');
                removeBtn.disabled = false;
                return;
            }

            toastr.success('Den vedhæftede fil er fjernet');
            window.location.reload();
        }
        catch (error) {
            console.error(error);
            toastr.warning('Kunne ikke fjerne filen');
            removeBtn.disabled = false;
        }
    });
}

async function sendTestEmail(button, id, subject, content, token) {
    button.disabled = true;

    try {
        const response = await fetch(`/rest/admin/emailtemplate/${id}/test`, {
            method: 'POST',
            headers: {
                'content-type': 'application/json',
                'X-CSRF-TOKEN': token
            },
            body: JSON.stringify({ subject: subject, content: content })
        });

        if (!response.ok) {
            toastr.warning(await response.text() || 'Kunne ikke sende test email');
            return;
        }

        const result = await response.json();
        toastr.success(result?.message);
        if (result?.warning) {
            toastr.warning(result.warning, 'Indhold fjernet', WARNING_TOAST_OPTIONS);
        }
    }
    catch (error) {
        console.error(error);
        toastr.warning('Kunne ikke sende test email');
    }
    finally {
        button.disabled = false;
    }
}
