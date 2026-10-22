var token = $("meta[name='_csrf']").attr("content");
let searchService;

$(document).ready(function() {
    searchService = new SearchService();
    searchService.init();
});

function SearchService() {
    let currentIndex = -1;
    this.init = function () {
        // focus searchbar on double-shift
        let lastShift = 0;
        globalThis.addEventListener('keydown', function(event) {
            if( event.key === 'Shift' ) {
                if(Date.now() - lastShift < 500 ) {
                    $('#searchBar').focus();
                }
                else {
                    lastShift = Date.now();
                }
            }
            else {
                lastShift = 0;
            }
        });
    }

    this.onSearch = async function () {
        const searchBar = $('#searchBar').val();
        if (searchBar.length > 1) {
            try {
                const url = `/rest/search/${searchBar}`;
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        "content-type": "application/json",
                        'X-CSRF-TOKEN': token
                    }
                });

                if (!response.ok) {
                    throw new Error("Der opstod en teknisk fejl");
                }

                const data = await response.json();
                searchService.displayResults(data);
            } catch (error) {
                console.error(error);
                // There is no error message that makes sense here
                toastr.warning("Der opstod en teknisk fejl");
            }
        } else {
            $('#searchResults').empty();
            currentIndex = -1;
        }
    }

    this.displayResults = function (data) {
        const iconMap = {
            "Vikar": "fa fa-user",
            "Vikarer": "fa fa-users",
            "Bureau": "fa fa-building",
            "Godkendelser": "fa fa-check",
            "Tildelinger": "fa fa-users",
            "Auditlog": "fa fa-table",
            "Cms": "fa fa-fw fa-cog",
            "Ext": "fa fa-plus",
            "Table": "fa fa-table"
        };
        let resultsContainer = $('#searchResults');
        // Clear and reset before getting new results
        resultsContainer.empty();
        currentIndex = -1;
        if (data && data.length > 0) {
            // Show 10 results max
            data.slice(0, 10).forEach(function (item, index) {
                let title;
                let icon;
                // Pre-process the text to add icons
                for (const [key, iconClass] of Object.entries(iconMap)) {
                    if (item.name.includes(key)) {
                        title = item.name.replace(`(${key})`, "");
                        icon = `<i class="${iconClass}"></i>`;
                        break;
                    }
                }
                let resultItem = $('<a>')
                    .addClass('list-group-item list-group-item-action')
                    .attr('href', item.url)
                    .attr('data-index', index)
                    .css('background-color', 'white')
                    .html(`<span>` + icon + `</span> &nbsp;` + title);
                resultsContainer.append(resultItem);
            });
            resultsContainer.show();
        } else {
            resultsContainer.append(
                $('<div>').addClass('list-group-item').text('No results found.').css('background-color', 'white')
            );
            resultsContainer.show();
        }
    }
    this.highlightItem = function (resultsItems) {
        // Clear highlighting made prior
        resultsItems.removeClass('active');
        $(resultsItems[currentIndex]).addClass('active').css('background-color', '#19aa8d');
    }

    this.triggerSearchOnClick = function () {
        if ($('#searchBar').val().length > 1) {
            searchService.onSearch();
        }
    }
}
