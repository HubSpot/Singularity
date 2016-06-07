Controller = require './Controller'

LogLines = require '../collections/LogLines'

LogView = require('../views/logView').default

Redux = require 'redux'
thunk = require 'redux-thunk'
logger = require 'redux-logger'
rootReducer = require '../reducers'
LogActions = require '../actions/log'
ActiveTasks = require('../actions/activeTasks').default

class LogViewer extends Controller
  initialize: ({@requestId, @path, @initialOffset, taskIds, viewMode, search}) ->
    @title "Tail of #{_.last @path.split('/')}"

    initialState = {
        viewMode,
        colors: ['Default', 'Light', 'Dark'],
        logRequestLength: 30000,
        activeRequest: {
            @requestId
        }
    }

    middlewares = [thunk.default]

    if window.localStorage.enableReduxLogging
        middlewares.push(logger())

    @store = Redux.createStore(rootReducer, initialState, Redux.compose(Redux.applyMiddleware.apply(this, middlewares)))

    if taskIds.length > 0
        initPromise = @store.dispatch(LogActions.initialize(@requestId, @path, search, taskIds))
    else
        initPromise = @store.dispatch(LogActions.initializeUsingActiveTasks(@requestId, @path, search))

    initPromise.then =>
        @store.dispatch(ActiveTasks.updateActiveTasks(@requestId))

    # create log view
    @view = new LogView @store

    @setView @view  # does nothing
    app.showView @view
    window.getStateJSON = () => JSON.stringify(@store.getState())

module.exports = LogViewer
