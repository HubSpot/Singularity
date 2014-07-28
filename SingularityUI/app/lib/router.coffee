DashboardController = require 'controllers/Dashboard'
StatusController = require 'controllers/Status'

RequestsTableController = require 'controllers/RequestsTable'
TasksTableController = require 'controllers/TasksTable'
TailController = require 'controllers/Tail'

RacksController = require 'controllers/Racks'
SlavesController = require 'controllers/Slaves'

TaskDetailController = require 'controllers/TaskDetail'

RequestView = require 'views/request'

NotFoundController = require 'controllers/NotFound'

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
        '*anything': 'notFound'

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

    task: (taskId, filePath) ->
        app.bootstrapController new TaskDetailController {taskId, filePath}

    tail: (taskId, path = '') ->
        app.bootstrapController new TailController {taskId, path}

    racks: ->
        app.bootstrapController new RacksController

    slaves: ->
        app.bootstrapController new SlavesController

    notFound: ->
        app.bootstrapController new NotFoundController

module.exports = Router
