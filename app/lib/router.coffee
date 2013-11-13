DashboardView = require 'views/dashboard'
RequestsView = require 'views/requests'
TasksView = require 'views/tasks'
PageNotFoundView = require 'views/page_not_found'
NavigationView = require 'views/navigation'

nav = ->
    if not app.views.navigationView?
        app.views.navigationView = new NavigationView
    app.views.navigationView.render()

class Router extends Backbone.Router

    routes:
        '(/)': 'dashboard'
        'requests(/)': 'requests'
        'tasks(/)': 'tasks'
        '*anything': 'templateFromURLFragment'

    dashboard: ->
        nav()
        if not app.views.dashboard?
            app.views.dashboard = new DashboardView
        app.views.current = app.views.dashboard
        app.views.dashboard.render()

    requests: ->
        nav()
        if not app.views.requests?
            app.views.requests = new RequestsView
        app.views.current = app.views.requests
        app.views.requests.render()

    tasks: ->
        nav()
        if not app.views.tasks?
            app.views.tasks = new TasksView
        app.views.current = app.views.tasks
        app.views.tasks.render()

    templateFromURLFragment: ->
        nav()
        app.views.current = undefined

        template = undefined
        try
            template = require "../views/templates/#{Backbone.history.fragment}"
        catch error

        if template
            $('body > .app').html template
            return

        @show404()

    show404: ->
        nav()
        if not app.views.pageNotFound?
            app.views.pageNotFound = new PageNotFoundView
        app.views.current = app.views.pageNotFound
        app.views.pageNotFound.render()

module.exports = Router