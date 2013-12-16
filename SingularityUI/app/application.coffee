Router = require 'lib/router'

State = require 'models/State'

RequestsActive = require 'collections/RequestsActive'
RequestsPaused = require 'collections/RequestsPaused'
RequestsPending = require 'collections/RequestsPending'
RequestsCleaning = require 'collections/RequestsCleaning'

TasksActive = require 'collections/TasksActive'
TasksScheduled = require 'collections/TasksScheduled'
TasksCleaning = require 'collections/TasksCleaning'

class Application

    initialize: =>
        @views = {}
        @collections = {}

        @allTasks = {}
        @allRequests = {}

        # Get users, projects, and targets, and user settings
        # before actually starting the app
        @fetchResources =>

            $('.page-loader.fixed').hide()

            @router = new Router

            Backbone.history.start
                pushState: location.hostname.substr(0, 'local'.length).toLowerCase() isnt 'local'
                root: '/singularity/'

            Object.freeze? @

    fetchResources: (success) =>
        @resolveCountdown = 0

        resolve = =>
            @resolveCountdown -= 1
            success() if @resolveCountdown is 0

        @resolveCountdown += 1
        @state = new State
        @state.fetch
            error: => vex.dialog.alert('An error occurred while trying to load the Singularity state.')
            success: -> resolve()

        resources = [{
            collection_key: 'requestsActive'
            collection: RequestsActive
            error_phrase: 'requests'
        }, {
            collection_key: 'requestsPending'
            collection: RequestsPending
            error_phrase: 'pending requests'
        }, {
            collection_key: 'requestsCleaning'
            collection: RequestsCleaning
            error_phrase: 'cleaning requests'
        }, {
            collection_key: 'requestsPaused'
            collection: RequestsPaused
            error_phrase: 'paused requests'
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
                error: ->
                    vex.dialog.alert("An error occurred while trying to load Singularity #{ r.error_phrase }.")
                    resolve()
                success: ->
                    resolve()

module.exports = new Application
