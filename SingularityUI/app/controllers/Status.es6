import Controller from './Controller';

import State from '../models/State';

import StatusView from '../views/status';

import * as StatusActions from '../actions/api/status';

class StatusController extends Controller {

    initialize({store}) {
        store.dispatch(StatusActions.fetchStatus());
        console.log(store);

        app.showPageLoader();
        this.title('Status');

        this.models.state = new State();

        return this.models.state.fetch().done(() => {
            this.setView(new StatusView(
                {model: this.models.state}));

            return app.showView(this.view);
        });
    }

    refresh() {
        return this.models.state.fetch();
    }
}


export default StatusController;
