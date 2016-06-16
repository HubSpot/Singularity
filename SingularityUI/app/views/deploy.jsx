import ReactView from './reactView';
import DeployDetail from '../components/deployDetail/DeployDetail';

import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

class DeployView extends ReactView {

    initialize(store) {
        this.store = store;
    }

    render() {
        ReactDOM.render(<Provider store={this.store}><DeployDetail/></Provider>, this.el);
    }
}

export default DeployView;
