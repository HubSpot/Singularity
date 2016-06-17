import Controller from './Controller';
import TaskView from '../views/task';
import { fetchTask as TaskFetchAction, clear as TaskFetchActionClear} from '../actions/api/task';
import { FetchAction as TaskCleanupsFetchAction } from '../actions/api/taskCleanups';
import { FetchAction as TaskFilesFetchAction } from '../actions/api/taskFiles';
import { FetchAction as TaskResourceUsageFetchAction } from '../actions/api/taskResourceUsage';
import { FetchAction as TaskS3LogsFetchAction } from '../actions/api/taskS3Logs';

class TaskDetail extends Controller {

  initialize({store, taskId, filePath}) {
    app.showPageLoader()
    this.title(taskId);
    this.store = store;
    this.taskId = taskId;
    this.filePath = filePath;

    this.store.dispatch(TaskFetchActionClear());
    this.store.dispatch(TaskFilesFetchAction.trigger(this.taskId, this.filePath))
    this.refresh().then(() => {
      this.setView(new TaskView(store, this.taskId));
      app.showView(this.view);
    });
  }

  refresh() {
    let promises = [];
    promises.push(this.store.dispatch(TaskFetchAction(this.taskId)));
    promises.push(this.store.dispatch(TaskCleanupsFetchAction.trigger()));
    promises.push(this.store.dispatch(TaskResourceUsageFetchAction.trigger(this.taskId)));
    promises.push(this.store.dispatch(TaskS3LogsFetchAction.trigger(this.taskId)));
    return Promise.all(promises);
  }
}

export default TaskDetail;
