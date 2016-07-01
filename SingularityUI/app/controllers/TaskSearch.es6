import Controller from './Controller';
import TaskSearchView from '../views/taskSearch';
import { FetchRequest } from '../actions/api/requests';

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
      return this.store.dispatch(FetchRequest.trigger(this.requestId));
    }
}


export default TaskSearchController;
