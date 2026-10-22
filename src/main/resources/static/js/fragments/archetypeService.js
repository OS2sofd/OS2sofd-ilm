/**
*   A factory that owns functions to save and update archetypes, as well as a function to close the modal.
*/
export function createArchetypeService() {
    let archetypeName = null;
    let archetypeId = null;

    function closeModal() {
        $('input[name="archetypeChoice"]').prop('checked', false);
        archetypeId = null;
        archetypeName = null;
        $('#modal-archetype').modal('hide');
    }

    function saveArchetype() {
        if (archetypeName !== null) {
            $('#archetype').val(archetypeName);
        }
        if (archetypeId !== null) {
            $('#archetype').attr('data-id', archetypeId);
        }
        $('#modal-archetype').modal('hide');
    }

    function updateArchetype(obj) {
        archetypeId = null;
        archetypeName = null;

        if ($(obj).prop('checked')) {
            archetypeName = $(obj).data('name');
            archetypeId = $(obj).data('id');
        }
    }

    return {closeModal, saveArchetype, updateArchetype};
}
