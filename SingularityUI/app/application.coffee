Router = require 'lib/router'

User = require 'models/User'
State = require 'models/State'

RequestsAll = require "collections/RequestsAll"
RequestsActive = require 'collections/RequestsActive'
RequestsCooldown = require 'collections/RequestsCooldown'
RequestsPaused = require 'collections/RequestsPaused'
RequestsPending = require 'collections/RequestsPending'
RequestsCleaning = require 'collections/RequestsCleaning'

RequestsStarred = require 'collections/RequestsStarred'

TasksActive = require 'collections/TasksActive'
TasksScheduled = require 'collections/TasksScheduled'
TasksCleaning = require 'collections/TasksCleaning'

NavView = require 'views/nav'
GlobalSearchView = require 'views/globalSearch'

class Application

    initialize: ->
        @isMobile = touchDevice = 'ontouchstart' of document.documentElement
        @setupGlobalErrorHandling()

        @setupUser()

        @$page = $('#page')
        @page = @$page[0]

        @views = {}
        @collections = {}

        @allTasks = {}
        @allRequests = {}
        @allDeploys = {}
        @allRequestHistories = {}

        @setupAppCollections()
        @setupNav()
        @setupGlobalSearchView()

        $('.page-loader.fixed').hide()

        @router = new Router

        # so sneaky
        el = document.createElement('a')
        el.href = config.appRoot

        Backbone.history.start
            pushState: true
            root: el.pathname

        Object.freeze? @

    setupGlobalErrorHandling: ->
        unloading = false
        $(window).on 'beforeunload', ->
            unloading = true
            return

        blurred = false
        $(window).on 'blur', -> blurred = true
        $(window).on 'focus', -> blurred = false

        $(document).on 'ajaxError', (e, jqxhr, settings) ->
            return if settings.suppressErrors
            return if jqxhr.statusText is 'abort'
            return if unloading
            return if blurred and jqxhr.statusText is 'timeout'

            url = settings.url.replace(config.appRoot, '')

            if jqxhr.status is 502
                Messenger().post "<p>A request failed because Singularity is deploying. Things should resolve in a few seconds so just hang tight...</p>"
            else if jqxhr.statusText is 'timeout'
                Messenger().post "<p>A <code>#{ jqxhr.statusText }</code> error occurred while accessing:</p><pre>#{ url }</pre>"
            else
                console.error "AJAX Error response"
                console.error jqxhr
                Messenger().post "<p>An error occurred when trying to access:</p><pre>#{ url }</pre><p>Check JS console for response.</p>"
                
    show: (view) ->
        if @page.children.length
            @page.replaceChild view.el, @page.children[0]
        else
            @page.appendChild view.el

    setupAppCollections: ->
        @collections.requestsStarred = new RequestsStarred
        @collections.requestsStarred.fetch() # Syncronous because it uses localStorage

        @state = new State

        resources = [{
            collection_key: 'requestsAll'
            collection: RequestsAll
            error_phrase: 'all requests'
        },  {
            collection_key: 'requestsActive'
            collection: RequestsActive
            error_phrase: 'active requests'
        },  {
            collection_key: 'requestsCooldown'
            collection: RequestsCooldown
            error_phrase: 'requests on cooldown'
        },  {
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
            @collections[r.collection_key] = new r.collection

    setupUser: ->
        @user = new User
        @user.fetch() # Syncronous because it uses localStorage
        @user.set(@user.get('0')) # Hack because the Backbone.LocalStorage adapter I use is jank

        if not @user.get('deployUser')
            Backbone.history.once 'route', =>
                setTimeout (=> @deployUserPrompt(welcome = true)), 1000

    getUsername: =>
        @user.get "deployUser" or "Unknown"

    deployUserPrompt: (welcome) ->
        vex.dialog.prompt
            message: """
                <h2>Set a deploy user</h2>
                #{ if welcome then '<p>Now you can set your deploy user (as a cookie) for a more tailored experience!</p>' else '' }
                <p>What deploy user would you like to view Singularity as?</p>
            """
            value: @user.get('deployUser')
            placeholder: 'user'
            afterOpen: ($vexContent) ->
                $vexContent.find('input[type="text"]').focus()
            callback: (user) =>
                if _.isString(user) and user isnt ''
                    @user.set('deployUser', @user.deployUser = user)
                    @user.save()

    setupNav: ->
        @views.nav = new NavView
        @views.nav.render()

    setupGlobalSearchView: ->
        @views.globalSearch = new GlobalSearchView
        @views.globalSearch.render()

module.exports = new Application
