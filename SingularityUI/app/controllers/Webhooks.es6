import Controller from './Controller';
import WebhooksView from '../views/webhooks';
import { FetchWebhooks } from '../actions/api/webhooks';

class WebhooksController extends Controller {
  initialize({store}) {
    this.title('Webhooks');
    this.setView(new WebhooksView(store));
    app.showView(this.view);

    this.store = store;

    this.refresh();
  }

  refresh() {
    this.store.dispatch(FetchWebhooks.trigger());
  }
}

export default WebhooksController;
