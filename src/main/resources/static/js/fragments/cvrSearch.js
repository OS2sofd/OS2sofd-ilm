var token = $("meta[name='_csrf']").attr("content");
var searchService;

$(document).ready(function() {
    searchService = new SearchService();
    searchService.init();
});

function SearchService() {
    var currentIndex = -1;
    var resultsHidden = false;
    this.init = function () {
        // focus searchbar on double-shift
        var lastShift = 0;
        window.addEventListener('keydown', function(event) {
            if( event.keyCode == 16 ) {
                if( new Date().getTime() - lastShift < 500 ) {
                    $('#searchBar').focus();
                }
                else {
                    lastShift = new Date().getTime();
                }
            }
            else {
                lastShift = 0;
            }
        });
    }

    this.onSearch = function () {
        var token = $("meta[name='_csrf']").attr("content");
        if ($('#searchBar').val().length > 1) {
            $.ajax({
                url: "/rest/search/" + $('#searchBar').val(),
                headers: {
                    'X-CSRF-TOKEN': token
                },
                type: 'GET',
                contentType: 'application/json',
                success: function (data, textStatus, jQxhr) {
                    searchService.displayResults(data);
                }
            });
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
        var resultsContainer = $('#searchResults');
        // Clear and reset before getting new results
        resultsContainer.empty();
        currentIndex = -1;
        resultsHidden = false;
        if (data && data.length > 0) {
            // Show 10 results max
            data.slice(0, 10).forEach(function (item, index) {
                var title;
                var icon;
                // Pre-process the text to add icons
                for (const [key, iconClass] of Object.entries(iconMap)) {
                    if (item.name.includes(key)) {
                        title = item.name.replace(`(${key})`, "");
                        icon = `<i class="${iconClass}"></i>`;
                        break;
                    }
                }
                var resultItem = $('<a>')
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
    this.handleKeyNavigation = function (event) {
        var resultsItems = $('#searchResults .list-group-item-action');
        // When we hide the results and press arrow, we initialise the search again
        if (resultsHidden && (event.key === 'ArrowDown' || event.keyCode == 40)) {
            onSearch();
            return;
        }
        if (resultsItems.length === 0) return;
        if (event.key === 'ArrowDown' || event.keyCode == 40) {
            event.preventDefault();
            if (currentIndex < resultsItems.length - 1) {
                currentIndex++;
                searchService.highlightItem(resultsItems);
            }
        }
        else if (event.key === 'ArrowUp' || event.keyCode == 38) {
            event.preventDefault();
            if (currentIndex > 0) {
                currentIndex--;
                searchService.highlightItem(resultsItems);
            }
        }
        else if ((event.key === 'Enter' || event.keyCode == 13) && currentIndex > -1) {
            event.preventDefault();
            window.location.href = $(resultsItems[currentIndex]).attr('href');
        }
        else if (event.key === 'Escape' || event.keyCode == 27) {
            $('#searchResults').hide();
            resultsHidden = true;
            currentIndex = -1;
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