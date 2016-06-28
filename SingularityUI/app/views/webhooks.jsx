import React from 'react';
import ReactDOM from 'react-dom';

import Webhooks from '../components/webhooks/Webhooks';

import View from './view';

class WebhooksView extends View {

    initialize({collections, fetched}, opts) {
        this.collections = collections;
        this.fetched = fetched;
        this.opts = opts;
    }

    render() {
        $(this.el).addClass("webhooks-root");
        ReactDOM.render(
            <Webhooks
                fetched = {this.fetched}
                collections = {this.collections}
            />,
          this.el);
    }
}


export default WebhooksView;
