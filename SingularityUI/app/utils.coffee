ZeroClipboard = require 'zeroclipboard'
vex = require 'vex.dialog'

class Utils

    # Constants
    @TERMINAL_TASK_STATES: ['TASK_KILLED', 'TASK_LOST', 'TASK_FAILED', 'TASK_FINISHED']
    @DECOMMISION_STATES: ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'DECOMISSIONING', 'DECOMISSIONED', 'STARTING_DECOMISSION']

    @viewJSON: (model, callback) ->
        if not model?
            callback?({error: 'Invalid model given'})
            console.error 'Invalid model given'
            return

        # We want to fetch the model before we display it if it
        # hasn't been fetched yet
        if model.synced? and not model.synced
            vex.showLoading()

            ajaxRequest = model.fetch()
            ajaxRequest.done  => @viewJSON model
            ajaxRequest.error =>
                app.caughtError()
                @viewJSON new Backbone.Model
                    message: "There was an error with the server"
            return

        vex.hideLoading()
        # We'll want to take any ignored attributes out of the object first
        if not model.ignoreAttributes?
            # Always ignore `id`
            model.ignoreAttributes = ['id']

        objectToSerialise = {}
        modelJSON = model.toJSON()

        for key in _.keys modelJSON
            if key not in model.ignoreAttributes
                objectToSerialise[key] = modelJSON[key]

        json = JSON.stringify objectToSerialise, undefined, 4

        closeButton = _.extend _.clone(vex.dialog.buttons.YES), text: 'Close'
        copyButton =
            text: "Copy"
            type: "button"
            className: "vex-dialog-button-secondary copy-button"

        vex.dialog.open
            buttons:   [closeButton, copyButton]
            message:   "<pre>#{ _.escape json }</pre>"
            className: 'vex vex-theme-default json-modal'

            afterOpen: ($vexContent) ->
                $vexContent.parents('.vex').scrollTop 0

                # Dity hack to make ZeroClipboard play along
                # The Flash element doesn't work if it falls outside the
                # bounds of the body, even if it's inside the dialog
                overlayHeight = $vexContent.parents(".vex-overlay").height()
                $("body").css "min-height", overlayHeight + "px"

                $button = $vexContent.find ".copy-button"
                $button.attr "data-clipboard-text", $vexContent.find("pre").html()

                zeroClipboardClient = new ZeroClipboard $button[0]

                zeroClipboardClient.on "ready", =>
                    zeroClipboardClient.on "aftercopy", =>
                        $button.val "Copied"
                        setTimeout (-> $button.val "Copy"), 800

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
                new ZeroClipboard $copyLink[0]

    # Copy anything
    @makeMeCopy: (options) =>
        $element = $(options.selector)
        linkText = options.linkText || 'Copy'
        textSelector = options.textSelector || '.copy-text'

        text = $element.find(textSelector).html()
        $copyLink = $ "<a data-clipboard-text='#{ _.escape text }'>#{linkText}</a>"
        $(options.copyLink).html($copyLink)
        new ZeroClipboard $copyLink[0]

    @fixTableColumns: ($table) =>
        $headings = $table.find "th"
        if $headings.length and $table.css('table-layout') isnt 'fixed'
            # Reset any previous widths
            $table.css "table-layout", "auto"
            $headings.css "width", "auto"

            totalWidth = $table.width()
            sortable = $table.attr('data-sortable') != undefined
            if not sortable
                for $heading in $headings
                    $heading = $ $heading
                    percentage = $heading.width() / totalWidth * 100
                    # Set a %-width to each table heading based on current values
                    $heading.css "width", "#{ percentage }%"

                # Set the table layout to be fixed based on these new widths
                $table.css "table-layout", "fixed"

    @pathToBreadcrumbs: (path="") ->
        pathComponents = path.split '/'
        # [a, b, c] => [a, a/b, a/b/c]
        results = _.map pathComponents, (crumb, index) =>
            path = _.first pathComponents, index
            path.push crumb
            return { name: crumb, path: path.join '/' }
        results.unshift { name: "root", path: "" }
        results


    # Will make $el as tall as the page and will attach a scroll event
    # that shrinks it
    @animatedExpansion: ($el, shrinkCallback) ->
        newHeight = $(window).height()
        offset = $el.offset().top

        # Resize body so we can scroll
        $('body').css 'min-height', "#{ offset + newHeight }px"

        $el.css 'min-height', "#{ $el.height() }px"
        $el.animate
            duration:  1000
            minHeight: "#{ newHeight }px"

        scroll = => $(window).scrollTop $el.offset().top - 20
        scroll()
        setTimeout scroll, 200

        shrinkTime = 1000

        removeEvent = =>
            $(window).off 'scroll', checkForShrink

        shrink = =>
            # $('html').css 'min-height', '0'
            $('html, body').animate
                minHeight: '0px'
            , shrinkTime

            $el.animate
                minHeight: '0px'
            , shrinkTime

            shrinkCallback?()

            removeEvent()

        checkForShrink = =>
            cancelAnimationFrame frameRequest if frameRequest?
            frameRequest = requestAnimationFrame =>
                removeEvent() if not $el

                $window = $(window)

                lastChild = $ _.last $el.children()
                shrink() if not lastChild

                elOffset           = $el.offset().top
                childScrollBottom  = lastChild.height() + lastChild.offset().top
                windowScrollTop    = $window.scrollTop()
                windowScrollBottom = $window.height() + windowScrollTop

                scrolledEnoughTop    = windowScrollTop < elOffset - 50
                scrolledEnoughBottom = windowScrollTop > elOffset + 50
                scrolledEnoughBottom = scrolledEnoughBottom and windowScrollBottom > childScrollBottom
                shouldShrink = scrolledEnoughTop or scrolledEnoughBottom

                shrink() if shouldShrink

        setTimeout =>
            $(window).on 'scroll', checkForShrink
            $el.on       'shrink', shrink
        , 100


    # Will scroll to a DOM node via a passed in jQuery selector
    # `offset` is an optional pixel offset from the selector
    @scrollTo: (path, offset=50) ->
        location = $("#{path}").offset().top - offset
        $('html, body').animate 'scrollTop' : location+'px', 1000

    @getQueryParams: ->
        if location.search
          JSON.parse('{"' + decodeURI(location.search.substring(1).replace(/&/g, "\",\"").replace(/\=/g,"\":\"")) + '"}')
        else
          {}

    @fuzzyAdjustScore: (filter, fuzzyObject) ->
        if fuzzyObject.original.id.toLowerCase().startsWith(filter.toLowerCase())
            fuzzyObject.score * 10
        else if fuzzyObject.original.id.toLowerCase().indexOf(filter.toLowerCase()) > -1
            fuzzyObject.score * 5
        else
            fuzzyObject.score


module.exports = Utils
