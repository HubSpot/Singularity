View = require './view'

class FileBrowserSubview extends View

    path = ''

    template: require '../templates/taskDetail/taskFileBrowser'

    events: ->
        'click [data-directory-path]':  'navigate'

    initialize: ({ @scrollWhenReady }) ->
        @listenTo @collection, 'sync',  @render
        @listenTo @collection, 'error', @catchAjaxError
        @listenTo @model, 'sync', @render
        @task = @model

        @scrollAfterRender = Backbone.history.fragment.indexOf('/files') isnt -1

    render: ->
        # Ensure we have enough space to scroll
        offset = @$el.offset().top

        breadcrumbs = utils.pathToBreadcrumbs @collection.currentDirectory

        emptySandboxMessage = 'No files exist in task directory.'

        if @task.get('taskUpdates') and @task.get('taskUpdates').length > 0
            switch _.last(@task.get('taskUpdates')).taskState
                when 'TASK_LAUNCHED', 'TASK_STAGING', 'TASK_STARTING' then emptySandboxMessage = 'Could not browse files. The task is still starting up.'
                when 'TASK_KILLED', 'TASK_FAILED', 'TASK_LOST', 'TASK_FINISHED' then emptySandboxMessage = 'No files exist in task directory. It may have been cleaned up.'

        @$el.html @template
            synced:                 @collection.synced and @task.synced
            files:                  _.pluck @collection.models, 'attributes'
            path:                   @collection.path
            breadcrumbs:            breadcrumbs
            task:                   @task
            emptySandboxMessage:    emptySandboxMessage

        # make sure body is large enough so we can fit the browser
        minHeight = @$el.offset().top + $(window).height()
        $('body').css 'min-height', "#{ minHeight }px"

        scroll = => $(window).scrollTop @$el.offset().top - 20
        if @scrollAfterRender
            @scrollAfterRender = false

            scroll()
            setTimeout scroll, 100

        @$('.actions-column a[title]').tooltip()

    catchAjaxError: ->
        app.caughtError()
        @render()

    navigate: (event) ->
        event.preventDefault()

        $table = @$ 'table'
        # Get table height for later
        if $table.length
            tableHeight = $table.height()

        path = $(event.currentTarget).data 'directory-path'
        @collection.path = "#{ path }"

        app.router.navigate "#task/#{ @collection.taskId }/files/#{ @collection.path }"

        @collection.fetch reset: true

        @render()

        @scrollWhenReady = true
        $loaderContainer = @$ '.page-loader-container'
        if tableHeight?
            $loaderContainer.css 'height', "#{ tableHeight }px"

module.exports = FileBrowserSubview
