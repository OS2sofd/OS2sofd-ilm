var token = $("meta[name='_csrf']").attr("content");
let jsTreeService;

$(document).ready(function() {
    jsTreeService = new JsTreeService();
    jsTreeService.initJsTree().catch(() => {
        // Error handled in the method, we simply don't want to make the page load asynchronous
    });
    initJsTreeButtons();
});

function JsTreeService() {
    this.initJsTree = async function () {
        try {
            const response = await fetch("/rest/admin/orgUnits/tree", {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-TOKEN": token
                }
            });

            if (!response.ok) {
                console.error(response.statusText);
                return;
            }

            const allOUs = await response.json();

            $('#ou-tree').jstree({
                "core": {
                    "data": allOUs,
                    "themes": {
                        "icons": false
                    },
                    "multiple": false
                },
                "search" : {
                    "show_only_matches": true,
                    "search_callback": function(str, node) {
                        return (node.text.toUpperCase().includes(str.toUpperCase()));
                    }
                },
                "plugins" : ["search", "wholerow"]
            });

            // searching in the JSTree
            let to = false;
            $('#ou-tree-search').keyup(function() {
                if (to) {
                    clearTimeout(to);
                }

                to = setTimeout(function() {
                    const v = $('#ou-tree-search').val();

                    $('#ou-tree').jstree(true).search(v);
                }, 400);
            });
        } catch (error) {
            console.error("Error fetching organisational units: ", error.message);
        }
    }

    this.chooseEntity = function () {
        $('#modal-ou').modal("show");
    }

    this.saveOUs = function (){
        const selectedNodes = $('#ou-tree').jstree('get_selected', true);

        let org = $("#orgUnits");
        org.empty();

        let entity = $('#entity');

        if (selectedNodes.length === 1) {
            const node = selectedNodes[0];
            org.append('<option value=\"' + node.id + '\" selected="selected"> ' + node.text + "</option>");
            entity.val(node.text);
            entity.attr('data-uuid', node.id);
        }

        $('#modal-ou').modal('hide');
    }
}

function initJsTreeButtons() {
    $(document).on('click', (e) => {
        const target = e.target;

        const btnId = target.id || target.closest('[id]')?.id;

        switch (btnId) {
            case 'saveOuBtn':
                jsTreeService.saveOUs();
                break;
            case 'chooseEntityBtn':
                jsTreeService.chooseEntity();
                break;
        }
    });
}
