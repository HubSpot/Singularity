Controller = require './Controller'

LogLines = require '../collections/LogLines'
MergedLogLines = require '../collections/MergedLogLines'

LogView = require '../views/logView'

Redux = require 'redux'
thunk = require 'redux-thunk'
logger = require 'redux-logger'
rootReducer = require '../reducers'
LogActions = require '../actions/log'
ActiveTasks = require '../actions/activeTasks'

class LogViewer extends Controller
  initialize: ({@requestId, @path, @initialOffset, taskIds, viewMode, search}) ->
    window.lv = @
    @title 'Tail of ' + @path

    initialState = {
        viewMode,
        colors: ['Default', 'Light', 'Dark'],
        logRequestLength: 30000,
        activeRequest: {
            @requestId
        }
    }

    @store = Redux.createStore(rootReducer, initialState, Redux.compose(Redux.applyMiddleware(thunk.default, logger())))

    if taskIds
        initPromise = @store.dispatch(LogActions.initialize(@requestId, @path, search, taskIds))
    else
        initPromise = @store.dispatch(LogActions.initializeUsingActiveTasks(@requestId, @path, search))
    
    initPromise.then =>
        @store.dispatch(ActiveTasks.updateActiveTasks(@requestId))

    setInterval @update, 1000
    setInterval @updateFilesizes, 10000

    # create log view
    @view = new LogView @store

    @setView @view

    @view.render()
    app.showView @view

  update: => @store.dispatch(LogActions.updateGroups())

  updateFilesizes: => @store.dispatch(LogActions.updateFilesizes())

module.exports = LogViewer
