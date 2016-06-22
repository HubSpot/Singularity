import Controller from './Controller';
import TasksView from '../views/tasks';
import { FetchAction } from '../actions/api/tasks';

class TasksTableController extends Controller {

  initialize({store, state, requestsSubFilter, searchFilter}) {
    app.showPageLoader()
    this.title('Tasks');
    this.store = store;
    this.state = state;
    this.requestsSubFilter = requestsSubFilter;
    this.searchFilter = searchFilter;

    this.refresh().then(() => {
      this.setView(new TasksView(this.store, this.state, this.requestsSubFilter, this.searchFilter));
      app.showView(this.view);
    });
  }

  refresh() {
    let promises = [];
    promises.push(this.store.dispatch(FetchAction.trigger(this.state)));
    return Promise.all(promises);
  }
}


export default TasksTableController;
