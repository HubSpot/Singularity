import Controller from './Controller';
import RequestFormView from '../views/requestForm';
import Racks from '../collections/Racks';
import Request from '../models/Request';
import { FetchAction as RequestFetchAction } from '../actions/api/request';
import { FetchAction as RacksFetchAction } from '../actions/api/racks';

class ReqeustFormController extends Controller {

    showView(store, edit) {
        this.setView(new RequestFormView({store, edit}));
        app.showView(this.view);
    }

    initialize({store, requestId = ''}) {
        app.showPageLoader();
        this.title(`${requestId ? 'Edit' : 'New'} Request`);
        this.store = store;

        let racksPromise = this.store.dispatch(RacksFetchAction.trigger());
        if (requestId) {
            let requestPromise = this.store.dispatch(RequestFetchAction.trigger(requestId));
            requestPromise.then(() => {
                this.showView(store, true);
            });
        } else {
            this.showView(store, false);
        }
    }
}

export default ReqeustFormController;
