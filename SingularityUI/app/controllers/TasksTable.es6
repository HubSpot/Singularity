import Controller from './Controller';
import TasksView from '../views/tasks';
import { FetchAction } from '../actions/api/tasks';
import { FetchAction as CleanupsFetchAction} from '../actions/api/taskCleanups';

class TasksTableController extends Controller {

  initialize({store, state, requestsSubFilter, searchFilter}) {
    app.showPageLoader()
    this.title('Tasks');
    this.store = store;
    this.state = state;
    this.requestsSubFilter = requestsSubFilter;
    this.searchFilter = searchFilter;

    this.refresh().then(() => {
      this.setView(new TasksView(this.store, this.state, this.requestsSubFilter, this.searchFilter, (...args) => this.updateFilters(...args)));
      app.showView(this.view);
    });
  }

  refresh() {
    let promises = [];
    promises.push(this.store.dispatch(FetchAction.trigger(this.state)));
    promises.push(this.store.dispatch(CleanupsFetchAction.trigger()));
    return Promise.all(promises);
  }

  // Backbone router sucks
  updateFilters(state, requestsSubFilter, searchFilter) {
    this.state = state;
    this.requestsSubFilter = requestsSubFilter;
    this.searchFilter = searchFilter;
  }
}

export default TasksTableController;
