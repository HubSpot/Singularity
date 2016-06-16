import Controller from './Controller';
import TaskView from '../views/task';
import { fetchTask as TaskFetchAction} from '../actions/api/task';
import { clear as TaskFetchActionClear} from '../actions/api/task';

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
    return this.store.dispatch(TaskFetchAction(this.taskId));
  }
}

export default TaskDetail;
