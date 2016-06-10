import Controller from './Controller';
import WebhooksView from '../views/webhooks';
import { fetchWebhooks } from '../actions/api/webhooks';

class WebhooksController extends Controller {
    initialize({store}) {
        this.title('Webhooks');
        this.setView(new WebhooksView(store));
        app.showView(this.view);

        this.store = store;

        this.refresh();
    }

    refresh() {
      console.log("refresh");
      this.store.dispatch(fetchWebhooks());
    }
}

export default WebhooksController;
