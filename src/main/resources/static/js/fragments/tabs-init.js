$(document).ready(function () {
	// on load of the page: switch to the currently selected tab
	var hash = window.location.hash;
	if (hash && /^#[\w-]+$/.test(hash)) {
		$('.rememberPosition a[href="' + hash + '"]').tab('show');
	}
});

$('.rememberPosition a').click(function(e) {
	e.preventDefault();
	$(this).tab('show');
});

// store the currently selected tab in the hash value
$("ul.rememberPosition > li > a").on("shown.bs.tab", function(e) {
	var id = $(e.target).attr("href").substr(1);
	window.location.hash = id;
	window.scrollTo(0, 0); // Prevent scroll to tab
});