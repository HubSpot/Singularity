import ReactView from './reactView';
import TaskDetail from '../components/taskDetail/TaskDetail';

import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

class TaskView extends ReactView {

    constructor(store, taskId, filePath) {
        super();
        this.store = store;
        this.taskId = taskId;
        this.filePath = filePath;
    }

    render() {
        ReactDOM.render(<Provider store={this.store}><TaskDetail taskId={this.taskId} filePath={this.filePath} /></Provider>, this.el);
    }
}

export default TaskView;
