DashboardController = require 'controllers/Dashboard'
StatusController = require 'controllers/Status'

RequestsTableController = require 'controllers/RequestsTable'
TasksTableController = require 'controllers/TasksTable'
TailController = require 'controllers/Tail'

RacksController = require 'controllers/Racks'
SlavesController = require 'controllers/Slaves'

RequestView = require 'views/request'

TaskView = require 'views/task'

PageNotFoundView = require 'views/pageNotFound'

class Router extends Backbone.Router

    routes:
        '(/)': 'dashboard'
        'status(/)': 'status'
        'requests/:state/:subFilter/:searchFilter(/)': 'requestsTable'
        'requests/:state/:subFilter(/)': 'requestsTable'
        'requests/:state(/)': 'requestsTable'
        'requests(/)': 'requestsTable'
        'request/:requestId(/)': 'request'
        'tasks/:state/:searchFilter(/)': 'tasksTable'
        'tasks/:state(/)': 'tasksTable'
        'tasks(/)': 'tasksTable'
        'task/:taskId(/)': 'task'
        # 'task/:taskId/files(/)': 'task'
        'task/:taskId/files(/)*path': 'task'
        'task/:taskId/tail/*path': 'tail'
        'racks(/)': 'racks'
        'slaves(/)': 'slaves'
        '*anything': 'templateFromURLFragment'

    dashboard: ->
        app.bootstrapController new DashboardController

    status: ->
        app.bootstrapController new StatusController

    requestsTable: (state = 'all', subFilter = 'all', searchFilter = '') ->
        app.bootstrapController new RequestsTableController {state, subFilter, searchFilter}

    request: (requestId) ->
        app.showView new RequestView requestId: requestId

    tasksTable: (state = 'active', searchFilter = '') ->
        app.bootstrapController new TasksTableController {state, searchFilter}

    task: (taskId, path) ->
        app.showView new TaskView id: taskId, path: path

    tail: (taskId, path = '') ->
        app.bootstrapController new TailController {taskId, path}

    racks: ->
        app.bootstrapController new RacksController

    slaves: ->
        app.bootstrapController new SlavesController

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
            app.show app.views.pageNotFound.render()
        else
            app.views.current = app.views.pageNotFound
            app.show app.views.pageNotFound

module.exports = Router
