import Controller from './Controller';

import LogLines from '../collections/LogLines';

import LogView from '../views/logView';

import configureStore from '../store/configureStore';

import LogActions from '../actions/log';
import ActiveTasks from '../actions/activeTasks';

class LogViewerController extends Controller {
  initialize({requestId, path, initialOffset, taskIds, viewMode, search}) {
    this.requestId = requestId;
    this.path = path;
    this.initialOffset = initialOffset;
    this.title(`Tail of ${_.last(this.path.split('/'))}`);

    this.store = configureStore();

    // Set up initial state for this
    const initialState = {
      viewMode,
      colors: ['Default', 'Light', 'Dark'],
      logRequestLength: 30000,
      activeRequest: {
        requestId: this.requestId
      }
    };

    this.store.dispatch(LogActions.setupInitialLoggingState(initialState));

    let initPromise;
    if (taskIds.length > 0) {
      initPromise = this.store.dispatch(LogActions.initialize(this.requestId, this.path, search, taskIds));
    } else {
      initPromise = this.store.dispatch(LogActions.initializeUsingActiveTasks(this.requestId, this.path, search));
    }

    initPromise.then(() => {
      return this.store.dispatch(ActiveTasks.updateActiveTasks(this.requestId));
    });

    // create log view
    this.view = new LogView(this.store);

    this.setView(this.view); // does nothing
    app.showView(this.view);
    return window.getStateJSON = () => JSON.stringify(this.store.getState());
  }
}

export default LogViewerController;
