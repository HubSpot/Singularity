React = require 'react'
ReactDOM = require 'react-dom'
View = require './view'
TaskFileBrowser = require '../components/taskDetail/TaskFileBrowser'

class FileBrowserSubview extends View

    path = ''

    events: ->
        'click [data-directory-path]':  'navigate'

    initialize: ({ @scrollWhenReady, @slaveOffline }) ->
        @listenTo @collection, 'sync',  @render
        @listenTo @collection, 'error', @catchAjaxError
        @listenTo @model, 'sync', @render
        @task = @model

        @scrollAfterRender = Backbone.history.fragment.indexOf('/files') isnt -1

    render: ->
        # Ensure we have enough space to scroll
        offset = @$el.offset().top

        breadcrumbs = utils.pathToBreadcrumbs @collection.currentDirectory

        ReactDOM.render(
            <TaskFileBrowser
                synced = {@collection.synced and @task.synced}
                files = {_.pluck @collection.models, 'attributes'}
                collection = {@collection}
                path = {@collection.path}
                breadcrumbs = {breadcrumbs}
                task = {@task}
            />
            ,@el)

        scroll = => $(window).scrollTop @$el.offset().top - 20
        if @scrollAfterRender
            @scrollAfterRender = false

            scroll()
            setTimeout scroll, 100

        @$('.actions-column a[title]').tooltip()

    catchAjaxError: ->
        app.caughtError()
        @render()

module.exports = FileBrowserSubview
