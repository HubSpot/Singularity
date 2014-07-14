View = require './view'

class FileBrowserSubview extends View

    path = ''

    template: require './templates/taskFileBrowser'

    events: ->
        'click [data-directory-path]': 'navigate'

    initialize: ->
        @listenTo @collection, 'sync', @render
        @listenTo @collection, 'error', @catchAjaxError

    render: ->
        # a/b/c => [a, b, c]
        pathComponents = @collection.path.split '/'
        # [a, b, c] => [a, a/b, a/b/c]
        breadcrumbs = _.map pathComponents, (crumb, index) =>
            path = _.first pathComponents, index
            path.push crumb
            return { name: crumb, path: path.join '/' }

        @$el.html @template
            synced:      @collection.synced
            files:       _.pluck @collection.models, 'attributes'
            path:        @collection.path
            breadcrumbs: breadcrumbs

    catchAjaxError: ->
        @$('.span-12').html '<h3>Could not get files :(</h3>'

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

        @collection.reset()
        @collection.fetch()

        @render()

        $loaderContainer = @$ '.page-loader-container'
        if tableHeight?
            $loaderContainer.css 'height', "#{ tableHeight }px"

module.exports = FileBrowserSubview