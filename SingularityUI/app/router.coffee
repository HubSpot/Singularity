ReactDashboardController    = require 'controllers/Dashboard'

StatusController    = require 'controllers/Status'

NewRequestController = require 'controllers/NewRequest'
NewDeployController  = require 'controllers/NewDeploy'

RequestController = require 'controllers/Request'
RequestsTableController = require 'controllers/RequestsTable'

TasksController = require 'controllers/Tasks'
TaskDetailController = require 'controllers/TaskDetail'
TailController = require 'controllers/Tail'

RacksController = require 'controllers/Racks'
SlavesController = require 'controllers/Slaves'

NotFoundController = require 'controllers/NotFound'

class Router extends Backbone.Router

    routes:
        '(/)': 'reactDashboard'
        'status(/)': 'status'

        'requests/new(/)': 'newRequest'

        'requests/:state/:subFilter/:searchFilter(/)': 'requestsTable'
        'requests/:state/:subFilter(/)': 'requestsTable'
        'requests/:state(/)': 'requestsTable'
        'requests(/)': 'requestsTable'

        'request/:requestId(/)': 'request'

        'request/:requestId/deploy(/)': 'newDeploy'

        'tasks/:state/:searchFilter(/)': 'tasks'
        'tasks/:state(/)': 'tasks'
        'tasks(/)': 'tasks'

        'task/:taskId(/)': 'taskDetail'
        'task/:taskId/files(/)*path': 'taskFileBrowser'
        'task/:taskId/tail/*path': 'tail'

        'racks(/)': 'racks'
        'slaves(/)': 'slaves'
        
        '*anything': 'notFound'

    dashboard: ->
        app.bootstrapController new DashboardController

    reactDashboard: ->
        app.bootstrapController new ReactDashboardController        

    status: ->
        app.bootstrapController new StatusController

    newRequest: ->
        app.bootstrapController new NewRequestController

    requestsTable: (state = 'all', subFilter = 'all', searchFilter = '') ->
        app.bootstrapController new RequestsTableController {state, subFilter, searchFilter}

    request: (requestId) ->
        app.bootstrapController new RequestController {requestId}

    newDeploy: (requestId) ->
        app.bootstrapController new NewDeployController {requestId}

    tasks: (state = 'active', searchFilter = '') ->
        app.bootstrapController new TasksController {state, searchFilter}

    taskDetail: (taskId) ->
        app.bootstrapController new TaskDetailController {taskId, filePath:null}
      
    taskFileBrowser: (taskId, filePath="") ->
        app.bootstrapController new TaskDetailController {taskId, filePath}
        
    tail: (taskId, path = '') ->
        offset = window.location.hash.substr(1) || null
        app.bootstrapController new TailController {taskId, path, offset}

    racks: ->
        app.bootstrapController new RacksController

    slaves: ->
        app.bootstrapController new SlavesController

    notFound: ->
        app.bootstrapController new NotFoundController

module.exports = Router
