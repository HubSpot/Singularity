DashboardView = require 'views/dashboard'
SearchView = require 'views/search'
RequestsView = require 'views/requests'
RequestView = require 'views/request'
TasksView = require 'views/tasks'
TaskView = require 'views/task'
FilesView = require 'views/files'
TailView = require 'views/tail'
StatusView = require 'views/status'
RacksView = require 'views/racks'
SlavesView = require 'views/slaves'
WebhooksView = require 'views/webhooks'
PageNotFoundView = require 'views/pageNotFound'
NavigationView = require 'views/navigation'

currentViewIsTailView = ->
    Backbone.history.fragment.match(/^task\/.+\/tail\//)?.length is 1

htmlClasses = ->
    if currentViewIsTailView()
        $('html').addClass('tail-view')
    else
        $('html').removeClass('tail-view')

Backbone.history.on 'route', ->
    nav()
    globalRefresh()
    htmlClasses()

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
    , 20 * 1000

class Router extends Backbone.Router

    routes:
        '(/)': 'dashboard'
        'search(/)': 'search'
        'status(/)': 'status'
        'requests(/)': 'requestsFiltered'
        'requests/:requestsFilter/:requestsSubFilter(/)': 'requestsFiltered'
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
        app.show app.views.dashboard.render()

    search: ->
        if not app.views.search?
            app.views.search = new SearchView
            app.views.current = app.views.search
            app.show app.views.search.render()
        else
            app.views.current = app.views.search
            app.show app.views.search

    status: ->
        if not app.views.status?
            app.views.status = new StatusView
        app.views.current = app.views.status
        app.show app.views.status.refresh(fromRoute = true)

    requestsFiltered: (requestsFilter = 'active', requestsSubFilter = 'active') ->
        if not app.views.requests?
            app.views.requests = new RequestsView { requestsFilter, requestsSubFilter }

        if app.views.requests is app.views.current and @lastRequestsFilter is requestsFilter
            app.show app.views.requests.render(requestsFilter, requestsSubFilter)
        else
            @lastRequestsFilter = requestsFilter
            app.views.current = app.views.requests
            app.show app.views.requests.render(requestsFilter, requestsSubFilter).refresh()

    request: (requestId) ->
        app.views.requestViews = {} if not app.views.requestViews
        if not app.views.requestViews[requestId]
            app.views.requestViews[requestId] = new RequestView requestId: requestId
            app.views.current = app.views.requestViews[requestId]
            app.show app.views.requestViews[requestId].render()
        else
            app.views.current = app.views.requestViews[requestId]
            app.show app.views.requestViews[requestId]

    tasks: ->
        @tasksFiltered 'active'

    tasksFiltered: (tasksFilter) ->
        if not app.views.tasks?
            app.views.tasks = new TasksView tasksFilter: tasksFilter
        app.views.current = app.views.tasks
        app.show app.views.tasks.render(tasksFilter).refresh()

    task: (taskId) ->
        app.views.taskViews = {} if not app.views.taskViews
        if not app.views.taskViews[taskId]
            app.views.taskViews[taskId] = new TaskView taskId: taskId
        app.views.current = app.views.taskViews[taskId]
        app.show app.views.taskViews[taskId].render().refresh()

    files: (taskId, path = '') ->
        app.views.filesViews = {} if not app.views.filesViews
        if not app.views.filesViews[taskId] or app.views.filesViews[taskId].path isnt path
            app.views.filesViews[taskId] = new FilesView taskId: taskId, path: path
        else
            app.views.filesViews[taskId].browse path
        app.views.current = app.views.filesViews[taskId]
        app.show app.views.filesViews[taskId].render()

    tail: (taskId, path = '') ->
        app.views.tailViews = {} if not app.views.tailViews
        if not app.views.tailViews[taskId] or app.views.tailViews[taskId].path isnt path
            app.views.tailViews[taskId] = new TailView taskId: taskId, path: path
        app.views.current = app.views.tailViews[taskId]
        app.show app.views.tailViews[taskId].render()

    racks: ->
        if not app.views.racks?
            app.views.racks = new RacksView
            app.views.current = app.views.racks
            app.show app.views.racks.render().refresh()
        else
            app.views.current = app.views.racks
            app.show app.views.racks.refresh()

    slaves: ->
        if not app.views.slaves?
            app.views.slaves = new SlavesView
            app.views.current = app.views.slaves
            app.show app.views.slaves.render().refresh()
        else
            app.views.current = app.views.slaves
            app.show app.views.slaves.refresh()

    webhooks: ->
        if not app.views.webhooks?
            app.views.webhooks = new WebhooksView
            app.views.current = app.views.webhooks
            app.show app.views.webhooks.render().refresh()
        else
            app.views.current = app.views.webhooks
            app.show app.views.webhooks.refresh()

    templateFromURLFragment: ->
        app.views.current = undefined

        template = undefined
        try
            template = require "../views/templates/#{ Backbone.history.fragment }"
        catch error

        if template
            app.show el: $(template)[0]
            return

        @show404()

    show404: ->
        if not app.views.pageNotFound?
            app.views.pageNotFound = new PageNotFoundView
            app.views.current = app.views.pageNotFound
            app.show app.views.PageNotFoundView.render()
        else
            app.views.current = app.views.pageNotFound
            app.show app.views.pageNotFound

module.exports = Router