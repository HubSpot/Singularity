DashboardController = require 'controllers/Dashboard'
StatusController    = require 'controllers/Status'

RequestFormNewController = require 'controllers/RequestFormNew'
RequestFormEditController = require 'controllers/RequestFormEdit'

NewDeployController  = require 'controllers/NewDeploy'

RequestDetailController = require 'controllers/RequestDetail'
RequestsTableController = require 'controllers/RequestsTable'

TasksTableController = require 'controllers/TasksTable'
TaskDetailController = require 'controllers/TaskDetail'

RacksController = require 'controllers/Racks'
SlavesController = require 'controllers/Slaves'

NotFoundController = require 'controllers/NotFound'

DeployDetailController = require 'controllers/DeployDetail'

LogViewerController = require 'controllers/LogViewer'

Utils = require './utils'

class Router extends Backbone.Router

    routes:
        '(/)': 'dashboard'
        'status(/)': 'status'

        'requests/new(/)': 'newRequest'
        'requests/edit/:requestId': 'editRequest'

        'requests/:state/:subFilter/:searchFilter(/)': 'requestsTable'
        'requests/:state/:subFilter(/)': 'requestsTable'
        'requests/:state(/)': 'requestsTable'
        'requests(/)': 'requestsTable'

        'request/:requestId(/)': 'requestDetail'
        'request/:requestId/deploy/:deployId(/)': 'deployDetail'
        'request/:requestId/tail/*path': 'aggregateTail'

        'request/:requestId/deploy(/)': 'newDeploy'

        'tasks/:state/:requestsSubFilter/:searchFilter(/)': 'tasksTable'
        'tasks/:state/:requestsSubFilter(/)': 'tasksTable'
        'tasks/:state(/)': 'tasksTable'
        'tasks(/)': 'tasksTable'

        'task/:taskId(/)': 'taskDetail'
        'task/:taskId/files(/)*path': 'taskFileBrowser'
        'task/:taskId/tail/*path': 'tail'

        'racks(/)': 'racks'
        'racks/:state(/)': 'racks'

        'slaves/:state(/)': 'slaves'
        'slaves(/)': 'slaves'

        '*anything': 'notFound'

    dashboard: ->
        app.bootstrapController new DashboardController

    status: ->
        app.bootstrapController new StatusController

    newRequest: ->
        app.bootstrapController new RequestFormNewController

    editRequest: (requestId = '') ->
        app.bootstrapController new RequestFormEditController {requestId}

    requestsTable: (state = 'all', subFilter = 'all', searchFilter = '') ->
        app.bootstrapController new RequestsTableController {state, subFilter, searchFilter}

    requestDetail: (requestId) ->
        app.bootstrapController new RequestDetailController {requestId}

    newDeploy: (requestId) ->
        app.bootstrapController new NewDeployController {requestId}

    tasksTable: (state = 'active', requestsSubFilter = 'all', searchFilter = '') ->
        app.bootstrapController new TasksTableController {state, requestsSubFilter, searchFilter}

    taskDetail: (taskId) ->
        app.bootstrapController new TaskDetailController {taskId, filePath:null}

    taskFileBrowser: (taskId, filePath="") ->
        app.bootstrapController new TaskDetailController {taskId, filePath}

    tail: (taskId, path = '') ->
        initialOffset = parseInt(window.location.hash.substr(1), 10) || null
        splits = taskId.split('-')
        requestId = splits.slice(0, splits.length - 5).join('-')
        params = Utils.getQueryParams()

        search = params.search || ''

        path = path.replace(taskId, '$TASK_ID')

        app.bootstrapController new LogViewerController {requestId, path, initialOffset, taskIds: [taskId], search, viewMode: 'split'}

    racks: (state = 'all') ->
        app.bootstrapController new RacksController {state}

    slaves: (state = 'all') ->
        app.bootstrapController new SlavesController {state}

    notFound: ->
        app.bootstrapController new NotFoundController

    deployDetail: (requestId, deployId) ->
        app.bootstrapController new DeployDetailController {requestId, deployId}

    aggregateTail: (requestId, path = '') ->
        initialOffset = parseInt(window.location.hash.substr(1), 10) || null

        params = Utils.getQueryParams()
        if params.taskIds
            taskIds = params.taskIds.split(',')
        else
            taskIds = []
        viewMode = params.viewMode || 'split'
        search = params.search || ''

        app.bootstrapController new LogViewerController {requestId, path, initialOffset, taskIds, viewMode, search}

module.exports = Router
