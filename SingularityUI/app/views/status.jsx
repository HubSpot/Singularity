import ReactView from './reactView';
import StatusPage from '../components/status/StatusPage';

import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

class StatusView extends ReactView {

    initialize(store) {
        this.store = store;
    }

    render() {
        ReactDOM.render(<Provider store={this.store}><StatusPage/></Provider>, this.el);
    }
}

export default StatusView;
