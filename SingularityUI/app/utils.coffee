class Utils
    
    @viewJSON: (model) ->
        if not model?
            console.error 'Invalid model given'
            return
        
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

        $(window).scrollTop offset - 20

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

            shrinkCallback()

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

module.exports = Utils
