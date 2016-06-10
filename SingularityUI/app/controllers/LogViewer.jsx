import Controller from './Controller';

import LogView from '../views/logView';

import LogActions from '../actions/log';
import { updateActiveTasks } from '../actions/activeTasks';

class LogViewer extends Controller {
  initialize({store, requestId, path, initialOffset, taskIds, viewMode, search}) {
    this.requestId = requestId;
    this.path = path;
    this.initialOffset = initialOffset;
    this.title(`Tail of ${_.last(this.path.split('/'))}`);

    let initPromise;

    if (taskIds.length > 0) {
        initPromise = store.dispatch(LogActions.initialize(this.requestId, this.path, search, taskIds, viewMode))
    } else {
        initPromise = store.dispatch(LogActions.initializeUsingActiveTasks(this.requestId, this.path, search, viewMode))
    }

    initPromise.then(() => { store.dispatch(updateActiveTasks(requestId)); });

    // create log view
    this.view = new LogView(store);

    this.setView(this.view)  // does nothing
    app.showView(this.view)
  }
}

export default LogViewer;
