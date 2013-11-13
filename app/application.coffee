Login = require 'lib/login'
Router = require 'lib/router'

UserSettings = require 'models/UserSettings'
State = require 'models/State'

Requests = require 'collections/Requests'
TasksActive = require 'collections/TasksActive'
TasksScheduled = require 'collections/TasksScheduled'

class Application

    initialize: =>
        @login = new Login env

        @views = {}
        @collections = {}

        @login.verifyUser (data) =>
            # Pretend to be somebody else :)
            # @hubspot.context.user.email = 'email@hubspot.com'

            # Get users, projects, and targets, and user settings
            # before actually starting the app
            @fetchResources =>

                $('.page-loader').hide()

                @router = new Router

                Backbone.history.start
                    pushState: false
                    root: '/singularity/'

                Object.freeze? @

    fetchResources: (success) =>
        @resolve_countdown = 0

        resolve = =>
            @resolve_countdown -= 1
            success() if @resolve_countdown is 0

        @resolve_countdown += 1
        @user_settings = new UserSettings
        @user_settings.fetch
            error: => vex.dialog.alert("An error occurred while trying to load settings for <b>#{ @login.context.user.login }</b>.")
            success: -> resolve()

        @resolve_countdown += 1
        @state = new State
        @state.fetch
            error: => vex.dialog.alert('An error occurred while trying to load the Singularity state.')
            success: -> resolve()

        resources = [{
            collection_key: 'requests'
            collection: Requests
            error_phrase: 'requests'
        }, {
            collection_key: 'tasksActive'
            collection: TasksActive
            error_phrase: 'active tasks'
        }, {
            collection_key: 'tasksScheduled'
            collection: TasksScheduled
            error_phrase: 'scheduled tasks'
        }]

        _.each resources, (r) =>
            @resolve_countdown += 1
            @collections[r.collection_key] = new r.collection
            @collections[r.collection_key].fetch
                error: -> vex.dialog.alert("An error occurred while trying to load Singularity #{ r.error_phrase }.")
                success: -> resolve()

module.exports = new Application