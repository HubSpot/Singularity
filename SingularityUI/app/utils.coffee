class Utils

    @getHTMLTitleFromHistoryFragment: (fragment) ->
        _.capitalize(fragment.split('\/').join(' '))

    @getShortTaskID: (taskId) ->
        split = taskId.split(/\-|\:|\./)
        if split.length > 1
            return "#{ split[0] }..."
        else
            return _.truncate(taskId, 20)
        return taskId

    @getRequestNameFromID: (requestId) ->
        split = requestId.split(/\:/)
        if split.length > 1
            return "#{ split[0] }"
        else
            return requestId
        return requestId

    @getShortTaskIDMiddleEllipsis: (taskId) ->
        dateRegExp = /\-\d{12,}\-/
        bigSplit = taskId.split dateRegExp

        if bigSplit.length is 2
            "#{ utils.getShortTaskID(bigSplit[0]) }#{ taskId.match(dateRegExp)[0] }#{ _.truncate(bigSplit[1], 25) }"
        else
            utils.getShortTaskID taskId

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

    @scrollPreventDefaultAtBounds: ($scroll) ->
        $scroll.bind 'mousewheel', (e, delta) ->
            e.stopPropagation()

            if (@scrollTop >= ($scroll[0].scrollHeight - $scroll.outerHeight()) and delta < 0) or (@scrollTop is 0 and delta > 0)
                e.preventDefault()

    @scrollPreventAlways: ($scroll) ->
        $scroll.bind 'mousewheel', (e) ->
            e.preventDefault()

    @htmlEncode: (value) ->
        $('<div>').text(value).html()

    @setupSortableTables: ->
        # sortable.init()

    @handlePotentiallyEmptyFilteredTable: ($table, object = 'object', query = '') ->
        message = "No #{ object }s found matching "

        emptyTableInnerClass = 'empty-table-message'

        if query is ''
            message += 'that query'
        else
            message += """ "#{ query }" """

        if $table.find('tbody tr:not(".filtered")').length
            $table.removeClass('filtered').siblings(".#{ emptyTableInnerClass }").remove()

        else
            $emptyMessage = $table.siblings(".#{ emptyTableInnerClass }")

            if not $emptyMessage.length
                $table.addClass('filtered').after """
                    <div class="#{ emptyTableInnerClass }">
                        <p>#{ message }</p>
                    </div>
                """

            else
                $emptyMessage.find('p').html message

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

    @isScheduledRequest: (request) ->
        if _.isString(request.schedule) then true else false

    @isOnDemandRequest: (request) ->
        not request.daemon and not utils.isScheduledRequest(request)

    @spacifyObjectID: (id) ->
        id.replace(/([a-z]|(?:(?:^|[a-z])[AI])|^)([A-Z])/g, (match, first, second) -> "#{ first } #{ second }").replace(/[_:-]/g, ' ')

    @matchLowercaseOrWordsInWords: (query, string) ->
        queryWords = utils.spacifyObjectID(query).toLowerCase().split(' ')
        stringWords = utils.spacifyObjectID(string).toLowerCase().split(' ')

        return true if string?.toLowerCase().indexOf(query?.toLowerCase()) >= 0

        for word in queryWords
            found = false
            for stringWord in stringWords
                if _.string.contains stringWord, word
                    found = true
                    break
            if found is true
                found is false
            else
                return false
        return true

    @setupCopyPre: ($pre) ->
        if $pre.hasClass 'with-copy-button'
            return

        $pre.addClass 'with-copy-button'

        $copyButton = $ """<button data-copy-button></button>"""
        $copyButton.attr('data-clipboard-text', $pre.html())

        $pre.prepend $copyButton

        zeroClipboardClient = new ZeroClipboard $copyButton[0],
            moviePath: "#{ config.appRoot }/static/swf/ZeroClipboard.swf"
            forceHandCursor: true

        copiedTimeout = undefined

        zeroClipboardClient.on 'load', ->
            zeroClipboardClient.on 'complete', ->
                clearTimeout copiedTimeout
                $pre[0].clientHeight
                $pre.addClass 'copied'
                copiedTimeout = setTimeout ->
                    $pre.removeClass 'copied'
                , 600

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
