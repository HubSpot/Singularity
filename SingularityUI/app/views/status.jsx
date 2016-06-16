import View from './view';
import StatusPage from '../components/status/StatusPage';

import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

class StatusView extends View {

    initialize(store) {
        this.store = store;
    }

    remove() {
        super.remove();
        ReactDOM.unmountComponentAtNode(this.el);
    }

    render() {
        ReactDOM.render(<Provider store={this.store}><StatusPage/></Provider>, this.el);
    }
}

export default StatusView;
