import Controller from './Controller';
import TaskSearchView from '../views/taskSearch';
import { FetchAction } from '../actions/api/status';

class TaskSearchController extends Controller {

    initialize({store, requestId}) {
      app.showPageLoader()
      this.title('Task Search');
      this.store = store;
      this.requestId = requestId;

      this.refresh().then(() => {
        this.setView(new TaskSearchView(store, this.requestId));
        app.showView(this.view);
      });
    }

    refresh() {
      return this.store.dispatch(FetchAction.trigger());
    }
}


export default TaskSearchController;
