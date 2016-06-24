import Controller from './Controller';
import RequestFormView from '../views/requestForm';
import { FetchAction as RequestFetchAction } from '../actions/api/request';
import { FetchAction as RacksFetchAction } from '../actions/api/racks';

class RequestFormController extends Controller {

  initialize({store, requestId = ''}) {
    app.showPageLoader();
    this.title(`${requestId ? 'Edit' : 'New'} Request`);
    this.store = store;
    this.requestId = requestId;

    const racksPromise = this.store.dispatch(RacksFetchAction.trigger());
    let requestPromise;
    if (this.requestId) {
      requestPromise = this.store.dispatch(RequestFetchAction.trigger(this.requestId));
    } else {
      requestPromise = this.store.dispatch(RequestFetchAction.clearData());
    }

    Promise.all([racksPromise, requestPromise]).then(() => {
      this.setView(new RequestFormView({store: this.store}));
      app.showView(this.view);
    });
  }
}

export default RequestFormController;
