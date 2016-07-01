import Controller from './Controller';
import TaskSearchView from '../views/taskSearch';
import { FetchRequest } from '../actions/api/requests';
import { FetchAction as FetchTaskHistory } from '../actions/api/taskHistory';
import TaskSearch from '../components/taskSearch/TaskSearch';

class TaskSearchController extends Controller {

    initialize({store, requestId}) {
      app.showPageLoader()
      this.title('Task Search');
      this.store = store;
      this.requestId = requestId;

      const promises = [];
      promises.push(this.store.dispatch(FetchRequest.trigger(this.requestId)));
      promises.push(this.store.dispatch(FetchTaskHistory.trigger({requestId: this.requestId, page: 1, count: TaskSearch.TASKS_PER_PAGE})));

      Promise.all(promises).then((r) => {
        this.setView(new TaskSearchView(store, this.requestId));
        app.showView(this.view);
      });
    }
}

export default TaskSearchController;
