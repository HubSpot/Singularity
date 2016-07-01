import Controller from './Controller';
import RequestFormView from '../views/requestForm';
import { FetchRequest, SaveRequest } from '../actions/api/requests';
import { FetchRacks } from '../actions/api/racks';

class RequestFormController extends Controller {

  initialize({store, requestId = ''}) {
    app.showPageLoader();
    this.title(`${requestId ? 'Edit' : 'New'} Request`);
    this.store = store;
    this.requestId = requestId;

    const racksPromise = this.store.dispatch(FetchRacks.trigger());
    let requestPromise;
    if (this.requestId) {
      requestPromise = this.store.dispatch(FetchRequest.trigger(this.requestId));
    } else {
      requestPromise = this.store.dispatch(FetchRequest.clearData());
    }
    const clearSaveRequestDataPromise = this.store.dispatch(SaveRequest.clearData());

    Promise.all([racksPromise, requestPromise, clearSaveRequestDataPromise]).then(() => {
      this.setView(new RequestFormView({store: this.store, requestId: this.requestId}));
      app.showView(this.view);
    });
  }
}

export default RequestFormController;
