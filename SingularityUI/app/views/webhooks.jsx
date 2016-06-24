import React from 'react';
import ReactDOM from 'react-dom';

import Webhooks from '../components/webhooks/Webhooks';

import ReactView from './reactView';
import { Provider } from 'react-redux';

class WebhooksView extends ReactView {
    initialize(store) {
        this.store = store;
    }

    render() {
        $(this.el).addClass("webhooks-root");
        ReactDOM.render(<Provider store={this.store}><Webhooks /></Provider>, this.el);
    }
}

export default WebhooksView;
