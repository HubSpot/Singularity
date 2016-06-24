import Controller from './Controller';
import RequestFormView from '../views/requestForm';
import { FetchAction as RequestFetchAction, SaveAction } from '../actions/api/request';
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
    const clearSaveRequestDataPromise = this.store.dispatch(SaveAction.clearData());

    Promise.all([racksPromise, requestPromise, clearSaveRequestDataPromise]).then(() => {
      this.setView(new RequestFormView({store: this.store}));
      app.showView(this.view);
    });
  }
}

export default RequestFormController;
