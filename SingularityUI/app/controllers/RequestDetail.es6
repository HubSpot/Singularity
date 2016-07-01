import Controller from './Controller';
import RequestDetailView from '../views/request';
import { FetchRequest } from '../actions/api/requests';
import { FetchActiveTasksForRequest } from '../actions/api/history';
import { FetchTaskCleanups } from '../actions/api/tasks';

class RequestDetailController extends Controller {

  initialize({requestId, store}) {
    app.showPageLoader();
    this.requestId = requestId;
    this.store = store;

    this.title(this.requestId);

    this.refresh().then(() => {
      this.setView(new RequestDetailView({store, requestId}));
      app.showView(this.view);
    });
  }

  refresh() {
    const requestPromise = this.store.dispatch(FetchRequest.trigger(this.requestId));
    const activeTasksPromise = this.store.dispatch(FetchActiveTasksForRequest.trigger(this.requestId));
    const taskCleanupsPromise = this.store.dispatch(FetchTaskCleanups.trigger());

    const promises = [requestPromise, activeTasksPromise, taskCleanupsPromise];

    return Promise.all(promises);
  }
}


export default RequestDetailController;
