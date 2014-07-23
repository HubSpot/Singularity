class Utils

    @humanizeFileSize: (fileSize) ->
        kilo = 1024
        mega = 1024 * 1024
        giga = 1024 * 1024 * 1024

        shorten = (which) -> Math.round fileSize / which

        if fileSize > giga
            return "#{ shorten giga }GB"
        else if fileSize > mega
            return "#{ shorten mega }MB"
        else if fileSize > kilo
            return "#{ shorten kilo }KB"
        else
            return "#{ fileSize }B"

    @stringJSON: (object) ->
        JSON.stringify object, null, '    '

    @viewJSON: (type, objectId) ->
        lookupObject = {}

        if type is 'task'
            lookupObject = app.allTasks
        if type is 'request'
            lookupObject = app.allRequests
        if type is 'deploy'
            lookupObject = app.allDeploys
        if type is 'requestHistory'
            lookupObject = app.allRequestHistories

        copyButton =
            text: "Copy"
            type: "button"
            className: "vex-dialog-button-secondary copy-button"
                
        vex.dialog.alert
            buttons: [
                $.extend({}, vex.dialog.buttons.YES, text: 'Done'),
                copyButton
            ]
            className: 'vex-theme-default vex-theme-default-json-view'
            message: "<pre>#{ utils.htmlEncode lookupObject[objectId].JSONString }</pre>"
            afterOpen: ($vexContent) ->
                utils.scrollPreventDefaultAtBounds $vexContent.find('pre')
                utils.scrollPreventAlways $vexContent.parent()
                
                # Dity hack to make ZeroClipboard play along
                # The Flash element doesn't work if it falls outside the
                # bounds of the body, even if it's inside the dialog
                overlayHeight = $(".vex-overlay").height()
                $("body").css "min-height", overlayHeight + "px"
                
                $button = $vexContent.find ".copy-button"
                $button.attr "data-clipboard-text", $vexContent.find("pre").html()
                
                zeroClipboardClient = new ZeroClipboard $button[0],
                    moviePath: "#{ config.appRoot }/static/swf/ZeroClipboard.swf"
                
                zeroClipboardClient.on "load", ->
                    zeroClipboardClient.on "complete", ->
                        $button.val "Copied"
                        setTimeout (-> $button.val "Copy"), 800

    @humanTime: (date, comparison = undefined, future = false) ->
        return '' unless date?
        now = if not comparison? then moment() else moment(comparison)
        time = moment(date)
        wasToday = time.date() is now.date() and Math.abs(time.diff(now)) < 86400000
        wasJustNow = Math.abs(time.diff(now)) < 120000
        """#{ if future then time.from() else time.from(now) } #{ if wasJustNow then '' else time.format('(' + (if wasToday then '' else 'l ') + 'h:mma)') }"""

    @humanTimeShort: (date) ->
        return '' unless date?
        now = moment()
        time = moment(date)
        wasToday = time.date() is now.date() and Math.abs(time.diff(now)) < 86400000
        wasJustNow = Math.abs(time.diff(now)) < 120000
        """#{ time.fromNow() }"""

    @humanTimeAgo: (date) ->
        utils.humanTime date

    @humanTimeSoon: (date) ->
        utils.humanTime date, undefined, true

    @flashRow: ($row) ->
        $row.removeClass('flash-prime flash')
        $row.addClass('flash-prime')
        $row[0].clientHeight
        $row.addClass('flash')

    # For .horizontal-description-list
    @setupCopyLinks: ($element) =>
        $items = $element.find ".horizontal-description-list li"
        _.each $items, ($item) =>
            $item = $($item)
            # Don't do it if there's already a button
            if not $item.find('a').length
                text = $item.find('p').html()
                $copyLink = $ "<a data-clipboard-text='#{ _.escape text }'>Copy</a>"
                $item.find("h4").append $copyLink
                new ZeroClipboard $copyLink[0],
                    moviePath: "#{ config.appRoot }/static/swf/ZeroClipboard.swf"

    @fixTableColumns: ($table) =>
        $headings = $table.find "th"
        if $headings.length and $table.css('table-layout') isnt 'fixed'
            # Reset any previous widths
            $table.css "table-layout", "auto"
            $headings.css "width", "auto"

            totalWidth = $table.width()
            for $heading in $headings
                $heading = $ $heading
                percentage = $heading.width() / totalWidth * 100
                # Set a %-width to each table heading based on current values
                $heading.css "width", "#{ percentage }%"

            # Set the table layout to be fixed based on these new widths
            $table.css "table-layout", "fixed"

    @pathToBreadcrumbs: (path) ->
        # a/b/c => [a, b, c]
        pathComponents = path.split '/'
        # [a, b, c] => [a, a/b, a/b/c]
        _.map pathComponents, (crumb, index) =>
            path = _.first pathComponents, index
            path.push crumb
            return { name: crumb, path: path.join '/' }

module.exports = Utils
