import Controller from './Controller';
import TaskView from '../views/task';
import { fetchTask as TaskFetchAction, clear as TaskFetchActionClear} from '../actions/api/task';
import { FetchAction as TaskCleanupsFetchAction } from '../actions/api/taskCleanups';

class TaskDetail extends Controller {

  initialize({store, taskId, filePath}) {
    app.showPageLoader()
    this.title(taskId);
    this.store = store;
    this.taskId = taskId;
    this.filePath = filePath;

    this.store.dispatch(TaskFetchActionClear());
    this.refresh().then(() => {
      this.setView(new TaskView(store, this.taskId));
      app.showView(this.view);
    });
  }

  refresh() {
    let promises = [];
    promises.push(this.store.dispatch(TaskFetchAction(this.taskId)));
    promises.push(this.store.dispatch(TaskCleanupsFetchAction.trigger()));
    return Promise.all(promises);
  }
}

export default TaskDetail;
