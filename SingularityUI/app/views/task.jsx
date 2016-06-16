import View from './view';
import TaskDetail from '../components/taskDetail/TaskDetail';

import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

class TaskView extends View {

    constructor(store, taskId) {
        super();
        this.store = store;
        this.taskId = taskId;
    }

    remove() {
        super.remove();
        ReactDOM.unmountComponentAtNode(this.el);
    }

    render() {
        ReactDOM.render(<Provider store={this.store}><TaskDetail taskId={this.taskId}/></Provider>, this.el);
    }
}

export default TaskView;
