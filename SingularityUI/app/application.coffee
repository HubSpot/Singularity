Router = require 'lib/router'

State = require 'models/State'

RequestsActive = require 'collections/RequestsActive'
RequestsPaused = require 'collections/RequestsPaused'
RequestsPending = require 'collections/RequestsPending'
RequestsCleaning = require 'collections/RequestsCleaning'

RequestsStarred = require 'collections/RequestsStarred'

TasksActive = require 'collections/TasksActive'
TasksScheduled = require 'collections/TasksScheduled'
TasksCleaning = require 'collections/TasksCleaning'

class Application

    initialize: ->
        app.isMobile = touchDevice = 'ontouchstart' of document.documentElement
        app.setupGlobalErrorHandling()

        app.$page = $('#page')
        app.page = app.$page[0]

        @views = {}
        @collections = {}

        @allTasks = {}
        @allRequests = {}

        @fetchResources =>

            $('.page-loader.fixed').hide()

            @router = new Router

            Backbone.history.start
                pushState: location.hostname.substr(0, 'local'.length).toLowerCase() isnt 'local'
                root: '/singularity/'

            Object.freeze? @

    setupGlobalErrorHandling: ->
        $(document).on 'ajaxError', (event, jqxhr, settings) ->
            unless settings.suppressErrors
                vex.dialog.alert "<p>A <code>#{ jqxhr.statusText }</code> error occurred when trying to access:</p><pre>#{ settings.url }</pre><p>The request had status code <code>#{ jqxhr.status }</code>.</p><p>Here's the full <code>jqxhr</code> object:</p><pre>#{ utils.stringJSON jqxhr }</pre>"

    show: (view) ->
        if app.page.children.length
            app.page.replaceChild view.el, app.page.children[0]
        else
            app.page.appendChild view.el

    fetchResources: (success) ->
        @resolveCountdown = 0

        @collections.requestsStarred = new RequestsStarred
        @collections.requestsStarred.fetch()

        resolve = =>
            @resolveCountdown -= 1
            success() if @resolveCountdown is 0

        @resolveCountdown += 1
        @state = new State
        @state.fetch
            suppressErrors: true
            error: -> vex.dialog.alert('An error occurred while trying to load the Singularity state.')
            success: -> resolve()

        resources = [{
            collection_key: 'requestsActive'
            collection: RequestsActive
            error_phrase: 'requests'
        }, {
            collection_key: 'requestsPaused'
            collection: RequestsPaused
            error_phrase: 'paused requests'
        }, {
            collection_key: 'requestsPending'
            collection: RequestsPending
            error_phrase: 'pending requests'
        }, {
            collection_key: 'requestsCleaning'
            collection: RequestsCleaning
            error_phrase: 'cleaning requests'
        }, {
            collection_key: 'tasksActive'
            collection: TasksActive
            error_phrase: 'active tasks'
        }, {
            collection_key: 'tasksScheduled'
            collection: TasksScheduled
            error_phrase: 'scheduled tasks'
        }, {
            collection_key: 'tasksCleaning'
            collection: TasksCleaning
            error_phrase: 'cleaning tasks'
        }]

        _.each resources, (r) =>
            @resolveCountdown += 1
            @collections[r.collection_key] = new r.collection
            @collections[r.collection_key].fetch
                suppressErrors: true
                error: (e) ->
                    vex.dialog.alert("An error occurred while trying to load Singularity #{ r.error_phrase }.")
                    resolve()
                success: ->
                    resolve()

module.exports = new Application
