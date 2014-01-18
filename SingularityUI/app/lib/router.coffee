DashboardView = require 'views/dashboard'
SearchView = require 'views/search'
RequestsView = require 'views/requests'
RequestView = require 'views/request'
TasksView = require 'views/tasks'
TaskView = require 'views/task'
RacksView = require 'views/racks'
SlavesView = require 'views/slaves'
WebhooksView = require 'views/webhooks'
PageNotFoundView = require 'views/pageNotFound'
NavigationView = require 'views/navigation'
FilesView = require 'views/files'
TailView = require 'views/tail'

Backbone.history.on 'route', ->
    nav()
    globalRefresh()

nav = ->
    if not app.views.navigationView?
        app.views.navigationView = new NavigationView
    app.views.navigationView.render()

window.globalRefreshTimeout = undefined
globalRefresh = =>
    clearTimeout(window.globalRefreshTimeout) if window.globalRefreshTimeout
    window.globalRefreshTimeout = setInterval ->
        if not $('body > .vex').length
            app.views.current?.refresh?()
    , 10 * 1000

class Router extends Backbone.Router

    routes:
        '(/)': 'dashboard'
        'search(/)': 'search'
        'requests(/)': 'requests'
        'requests/:requestsFilter(/)': 'requestsFiltered'
        'request/:requestId(/)': 'request'
        'tasks(/)': 'tasks'
        'tasks/:tasksFilter(/)': 'tasksFiltered'
        'task/:taskId(/)': 'task'
        'task/:taskId/files(/)': 'files'
        'task/:taskId/files/*path': 'files'
        'task/:taskId/tail/*path': 'tail'
        'racks(/)': 'racks'
        'slaves(/)': 'slaves'
        'webhooks(/)': 'webhooks'
        '*anything': 'templateFromURLFragment'

    dashboard: ->
        if not app.views.dashboard?
            app.views.dashboard = new DashboardView
        app.views.current = app.views.dashboard
        app.views.dashboard.render().refresh(fromRoute = true)

    search: ->
        if not app.views.search?
            app.views.search = new SearchView
        app.views.current = app.views.search
        app.views.search.render()

    requests: ->
        @requestsFiltered 'active'

    requestsFiltered: (requestsFilter) ->
        if not app.views.requests?
            app.views.requests = new RequestsView requestsFilter: requestsFilter
        app.views.current = app.views.requests
        app.views.requests.render(requestsFilter).refresh()

    request: (requestId) ->
        app.views.requestViews = {} if not app.views.requestViews
        if not app.views.requestViews[requestId]
            app.views.requestViews[requestId] = new RequestView requestId: requestId
        app.views.current = app.views.requestViews[requestId]
        app.views.requestViews[requestId].render()

    tasks: ->
        @tasksFiltered 'active'

    tasksFiltered: (tasksFilter) ->
        if not app.views.tasks?
            app.views.tasks = new TasksView tasksFilter: tasksFilter
        app.views.current = app.views.tasks
        app.views.tasks.render(tasksFilter).refresh()

    task: (taskId) ->
        app.views.taskViews = {} if not app.views.taskViews
        if not app.views.taskViews[taskId]
            app.views.taskViews[taskId] = new TaskView taskId: taskId
        app.views.current = app.views.taskViews[taskId]
        app.views.taskViews[taskId].render()

    files: (taskId, path='') ->
        app.views.filesViews = {} if not app.views.filesViews
        if not app.views.filesViews[taskId]
            app.views.filesViews[taskId] = new FilesView taskId: taskId, path: path
        else
            app.views.filesViews[taskId].browse path
        app.views.current = app.views.filesViews[taskId]
        app.views.filesViews[taskId].render()

    tail: (taskId, path='') ->
        app.views.tailViews = {} if not app.views.tailViews
        if not app.views.tailViews[taskId] or app.views.tailViews[taskId].path isnt path
            app.views.tailViews[taskId] = new TailView taskId: taskId, path: path
        app.views.current = app.views.tailViews[taskId]
        app.views.tailViews[taskId].render()

    racks: ->
        if not app.views.racks?
            app.views.racks = new RacksView
        app.views.current = app.views.racks
        app.views.racks.render().refresh()

    slaves: ->
        if not app.views.slaves?
            app.views.slaves = new SlavesView
        app.views.current = app.views.slaves
        app.views.slaves.render().refresh()

    webhooks: ->
        if not app.views.webhooks?
            app.views.webhooks = new WebhooksView
        app.views.current = app.views.webhooks
        app.views.webhooks.render().refresh()

    templateFromURLFragment: ->
        app.views.current = undefined

        template = undefined
        try
            template = require "../views/templates/#{ Backbone.history.fragment }"
        catch error

        if template
            $('body > .app').html template
            return

        @show404()

    show404: ->
        if not app.views.pageNotFound?
            app.views.pageNotFound = new PageNotFoundView
        app.views.current = app.views.pageNotFound
        app.views.pageNotFound.render()

module.exports = Router