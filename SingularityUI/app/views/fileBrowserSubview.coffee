View = require './view'

class FileBrowserSubview extends View

    path = ''

    template: require '../templates/taskDetail/taskFileBrowser'

    events: ->
        'click [data-directory-path]':  'navigate'
        'click [data-action="shrink"]': 'startShrink'

    initialize: ({ @scrollWhenReady }) ->
        @listenTo @collection, 'sync',  @render
        @listenTo @collection, 'error', @catchAjaxError

    render: ->
        # Ensure we have enough space to scroll
        offset = @$el.offset().top

        breadcrumbs = utils.pathToBreadcrumbs @collection.path

        @$el.html @template
            synced:      @collection.synced
            files:       _.pluck @collection.models, 'attributes'
            path:        @collection.path
            breadcrumbs: breadcrumbs

        if @scrollWhenReady
            @scrollToTop()

            # Only do it once
            @scrollWhenReady = false

    catchAjaxError: ->
        app.caughtError()
        @render()

    scrollToTop: ->
        return if @suppressExpansion

        @$el.addClass 'expanded'

    navigate: (event) ->
        event.preventDefault()

        $table = @$ 'table'
        # Get table height for later
        if $table.length
            tableHeight = $table.height()

        path = $(event.currentTarget).data 'directory-path'
        if not @collection.path
            @collection.path = "#{ @collection.taskId }/#{ path }"
        else
            @collection.path = "#{ path }"

        app.router.navigate "#task/#{ @collection.taskId }/files/#{ @collection.path }"

        @collection.fetch reset: true

        @render()

        @scrollWhenReady = true
        $loaderContainer = @$ '.page-loader-container'
        if tableHeight?
            $loaderContainer.css 'height', "#{ tableHeight }px"

module.exports = FileBrowserSubview
