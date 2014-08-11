DashboardController = require 'controllers/Dashboard'
StatusController    = require 'controllers/Status'

NewRequestController = require 'controllers/NewRequest'
NewDeployController  = require 'controllers/NewDeploy'

RequestDetailController = require 'controllers/RequestDetail'
RequestsTableController = require 'controllers/RequestsTable'

TasksTableController = require 'controllers/TasksTable'
TaskDetailController = require 'controllers/TaskDetail'
TailController = require 'controllers/Tail'

RacksController = require 'controllers/Racks'
SlavesController = require 'controllers/Slaves'

NotFoundController = require 'controllers/NotFound'

class Router extends Backbone.Router

    routes:
        '(/)': 'dashboard'
        'status(/)': 'status'

        'requests/new(/)': 'newRequest'

        'requests/:state/:subFilter/:searchFilter(/)': 'requestsTable'
        'requests/:state/:subFilter(/)': 'requestsTable'
        'requests/:state(/)': 'requestsTable'
        'requests(/)': 'requestsTable'

        'request/:requestId(/)': 'requestDetail'

        'request/:requestId/deploy(/)': 'newDeploy'

        'tasks/:state/:searchFilter(/)': 'tasksTable'
        'tasks/:state(/)': 'tasksTable'
        'tasks(/)': 'tasksTable'

        'task/:taskId(/)': 'taskDetail'
        'task/:taskId/files(/)*path': 'taskDetail'
        'task/:taskId/tail/*path': 'tail'

        'racks(/)': 'racks'
        'slaves(/)': 'slaves'
        
        '*anything': 'notFound'

    dashboard: ->
        app.bootstrapController new DashboardController

    status: ->
        app.bootstrapController new StatusController

    newRequest: ->
        app.bootstrapController new NewRequestController

    requestsTable: (state = 'all', subFilter = 'all', searchFilter = '') ->
        app.bootstrapController new RequestsTableController {state, subFilter, searchFilter}

    requestDetail: (requestId) ->
        app.bootstrapController new RequestDetailController {requestId}

    newDeploy: (requestId) ->
        app.bootstrapController new NewDeployController {requestId}

    tasksTable: (state = 'active', searchFilter = '') ->
        app.bootstrapController new TasksTableController {state, searchFilter}

    taskDetail: (taskId, filePath) ->
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
