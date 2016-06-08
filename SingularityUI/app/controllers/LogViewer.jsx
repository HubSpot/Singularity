import Controller from './Controller';

import LogLines from '../collections/LogLines';

import LogView from '../views/logView';

import { createStore, compose, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import logger from 'redux-logger';
import rootReducer from '../reducers/index';
import LogActions from '../actions/log';
import ActiveTasks from '../actions/activeTasks';

class LogViewer extends Controller {
  initialize({requestId, path, initialOffset, taskIds, viewMode, search}) {
    this.requestId = requestId;
    this.path = path;
    this.initialOffset = initialOffset;
    this.title(`Tail of ${_.last(this.path.split('/'))}`);

    let initialState = {
        viewMode,
        colors: ['Default', 'Light', 'Dark'],
        logRequestLength: 30000,
        activeRequest: {
            requestId: this.requestId
        }
    }

    let middlewares = [thunk];

    if (window.localStorage.enableReduxLogging) {
      middlewares.push(logger());
    }

    const store = createStore(rootReducer, initialState, compose(applyMiddleware.apply(this, middlewares)))
    let initPromise;

    if (taskIds.length > 0) {
        initPromise = store.dispatch(LogActions.initialize(this.requestId, this.path, search, taskIds))
    } else {
        initPromise = store.dispatch(LogActions.initializeUsingActiveTasks(this.requestId, this.path, search))
    }

    initPromise.then(function () {
        store.dispatch(ActiveTasks.updateActiveTasks(requestId))
      });

    // create log view
    this.view = new LogView(store);

    this.setView(this.view)  // does nothing
    app.showView(this.view)
  }
}

export default LogViewer;
